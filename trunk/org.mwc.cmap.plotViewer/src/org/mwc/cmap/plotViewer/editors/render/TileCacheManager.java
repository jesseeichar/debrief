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
	public TileCacheManager(Dimension tileSize, int[] scales,
			Coordinate bottomLeft, double dpi, int precision,
			CoordinateReferenceSystem crs)
	{
		super(tileSize, scales, bottomLeft, dpi, precision, crs);
	}

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

	public void remove(Layer changedLayer)
	{
		// get the image
		TileCache theCache = _tileCache.get(changedLayer);

		// and ditch the image
		if (theCache != null)
		{
			// dispose of the image
			theCache.dispose();

			// and delete that layer
			_tileCache.remove(changedLayer);
		}
	}

	public TileCache getTileCache(Layer thisLayer, ImageData imageData)
	{
		TileCache tileCache = _tileCache.get(thisLayer);
		if (tileCache == null)
		{
			LayerTileLoader loader = new LayerTileLoader(thisLayer);
			Image errorImage = new Image(Display.getDefault(), imageData);
			
			GC gc = new GC(errorImage);
			Color color = new Color(errorImage.getDevice(), 255, 0, 0);
			gc.setBackground(color);
			gc.setAlpha(128);
			gc.fillRectangle(0, 0, imageData.width, imageData.height);
			gc.dispose();

			tileCache = new TileCache(_tileSize, _scales, _bottomLeft, _dpi,
					_precision, _crs, loader, errorImage);
			_tileCache.put(thisLayer, tileCache);
		}
		return tileCache;
	}

}
