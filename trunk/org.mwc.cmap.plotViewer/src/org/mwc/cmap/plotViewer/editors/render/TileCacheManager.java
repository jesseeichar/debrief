package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.mwc.cmap.core.CorePlugin;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import MWC.GUI.Layer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * Manages Tile caches for several layers.
 * 
 * @author Jesse
 * 
 */
public class TileCacheManager extends TileCacheSupport
{
	private Map<Layer, TileCache> _tileCache = new IdentityHashMap<Layer, TileCache>();

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
	public TileCacheManager(Dimension tileSize, double[] scales,
			Coordinate bottomLeft, double dpi, int precision,
			CoordinateReferenceSystem crs)
	{
		super(tileSize, scales, bottomLeft, dpi, precision, crs);
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
	 */
	public TileCacheManager(Dimension tileSize, int numScales,
			Envelope minEnvelope, Envelope maxEnvelope, double dpi, int precision,
			CoordinateReferenceSystem crs)
	{
		super(tileSize, numScales, minEnvelope, maxEnvelope, dpi, precision, crs);
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
	 */
	public TileCacheManager(Dimension tileSize, int numScales, double minScale,
			Envelope maxEnvelope, double dpi, int precision,
			CoordinateReferenceSystem crs)
	{
		super(tileSize, numScales, minScale, maxEnvelope, dpi, precision, crs);
	}

	/**
	 * Dispose of all tile caches (and associated resources).
	 */
	public void clear()
	{
		// tell the images to clear themselves out
		Iterator<TileCache> iter = _tileCache.values().iterator();
		while (iter.hasNext())
		{
			Object nextI = iter.next();
			if (nextI instanceof TileCache)
			{
				try
				{
					TileCache thisCache = (TileCache) nextI;
					thisCache.dispose();
				}
				catch (Throwable t)
				{
					t.printStackTrace();
				}
			}
			else
			{
				CorePlugin.logError(IStatus.ERROR,
						"unexpected type of image found in buffer:" + nextI, null);
			}
		}

		// and clear out our buffered layers (they all need to be repainted
		// anyway)
		_tileCache.clear();
	}

	/**
	 * Dispose of the tile cache for a given layer.
	 * 
	 * @param layer
	 *          the layer whose tile cache needs to be disposed of.
	 */
	public void remove(Layer layer)
	{
		// get the image
		TileCache theCache = _tileCache.get(layer);

		// and ditch the image
		if (theCache != null)
		{
			// dispose of the image
			theCache.dispose();

			// and delete that layer
			_tileCache.remove(layer);
		}
	}

	/**
	 * Get the tile cache for a given layer. The tile cache will be created if
	 * necessary.
	 * 
	 * @param layer
	 *          the layer whose tile cache needs to be retrieved.
	 * @param errorImageData
	 *          image data to use when creating the error image (for tiles that
	 *          fail to load).
	 */
	public TileCache getTileCache(Layer layer, ImageData errorImageData)
	{
		TileCache tileCache = _tileCache.get(layer);
		if (tileCache == null)
		{
			LayerTileLoader loader = new LayerTileLoader(layer);
			Image errorImage = new Image(Display.getDefault(), errorImageData);

			GC gc = new GC(errorImage);
			Color color = new Color(errorImage.getDevice(), 255, 0, 0);
			gc.setBackground(color);
			gc.setAlpha(128);
			gc.fillRectangle(0, 0, errorImageData.width, errorImageData.height);
			gc.dispose();

			tileCache = new TileCache(_tileSize, _scales, _bottomLeft, _dpi,
					_precision, _crs, loader, errorImage);
			_tileCache.put(layer, tileCache);
		}
		return tileCache;
	}

}
