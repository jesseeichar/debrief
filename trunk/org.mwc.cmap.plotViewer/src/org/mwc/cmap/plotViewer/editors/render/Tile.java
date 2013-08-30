package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;

import org.eclipse.swt.graphics.Image;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * The base interface for a tile.
 * 
 * @author Jesse
 * 
 */
public interface Tile
{

	/**
	 * Dispose of all resources the tile may be holding.
	 */
	public abstract void dispose();

	/**
	 * Get the tile bounds.
	 */
	public abstract ReferencedEnvelope getBounds();

	/**
	 * Get Size of tile
	 */
	public abstract Dimension getSize();

	/**
	 * Get current state of tile.
	 */
	public abstract TileState getState();

	/**
	 * Load and return image
	 * 
	 * @return
	 */
	public abstract Image load();

}