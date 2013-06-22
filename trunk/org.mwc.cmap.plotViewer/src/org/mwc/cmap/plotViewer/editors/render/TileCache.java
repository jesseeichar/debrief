package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.renderer.lite.RendererUtilities;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
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

		public synchronized Tile getTile(int xIndex, int yIndex, double tileWidth,
				double tileHeight)
		{

			Map<Integer, Tile> row = tiles.get(xIndex);

			if (row == null)
			{
				row = new HashMap<Integer, Tile>();
				tiles.put(xIndex, row);
			}

			Tile tile = row.get(yIndex);

			if (tile == null)
			{
				double minx = bottomLeft.x + (xIndex * tileWidth);
				double miny = bottomLeft.y + (yIndex * tileHeight);
				ReferencedEnvelope bounds = new ReferencedEnvelope(minx, minx
						+ tileWidth, miny, miny + tileHeight, crs);
				tile = new Tile(loader, tileSize, bounds);
				tileCount++;
				row.put(yIndex, tile);
			}

			return tile;
		}
	}

	public static final int[] DEFAULT_WGS84 = new int[]
	{ 1, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10_000, 20_000, 50_000,
			100_000, 200_000, 500_000, 1_000_000, 2_000_000, 5_000_000, 10_000_000 };

	private static final double DEGREE_TO_METERS = 6378137.0;

	private Map<Integer, Integer> cacheIndices = new HashMap<Integer, Integer>();

	private final TileLoader loader;
	private ScaleLevelCache[] caches;
	private Dimension tileSize;
	private int[] scales;
	private Coordinate bottomLeft;

	private double dpi;

	private CoordinateReferenceSystem crs;

	private int _precision;

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
	 * @param loader
	 *          the strategy object used for loading the imagery of tiles.
	 */
	public TileCache(Dimension tileSize, int[] scales, Coordinate bottomLeft,
			double dpi, int precision, CoordinateReferenceSystem crs,
			TileLoader loader)
	{
		Assert.isTrue(loader != null, "Loader must be non-null");
		Assert.isTrue(scales != null, "Scales must be non-null");
		Assert.isTrue(tileSize != null, "tileSize must be non-null");
		Assert.isTrue(bottomLeft != null, "bottomLeft  must be non-null");
		Assert.isTrue(crs != null, "crs must be non-null");
		Assert.isTrue(dpi > 0, "dpi must be > 0");
		Assert.isTrue(precision > -2, "precision must be > -2");

		this.loader = loader;

		Arrays.sort(scales);
		this.tileSize = tileSize;
		this.scales = scales;
		this.bottomLeft = bottomLeft;
		this.dpi = dpi;
		this.crs = crs;
		this._precision = precision;
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

		double dScale = scale;
		double width;
		if (crs instanceof GeographicCRS)
		{
			width = (dScale * (screenSize.getWidth() / dpi * 0.0254)) / DEGREE_TO_METERS;
		}
		else
		{

			double pixelInMeters = this.dpi * 0.0254;
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
	public double calculateScale(Envelope bounds, Dimension dimension)
	{

		double scale;
		if (crs instanceof GeographicCRS)
		{
			scale = (bounds.getWidth() * DEGREE_TO_METERS)
					/ (dimension.getWidth() / dpi * 0.0254);
		}
		else
		{

			double pixelInMeters = this.dpi * 0.0254;
			scale = bounds.getWidth() / (dimension.getWidth() / pixelInMeters);
		}

		return scale;
	}

	/**
	 * Calculate and create/get tiles for the current view area.
	 * 
	 * @param screenSize
	 *          the size of the screen where the tiles will be drawn
	 * @param scale
	 *          the scale to draw at
	 * @param center
	 *          the center of the area to render (in world coordinates)
	 * @return the tiles in a x,y array of tiles. getTiles[x],[y] will get the
	 *         first tile to be drawn at the bottom left location.
	 */
	public Tile[][] getTiles(Dimension screenSize, int scale, Coordinate center)
	{
		int minXTileCount = (int) Math.ceil(((double) screenSize.width)
				/ tileSize.width);
		int minYTileCount = (int) Math.ceil(((double) screenSize.height)
				/ tileSize.height);

		Envelope bounds = calculateBounds(screenSize, scale, center);
		double rawXDistanceFromBottomLeft = bounds.getMinX() - bottomLeft.x;
		double rawYDistanceFromBottomLeft = bounds.getMinY() - bottomLeft.y;

		Envelope tileBounds = calculateBounds(tileSize, scale, center);
		double tileXDistanceFromBottomLeft = Math.floor(rawXDistanceFromBottomLeft
				/ tileBounds.getWidth())
				* tileBounds.getWidth();
		double tileYDistanceFromBottomLeft = Math.floor(rawYDistanceFromBottomLeft
				/ tileBounds.getHeight())
				* tileBounds.getHeight();

		Assert
				.isTrue(tileXDistanceFromBottomLeft <= rawXDistanceFromBottomLeft,
						"tiles should start at or before bounds.  This is not true for x axis.");
		Assert
				.isTrue(tileYDistanceFromBottomLeft <= rawYDistanceFromBottomLeft,
						"tiles should start at or before bounds.  This is not true for y axis.");

		Assert
				.isTrue(
						tileXDistanceFromBottomLeft + tileBounds.getWidth() >= rawXDistanceFromBottomLeft,
						"The end of first tile be within bounds.  This is not true for x axis.");
		Assert
				.isTrue(
						tileYDistanceFromBottomLeft + tileBounds.getHeight() >= rawYDistanceFromBottomLeft,
						"The end of first tile be within bounds.  This is not true for x axis.");

		int xStartIndex = (int) Math.round(tileXDistanceFromBottomLeft
				/ tileBounds.getWidth());
		int yStartIndex = (int) Math.round(tileYDistanceFromBottomLeft
				/ tileBounds.getHeight());

		int xTileCount = (int) Math.ceil((rawXDistanceFromBottomLeft
				+ bounds.getWidth() - tileXDistanceFromBottomLeft)
				/ tileBounds.getWidth());
		int yTileCount = (int) Math.ceil((rawYDistanceFromBottomLeft
				+ bounds.getHeight() - tileYDistanceFromBottomLeft)
				/ tileBounds.getHeight());

		Assert.isTrue(minXTileCount <= xTileCount
				&& minXTileCount + 1 >= xTileCount,
				"A programming error was detected.  " + minXTileCount + " should <= "
						+ xTileCount);
		Assert.isTrue(minYTileCount <= yTileCount
				&& minYTileCount + 1 >= yTileCount,
				"A programming error was detected.  " + minYTileCount + " should <= "
						+ yTileCount);

		double tileBoundsWidth = xTileCount * tileBounds.getWidth();
		double tileBoundsHeight = yTileCount * tileBounds.getHeight();
		Assert.isTrue(tileBoundsWidth >= bounds.getWidth()
				&& (tileBoundsWidth - tileBounds.getWidth()) < bounds.getWidth(),
				"Widths are not as expected: " + tileBoundsWidth + " should be >= "
						+ bounds.getWidth());
		Assert.isTrue(tileBoundsHeight >= bounds.getHeight()
				&& (tileBoundsHeight - tileBounds.getHeight()) < bounds.getHeight(),
				"Heights are not as expected: " + tileBoundsHeight + " should be >= "
						+ bounds.getHeight());

		ScaleLevelCache cache = getCache(scale);

		Tile[][] tiles = new Tile[xTileCount][yTileCount];
		for (int xIndex = xStartIndex; xIndex < xStartIndex + xTileCount; xIndex++)
		{
			for (int yIndex = yStartIndex; yIndex < yStartIndex + yTileCount; yIndex++)
			{
				ReferencedEnvelope currentTileBounds = new ReferencedEnvelope(
						tileBounds, crs);
				currentTileBounds.translate(tileBounds.getWidth() * xIndex
						+ bottomLeft.x, tileBounds.getHeight() * yIndex + bottomLeft.y);

				tiles[xIndex - xStartIndex][yIndex - yStartIndex] = cache.getTile(
						xIndex, yIndex, tileBounds.getWidth(), tileBounds.getHeight());
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
