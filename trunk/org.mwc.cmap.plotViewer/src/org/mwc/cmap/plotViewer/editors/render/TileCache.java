package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.resources.i18n.ErrorKeys;
import org.geotools.resources.i18n.Errors;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.EngineeringCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;

public class TileCache
{
	private static class ScaleLevelCache
	{
		private List<List<Tile>> tiles = new ArrayList<List<Tile>>();
	}

	public static final double[] DEFAULT_WGS84 = new double[]
	{ 1, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10_000, 20_000, 50_000,
			100_000, 200_000, 500_000, 1_000_000, 2_000_000, 5_000_000, 10_000_000,
			20_000_000, 50_000_000 };

	private List<ScaleLevelCache> caches = new ArrayList<ScaleLevelCache>();
	private Dimension tileSize;
	private double[] scales;
	private Coordinate bottomLeft;

	private double dpi;

	private CoordinateReferenceSystem crs;

	/**
	 * Constructor
	 * 
	 * 
	 * @param tileSize
	 *          size of the tiles
	 * @param scales
	 *          the scales of the cached tiles. The scales will be sorted so that
	 *          the smallest scale (1:1 is small) will be at index 0 and larger
	 *          scales will be at(1:5_000_000 is large) at greater indices
	 * @param bottomLeft
	 *          the coordinate to use as the bottom left of the "world" (or valid
	 *          region). The 0,0 referenced tile will be at this location and all
	 *          other tiles will be referenced relative to this location.
	 * @param dpi
	 *          dpi of the display
	 * @param crs
	 *          the crs of the cache
	 */
	public TileCache(Dimension tileSize, double[] scales, Coordinate bottomLeft,
			double dpi, CoordinateReferenceSystem crs)
	{
		Arrays.sort(scales);
		this.tileSize = tileSize;
		this.scales = scales;
		this.bottomLeft = bottomLeft;
		this.dpi = dpi;
		this.crs = crs;
	}

	/**
	 * Calculate the closest scale give the parameters
	 * 
	 * @param envelope
	 *          the area displayed
	 * @param dimension
	 *          the size of the screen where the map is displayed
	 */
	public double getClosestScale(Envelope envelope, Dimension dimension)
	{
		ReferencedEnvelope refEnv = new ReferencedEnvelope(envelope, crs);
		try
		{
			double diagonal = Math.sqrt(dimension.width * dimension.width
					+ dimension.height * dimension.height);
			Assert.isTrue(!Double.isNaN(diagonal) && !Double.isInfinite(diagonal),
					"The screen size is too large, cannot calculate diagonal distance: "
							+ dimension);
			double scale = RendererUtilities.calculateScale(refEnv, dimension.width,
					dimension.width, dpi);
			for (int i = 0; i < scales.length; i++)
			{
				double nextScale = scales[i];
				if (nextScale > scale)
				{
					if (i == 0)
					{
						return scales[0];
					}
					else
					{
						double diff1 = scale - scales[i - 1];
						double diff2 = nextScale - scale;
						if (diff1 < diff2)
						{
							return scales[i - 1];
						}
						else
						{
							return nextScale;
						}
					}
				}
			}
			return scales[scales.length - 1];
		}
		catch (TransformException e)
		{
			throw new RuntimeException(e);
		}
		catch (FactoryException e)
		{
			throw new RuntimeException(e);
		}

	}

	public Envelope calculateBounds(Dimension screenSize, int scale,
			Coordinate center)
	{
		double dHeight = screenSize.height;
		double dWidth = screenSize.width;
		double angle = Math.atan(dHeight / dWidth);
		int imageWidth = screenSize.width;
		int imageHeight = screenSize.height;
		double diagonalPixelDistancePixels = Math.sqrt(imageWidth * imageWidth
				+ imageHeight * imageHeight);
		double diagonalPixelDistanceMeters = diagonalPixelDistancePixels / dpi
				* 2.54 / 100; // 2.54 = cm/inch, 100= cm/m
		double diagonalGroundDistance = scale * diagonalPixelDistanceMeters;

		if (!(crs instanceof EngineeringCRS))
		{
			try
			{
				final CoordinateReferenceSystem tempCRS = CRS.getHorizontalCRS(crs);
				if (tempCRS == null)
				{
					throw new TransformException(Errors.format(
							ErrorKeys.CANT_REDUCE_TO_TWO_DIMENSIONS_$1, crs));
				}
				MathTransform transform = CRS.findMathTransform(crs,
						DefaultGeographicCRS.WGS84, true);
				Coordinate latLongCenter = JTS.transform(center, new Coordinate(),
						transform);
				return geodeticDiagonalDistance(latLongCenter, diagonalGroundDistance,
						Math.toDegrees(angle));
			}
			catch (TransformException e)
			{
				throw new RuntimeException(e);
			}
			catch (FactoryException e)
			{
				throw new RuntimeException(e);
			}
		}
		else
		{
			// if it's an engineering crs, compute only the graphical scale, assuming
			// a CAD space

			double width = Math.sin(angle) * diagonalGroundDistance;
			double height = Math.cos(angle) * diagonalGroundDistance;

			return new Envelope(center.x - width / 2, center.x + width / 2, center.y
					- height / 2, center.y + height / 2);
		}
	}

	private Envelope geodeticDiagonalDistance(Coordinate center,
			double diagonalGroundDistance, double angleDegrees) throws TransformException
	{
		GeodeticCalculator calculator = new GeodeticCalculator(
				DefaultGeographicCRS.WGS84);
		calculator.setStartingPosition(new DirectPosition2D(
				DefaultGeographicCRS.WGS84, center.x, center.y));
		calculator.setDirection(angleDegrees, diagonalGroundDistance / 2);
		Point2D upperRight = calculator.getDestinationGeographicPoint();
		calculator.setDirection(angleDegrees - 180, diagonalGroundDistance / 2);
		Point2D bottomLeft = calculator.getDestinationGeographicPoint();

		return new Envelope(bottomLeft.getX(), upperRight.getX(),
				bottomLeft.getY(), upperRight.getY());
	}

}
