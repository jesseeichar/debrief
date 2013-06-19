package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;

import org.eclipse.swt.graphics.Image;
import org.geotools.geometry.jts.ReferencedEnvelope;

public class Tile
{
	enum State {
		READY, BLANK, LOADING
	}
	
	
	private State state = State.BLANK;
	private Image image;
	private ReferencedEnvelope bounds;
	private Dimension size;
	private double scale;
	public synchronized void dispose(){
		if (image != null) {
			image.dispose();
		}
	}
}
