package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
	private class ScaleLevelCache
	{
		private Map<Integer, Map<Integer, Tile>> tiles = new HashMap<Integer, Map<Integer, Tile>>();
		volatile int tileCount = 0;
		public synchronized Tile getTile(int xIndex, int yIndex, ReferencedEnvelope bounds)
		{
			Map<Integer, Tile> row = tiles.get(xIndex);
			
			if (row == null) {
				row = new HashMap<Integer, Tile>();
				tiles.put(xIndex, row);
			}
			
			Tile tile = row.get(yIndex);
			
			if (tile == null) {
				tile = new Tile(loader, tileSize, bounds);
				tileCount ++;
				row.put(yIndex, tile);
			}
			
			return tile;
		}
	}

	public static final int[] DEFAULT_WGS84 = new int[]
	{ 1, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10_000, 20_000, 50_000,
			100_000, 200_000, 500_000, 1_000_000, 2_000_000, 5_000_000, 10_000_000,
			20_000_000, 50_000_000 };

	private Map<Integer, Integer> cacheIndices = new HashMap<Integer, Integer>();

	private final TileLoader loader;
	private ScaleLevelCache[] caches;
	private Dimension tileSize;
	private int[] scales;
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
	public TileCache(Dimension tileSize, int[] scales, Coordinate bottomLeft,
			double dpi, CoordinateReferenceSystem crs, TileLoader loader)
	{
		Assert.isTrue(loader != null);
		Assert.isTrue(scales != null);
		Assert.isTrue(tileSize != null);
		Assert.isTrue(bottomLeft != null);
		Assert.isTrue(crs != null);
		Assert.isTrue(dpi > 0);

		this.loader = loader;

		Arrays.sort(scales);
		this.tileSize = tileSize;
		this.scales = scales;
		this.bottomLeft = bottomLeft;
		this.dpi = dpi;
		this.crs = crs;

		caches = new ScaleLevelCache[scales.length];
		for (int i = 0; i < scales.length; i++)
		{
			cacheIndices.put(scales[i], i);
		}
	}

	/**
	 * Calculate the closest scale give the parameters
	 * 
	 * @param envelope
	 *          the area displayed
	 * @param dimension
	 *          the size of the screen where the map is displayed
	 */
	public int getClosestScale(Envelope envelope, Dimension dimension)
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
				int nextScale = scales[i];
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

	/**
	 * Calculate the bounds given a scale, the size of the display area and the
	 * center coordinate to be displayed.
	 * <p>
	 * See {@link #calculateScale(Envelope, Dimension)} on how scale is calculate
	 * </p>
	 * 
	 * @param screenSize
	 *          size of the display area
	 * @param scale
	 *          the desired scale
	 * @param center
	 *          the center coordinate (in world units)
	 */
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
		double diagonalPixelDistanceMeters = diagonalPixelDistancePixels / (dpi * 2.54 / 100); // 2.54 = cm/inch, 100= cm/m
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
				MathTransform transform = CRS.findMathTransform(
						DefaultGeographicCRS.WGS84, crs, true);
				Envelope latLongEnvAtOrigin = geodeticDiagonalDistance(
						diagonalGroundDistance, Math.toDegrees(angle));
				Envelope env = JTS.transform(latLongEnvAtOrigin, transform);
				env.translate(center.x, center.y);
				return env;
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

			double xFromCenter = width / 2;
			double yFromCenter = height / 2;
			return new Envelope(center.x - xFromCenter, center.x + xFromCenter, center.y
					- yFromCenter, center.y + yFromCenter);
		}
	}

	private Envelope geodeticDiagonalDistance(double diagonalGroundDistance,
			double angleDegrees) throws TransformException
	{
		GeodeticCalculator calculator = new GeodeticCalculator(
				DefaultGeographicCRS.WGS84);
		calculator.setDirection(angleDegrees, diagonalGroundDistance / 2);
		Point2D upperRight = calculator.getDestinationGeographicPoint();
		calculator.setDirection(angleDegrees - 180, diagonalGroundDistance / 2);
		Point2D bottomLeft = calculator.getDestinationGeographicPoint();

		return new Envelope(bottomLeft.getX(), upperRight.getX(),
				bottomLeft.getY(), upperRight.getY());
	}

	/**
	 * Calculate the scale and get the scale.
	 * <p>
	 * Scale is essentially the distance on the real world represented by the
	 * distance on the map.
	 * </p>
	 * <p>
	 * For engineering projections this is easy because the grid is essentially
	 * cartesian and simple trigonometry can be used to calculate the projection
	 * </p>
	 * <p>
	 * LatLong is a pain with regards to scale because the units are degrees as
	 * such, as you pan north (or south) the scale actually changes because the
	 * envelope.getWidth and getHeight stay the same but because the lines
	 * converge at the poles the ground distance actually decreases (to infinity).
	 * </p>
	 * <p>
	 * Since we want to use tiles, we don't want the scale (with respect to tile
	 * calculation) to change. To take this into account I am going to calculate
	 * the scale (for ellipsoidal projections) at the 0,0 instead of the actual
	 * place where the bounds are.
	 * </p>
	 * <p>
	 * Consider the following example:
	 * </p>
	 * <p>
	 * User zooms to an country (say switzerland) and pans to the UK. Obviously in
	 * this case the scale with respect to the tile calculation must stay the
	 * same. The actual scale may reflect the correct scale but the tiles don't
	 * need to care about that.
	 * </p>
	 * <p>
	 * Then use zooms out to view whole world, centers on UK and Zooms back to
	 * same zoom level. The tiles should be reused and the view should be the same
	 * as before.
	 * </p>
	 * <p>
	 * If we were to always calculate the scale at the current location 2 things
	 * would happen:
	 * </p>
	 * <ul>
	 * <li>
	 * The zoom back the UK would be at a different scale and tiles would be
	 * re-rendered</li>
	 * <li>
	 * As we zoom north or south we would either:
	 * <ul>
	 * <li>
	 * Get popping as the display adjusted to the new closest zoom level</li>
	 * <li>
	 * Or it would appear we are zooming out as we pan north.</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * 
	 * @param bounds
	 *          the bounds in the world
	 * @param dimension
	 *          the size of the display
	 */
	public double calculateScale(Envelope bounds, Dimension dimension)
	{
		try
		{

			if (!(crs instanceof EngineeringCRS))
			{
				MathTransform transform = CRS.findMathTransform(crs,
						DefaultGeographicCRS.WGS84, true);

				Envelope latLongBounds = JTS.transform(bounds, transform);

				Coordinate latLongCenter = latLongBounds.centre();
				latLongBounds.translate(-latLongCenter.x, -latLongCenter.y);

				return RendererUtilities.calculateScale(new ReferencedEnvelope(
						latLongBounds, crs), dimension.width, dimension.height, dpi);
			}
			else
			{
				return RendererUtilities.calculateScale(new ReferencedEnvelope(bounds,
						crs), dimension.width, dimension.height, dpi);
			}
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

	public Tile[][] getTiles(Dimension screenSize, int scale, Coordinate center)
	{
		Envelope bounds = calculateBounds(screenSize, scale, center);

		Envelope tileBounds = calculateBounds(tileSize, scale, center);
		tileBounds.translate(-tileBounds.getMinX(), -tileBounds.getMinY());

		int xStartIndex = (int) Math
				.round((bounds.getMinX() - bottomLeft.x) / tileBounds.getWidth());
		int yStartIndex = (int) Math.round((bounds.getMinY() - bottomLeft.y)
				/ tileBounds.getHeight());

		double minX = (tileBounds.getWidth() * xStartIndex) + bottomLeft.x;
		double minY = (tileBounds.getHeight() * yStartIndex) + bottomLeft.y;

		double xWidth = (bounds.getMaxX() + bottomLeft.x) - minX;
		double yWidth = (bounds.getMaxY() + bottomLeft.y) - minY;

		int xTileCount = (int) Math.floor(xWidth / tileBounds.getWidth());
		int yTileCount = (int) Math.floor(yWidth / tileBounds.getHeight());

		ScaleLevelCache cache = getCache(scale);

		Tile[][] tiles = new Tile[xTileCount][yTileCount];
		for (int xIndex = xStartIndex; xIndex < xStartIndex+xTileCount; xIndex++)
		{
			for (int yIndex = yStartIndex; yIndex < yStartIndex+yTileCount; yIndex++)
			{
				ReferencedEnvelope currentTileBounds = new ReferencedEnvelope(tileBounds, crs);
				currentTileBounds.translate(tileBounds.getWidth() * xIndex + bottomLeft.x, tileBounds.getHeight() * yIndex + bottomLeft.y);

				tiles[xIndex - xStartIndex][yIndex - yStartIndex] = cache.getTile(xIndex, yIndex, currentTileBounds);
			}
		}

		return tiles;
	}

	/**
	 * @param scale
	 * @return 
	 */
	protected ScaleLevelCache getCache(int scale)
	{
		int index = cacheIndices.get(scale);
		ScaleLevelCache cache = caches[index];

		if (cache == null)
		{
			cache = new ScaleLevelCache();
			caches[index] = cache;
		}
		
		return cache;
	}

}
