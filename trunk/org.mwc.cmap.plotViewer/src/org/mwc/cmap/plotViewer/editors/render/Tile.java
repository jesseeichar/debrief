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
}
