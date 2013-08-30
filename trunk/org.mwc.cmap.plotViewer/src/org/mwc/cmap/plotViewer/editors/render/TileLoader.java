package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;

import org.eclipse.swt.graphics.Image;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * Interface for strategies that load tile images. A Tile Loader is used by a
 * Tile for loading the tile image when it has not been cached.
 * 
 * @author Jesse
 */
public interface TileLoader
{
	/**
	 * Load the image of the given size at the particular world location.
	 * 
	 * @param tileSize
	 *          the size of the image to load
	 * @param envelope
	 *          the part of the world to render.
	 * 
	 * @return the tile image.
	 */
	Image load(Dimension tileSize, ReferencedEnvelope envelope);
}
