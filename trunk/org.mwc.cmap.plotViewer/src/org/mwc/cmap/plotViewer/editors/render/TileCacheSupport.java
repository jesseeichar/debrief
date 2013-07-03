package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;
import java.util.Arrays;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;

public abstract class TileCacheSupport
{
	public static final double[] DEFAULT_WGS84 = new double[]
	{ 1, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10_000, 20_000, 50_000,
			100_000, 200_000, 500_000, 1_000_000, 2_000_000, 5_000_000, 10_000_000, 
			20_000_000, 50_000_000, 100_000_000, 200_000_000, 500_000_000, 1_000_000_000, 
			2_000_000_000};

	private static final double DEGREE_TO_METERS = 6378137.0 * 2.0 * Math.PI / 360;

	protected final Dimension _tileSize;
	protected final double[] _scales;
	protected final Coordinate _bottomLeft;
	protected final double _dpi;
	protected final CoordinateReferenceSystem _crs;
	protected final int _precision;

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
	 * @param precision
	 *          the number of decimal digits. -1 will be maximum double precision,
	 *          0 is no decimals and any positive integer will define the number
	 *          of decimal places up-to the maximum supported by double
	 */
	public TileCacheSupport(Dimension tileSize, double[] scales,
			Coordinate bottomLeft, double dpi, int precision,
			CoordinateReferenceSystem crs)
	{
		Assert.isTrue(scales != null, "Scales must be non-null");
		Assert.isTrue(tileSize != null, "tileSize must be non-null");
		Assert.isTrue(bottomLeft != null, "bottomLeft  must be non-null");
		Assert.isTrue(crs != null, "crs must be non-null");
		Assert.isTrue(dpi > 0, "dpi must be > 0");
		Assert.isTrue(precision > -2, "precision must be > -2");

		Arrays.sort(scales);
		this._tileSize = tileSize;
		this._scales = scales;
		this._bottomLeft = bottomLeft;
		this._dpi = dpi;
		this._crs = crs;
		this._precision = precision;
	}
	
	/**
	 * Constructor
	 * 
	 * 
	 * @param tileSize
	 *          size of the tiles
	 * @param numScales
	 *          the number of scales to generate
	 * @param minScale
	 * 				  The smallest scale to allow.
	 * @param maxEnvelope
	 *          The maximum envelope to be allowed
	 * @param dpi
	 *          dpi of the display
	 * @param crs
	 *          the crs of the cache
	 * @param precision
	 *          the number of decimal digits. -1 will be maximum double precision,
	 *          0 is no decimals and any positive integer will define the number
	 *          of decimal places up-to the maximum supported by double
	 */
	public TileCacheSupport(Dimension tileSize, int numScales,
			double minScale, Envelope maxEnvelope, double dpi, int precision,
			CoordinateReferenceSystem crs)
	{
		Assert.isTrue(numScales > 0, "num scales must be greater than 0");
		Assert.isTrue(tileSize != null, "tileSize must be non-null");
		Assert.isTrue(minScale > 0, "minScale must be greater than 0");
		Assert.isTrue(maxEnvelope != null, "maxEnvelope  must be non-null");
		Assert.isTrue(crs != null, "crs must be non-null");
		Assert.isTrue(dpi > 0, "dpi must be > 0");
		Assert.isTrue(precision > -2, "precision must be > -2");

		double maxScale = calculateScale(maxEnvelope, tileSize, crs, dpi);

		double scaleStep = Math.max(1, (maxScale - minScale) / numScales);
		double[] scales = new double[numScales];
		for (int i = 0; i < scales.length; i++)
		{
			scales[i] = minScale + (scaleStep * i);
		}
		this._tileSize = tileSize;
		this._scales = scales;
		this._bottomLeft = new Coordinate(maxEnvelope.getMinX(), maxEnvelope.getMinY());
		this._dpi = dpi;
		this._crs = crs;
		this._precision = precision;
	}

	/**
	 * Constructor
	 * 
	 * 
	 * @param tileSize
	 *          size of the tiles
	 * @param numScales
	 *          the number of scales to generate
	 * @param minEnvelope 
	 * 				  The smallest envelope to be allowed
	 * @param maxEnvelope
	 *          The maximum envelope to be allowed
	 * @param dpi
	 *          dpi of the display
	 * @param crs
	 *          the crs of the cache
	 * @param precision
	 *          the number of decimal digits. -1 will be maximum double precision,
	 *          0 is no decimals and any positive integer will define the number
	 *          of decimal places up-to the maximum supported by double
	 */
	public TileCacheSupport(Dimension tileSize, int numScales,
			Envelope minEnvelope, Envelope maxEnvelope, double dpi, int precision,
			CoordinateReferenceSystem crs)
	{
		this(tileSize, numScales, calculateScale(minEnvelope, tileSize, crs, dpi), maxEnvelope, dpi, precision, crs);
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
		double scale = calculateScale(envelope, dimension);
		for (int i = 0; i < _scales.length; i++)
		{
			double nextScale = _scales[i];
			if (nextScale > scale)
			{
				if (i == 0)
				{
					return _scales[0];
				}
				else
				{
					double diff1 = scale - _scales[i - 1];
					double diff2 = nextScale - scale;
					if (diff1 < diff2)
					{
						return _scales[i - 1];
					}
					else
					{
						return nextScale;
					}
				}
			}
		}
		return _scales[_scales.length - 1];
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
	public Envelope calculateBounds(Dimension screenSize, double scale,
			Coordinate center)
	{

		double dScale = scale;
		double width;
		if (_crs instanceof GeographicCRS)
		{
			width = (dScale * (screenSize.getWidth() / _dpi * 0.0254))
					/ DEGREE_TO_METERS;
		}
		else
		{

			double pixelInMeters = this._dpi * 0.0254;
			width = dScale * (screenSize.getWidth() / pixelInMeters);
		}
		double xRad = width / 2;
		double ratio = screenSize.getHeight() / screenSize.getWidth();
		double yRad = ratio * xRad;
		Envelope rawEnv = new Envelope(center.x - xRad, center.x + xRad, center.y
				- yRad, center.y + yRad);
		return reducePrecision(rawEnv);
	}

	private Envelope reducePrecision(Envelope env)
	{
		double minx = round(env.getMinX());
		double maxx = round(env.getMaxX());
		double miny = round(env.getMinY());
		double maxy = round(env.getMaxY());
		return new Envelope(minx, maxx, miny, maxy);
	}

	private double round(double num)
	{
		if (_precision == 0)
		{
			return Math.round(num);
		}
		else if (_precision > 0)
		{
			long slide = 10L ^ _precision;
			double rounded = Math.round(num * slide);
			return rounded / slide;
		}
		return num;
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
	public synchronized double calculateScale(Envelope bounds, Dimension dimension)
	{
		return calculateScale(bounds, dimension, _crs, _dpi);
	}

	public static double calculateScale(Envelope bounds, Dimension dimension,
			CoordinateReferenceSystem crs, double dpi)
	{
		double scale;
		if (crs instanceof GeographicCRS)
		{
			scale = (bounds.getWidth() * DEGREE_TO_METERS)
					/ (dimension.getWidth() / dpi * 0.0254);
		}
		else
		{

			double pixelInMeters = dpi * 0.0254;
			scale = bounds.getWidth() / (dimension.getWidth() / pixelInMeters);
		}

		return scale;
	}
	
	public double[] getScales()
	{
		return _scales.clone();
	}

}
