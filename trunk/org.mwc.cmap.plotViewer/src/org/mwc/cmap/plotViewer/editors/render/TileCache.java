package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Image;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.renderer.lite.RendererUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.Assert;

/**
 * A tile cache for a single layer.
 * 
 * A Tile cache applies only to a single layer.
 * 
 * @author Jesse
 * 
 */
public class TileCache extends TileCacheSupport
{
	private class ScaleLevelCache
	{
		/**
		 * cached tiles. x index, y index. Can access the tile at x,y by doing:
		 * _tiles.get(x).get(y);
		 */
		private Map<Integer, Map<Integer, CachableTile>> _tiles = new HashMap<Integer, Map<Integer, CachableTile>>();
		volatile int _tileCount = 0;

		private synchronized CachableTile getTile(int xIndex, int yIndex,
				double tileWidth, double tileHeight)
		{

			Map<Integer, CachableTile> tileColumn = _tiles.get(xIndex);

			if (tileColumn == null)
			{
				tileColumn = new HashMap<Integer, CachableTile>();
				_tiles.put(xIndex, tileColumn);
			}

			CachableTile tile = tileColumn.get(yIndex);

			if (tile == null)
			{
				double minx = _bottomLeft.x + (xIndex * tileWidth);
				double miny = _bottomLeft.y + (yIndex * tileHeight);
				ReferencedEnvelope bounds = new ReferencedEnvelope(minx, minx
						+ tileWidth, miny, miny + tileHeight, _crs);
				tile = new CachableTile(_loader, _tileSize, bounds, _errorImage);
				_tileCount++;
				tileColumn.put(yIndex, tile);
			}

			if (tile.getState() == TileState.DISPOSED)
			{
				System.out.println("Tile has been disposed");
			}
			return tile;
		}

		public void dispose()
		{
			_tileCount = 0;
			Collection<Map<Integer, CachableTile>> values = _tiles.values();
			for (Map<Integer, CachableTile> tileColumn : values)
			{
				Collection<CachableTile> tiles = tileColumn.values();
				for (Tile tile : tiles)
				{
					tile.dispose();
				}
				tiles.clear();
			}
			values.clear();

			_errorImage.dispose();
		}
	}

	private static final Coordinate ORIGIN = new Coordinate(0, 0);
	private static final String TILE_CACHE_DEBUG = "tileCacheDebug";
	/**
	 * Scale -> index in {@link #_caches}
	 */
	private final Map<Double, Integer> _cacheIndices = new HashMap<Double, Integer>();
	private ScaleLevelCache[] _caches;

	private TileLoader _loader;
	private Image _errorImage;

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
	 * @param errorImage
	 *          An image to display when there is an error loading a tile.
	 */
	public TileCache(Dimension tileSize, double[] scales, Coordinate bottomLeft,
			double dpi, int precision, CoordinateReferenceSystem crs,
			TileLoader loader, Image errorImage)
	{
		super(tileSize, scales, bottomLeft, dpi, precision, crs);

		Assert.isTrue(loader != null, "Loader must be non-null");
		this._loader = loader;
		this._errorImage = errorImage;

		_caches = new ScaleLevelCache[scales.length];
		for (int i = 0; i < scales.length; i++)
		{
			_cacheIndices.put(scales[i], i);
		}
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
	 *          The smallest envelope to be allowed
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
	 * @param loader
	 *          the strategy object used for loading the imagery of tiles.
	 * @param errorImage
	 *          An image to display when there is an error loading a tile.
	 */
	public TileCache(Dimension tileSize, int numScales, Envelope minEnvelope,
			Envelope maxEnvelope, double dpi, int precision,
			CoordinateReferenceSystem crs, TileLoader loader, Image errorImage)
	{
		this(tileSize, numScales, calculateScale(minEnvelope, tileSize, crs, dpi),
				maxEnvelope, dpi, precision, crs, loader, errorImage);
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
	 *          The smallest scale to allow.
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
	 * @param loader
	 *          the strategy object used for loading the imagery of tiles.
	 * @param errorImage
	 *          An image to display when there is an error loading a tile.
	 */
	public TileCache(Dimension tileSize, int numScales, double minScale,
			Envelope maxEnvelope, double dpi, int precision,
			CoordinateReferenceSystem crs, TileLoader loader, Image errorImage)
	{
		super(tileSize, numScales, minScale, maxEnvelope, dpi, precision, crs);

		Assert.isTrue(loader != null, "Loader must be non-null");
		this._loader = loader;
		this._errorImage = errorImage;

		_caches = new ScaleLevelCache[_scales.length];
		for (int i = 0; i < _scales.length; i++)
		{
			_cacheIndices.put(_scales[i], i);
		}
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
	 * @return the tiles in a x,y array of tiles. getTiles[0][0] will get the
	 *         first tile to be drawn at the bottom left location.
	 */
	public synchronized PositionedTile[][] getTiles(Dimension screenSize,
			double scale, Coordinate center)
	{
		int minXTileCount = (int) Math.ceil(((double) screenSize.width)
				/ _tileSize.width);
		int minYTileCount = (int) Math.ceil(((double) screenSize.height)
				/ _tileSize.height);

		Envelope bounds = calculateBounds(screenSize, scale, center);
		double rawXDistanceFromBottomLeft = bounds.getMinX() - _bottomLeft.x;
		double rawYDistanceFromBottomLeft = bounds.getMinY() - _bottomLeft.y;

		if (rawXDistanceFromBottomLeft < 0 || rawYDistanceFromBottomLeft < 0)
		{
			throw new IllegalArgumentException(
					"The lower left corner of the display area [" + bounds
							+ "] cannot be beyond: [" + _bottomLeft + "]");
		}

		Envelope tileBounds = calculateBounds(_tileSize, scale, ORIGIN);
		tileBounds.translate(-tileBounds.getMinX(), -tileBounds.getMinY());

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
		Assert.isTrue(tileBoundsWidth >= bounds.getWidth(),
				"Widths are not as expected: " + tileBoundsWidth + " should be >= "
						+ bounds.getWidth());
		Assert.isTrue(tileBoundsHeight >= bounds.getHeight(),
				"Heights are not as expected: " + tileBoundsHeight + " should be >= "
						+ bounds.getHeight());

		ScaleLevelCache cache = getCache(scale);

		AffineTransform2D worldToScreenTransform;
		try
		{
			worldToScreenTransform = new AffineTransform2D(
					RendererUtilities.worldToScreenTransform(bounds, new Rectangle(
							screenSize), _crs));
		}
		catch (TransformException e)
		{
			throw new RuntimeException(e);
		}

		PositionedTile[][] tiles = new PositionedTile[xTileCount][yTileCount];
		for (int xIndex = xStartIndex; xIndex < xStartIndex + xTileCount; xIndex++)
		{
			for (int yIndex = yStartIndex; yIndex < yStartIndex + yTileCount; yIndex++)
			{
				ReferencedEnvelope currentTileBounds = new ReferencedEnvelope(
						tileBounds, _crs);
				currentTileBounds.translate(tileBounds.getWidth() * xIndex
						+ _bottomLeft.x, tileBounds.getHeight() * yIndex + _bottomLeft.y);

				Tile tile = cache.getTile(xIndex, yIndex, tileBounds.getWidth(),
						tileBounds.getHeight());
				tiles[xIndex - xStartIndex][yIndex - yStartIndex] = new PositionedTile(
						tile, worldToScreenTransform);
			}
		}

		return tiles;
	}

	/**
	 * Get the internal cache for the given scale level.
	 * 
	 * @param scale
	 * @return
	 */
	protected synchronized ScaleLevelCache getCache(double scale)
	{
		int index = _cacheIndices.get(scale);
		ScaleLevelCache cache = _caches[index];

		if (cache == null)
		{
			cache = new ScaleLevelCache();
			_caches[index] = cache;
		}

		return cache;
	}

	/**
	 * Dispose of all held resources. Will dispose of all tiles.
	 */
	public synchronized void dispose()
	{
		Collection<Integer> indices = _cacheIndices.values();
		for (Integer index : indices)
		{
			ScaleLevelCache nextCache = _caches[index];
			if (nextCache != null)
			{
				nextCache.dispose();
			}
		}
		_caches = null;
		_cacheIndices.clear();
	}

	/**
	 * Return if the system is in debug mode. Debug mode can be enabled by setting
	 * system property {@value #TILE_CACHE_DEBUG}. This can conveniently be done
	 * with a -D parameter on startup.
	 * 
	 * When debug is enabled the borders of each tile is drawn along with the
	 * bounding box.
	 */
	public static boolean isDebug()
	{
		return Boolean.parseBoolean(System.getProperty(TILE_CACHE_DEBUG, "false"));
	}

}
