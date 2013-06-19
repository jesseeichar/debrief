package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;

import org.eclipse.swt.graphics.Image;
import org.geotools.geometry.jts.ReferencedEnvelope;

public interface TileLoader
{
	Image load(Dimension tileSize, ReferencedEnvelope envelope);
}
