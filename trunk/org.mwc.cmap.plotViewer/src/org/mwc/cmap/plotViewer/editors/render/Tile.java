package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;

import org.eclipse.swt.graphics.Image;
import org.geotools.geometry.jts.ReferencedEnvelope;

import com.vividsolutions.jts.geom.Envelope;

/**
 * A tile of rendered (or to be rendered) map layer. 
 *
 * @author Jesse
 */
public class Tile
{
	/**
	 * The current state of the tile.
	 *
	 * @author Jesse
	 */
	enum State {
		READY, BLANK, LOADING, ERROR
	}
	
	
	private State state = State.BLANK;
	private Image image;
	private final ReferencedEnvelope bounds;
	private final Dimension size;
	private final TileLoader loader;

	public Tile(TileLoader loader, Dimension tileSize, ReferencedEnvelope bounds2)
	{
		this.loader = loader;
		this.size = tileSize;
		this.bounds = bounds2;
	}
	public synchronized void dispose(){
		if (image != null) {
			image.dispose();
		}
	}
	@Override
	public String toString()
	{
		return "Tile [" + state + ", " + bounds + "]";
	}
	/**
	 * Get the tile bounds.
	 */
	public ReferencedEnvelope getBounds()
	{
		return bounds;
	}
	
	/**
	 * Get Size of tile
	 */
	public Dimension getSize()
	{
		return size;
	}

	/**
	 * Get current state of tile.
	 */
	public State getState()
	{
		return state;
	}
	
}
