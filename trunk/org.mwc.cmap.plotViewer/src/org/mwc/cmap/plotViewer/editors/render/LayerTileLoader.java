package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Color;
import java.awt.Dimension;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mwc.cmap.core.ui_support.swt.SWTCanvasAdapter;
import org.mwc.cmap.gt2plot.data.JtsAdapter;
import org.mwc.cmap.gt2plot.proj.GtProjection;

import MWC.Algorithms.PlainProjection;
import MWC.GUI.CanvasType;
import MWC.GUI.Layer;

/**
 * A TileLoader for rendering a Layer (or the correct portion of the layer) to a
 * tile.
 * 
 * @author Jesse
 * 
 */
public class LayerTileLoader implements TileLoader
{

	private Layer _layer;

	public LayerTileLoader(Layer layer)
	{
		this._layer = layer;
	}

	@Override
	public Image load(Dimension tileSize, ReferencedEnvelope envelope)
	{
		PlainProjection proj = new GtProjection();
		proj.setScreenArea(tileSize);
		proj.setDataArea(JtsAdapter.toWorldArea(envelope));

		CanvasType dest = new SWTCanvasAdapter(proj);
		Image image = new Image(Display.getDefault(), tileSize.width,
				tileSize.height);
		GC gc = new GC(image);
		try
		{
			dest.startDraw(gc);
			_layer.paint(dest);

			if (TileCache.isDebug())
			{
				dest.setColor(Color.RED);
				dest.setLineWidth(2);
				gc.setAlpha(100);
				gc.fillRectangle(0, 0, tileSize.width - 1, tileSize.height - 1);
				gc.setAlpha(255);
				dest.setColor(Color.WHITE);
				String xbounds = "x:" + round(envelope.getMinX()) + " : "
						+ round(envelope.getMaxX());
				String ybounds = ", y:" + round(envelope.getMinY()) + " : "
						+ round(envelope.getMaxY());
				int quarterHeight = tileSize.height / 4;
				dest.drawText(xbounds + ybounds, 2, quarterHeight * 3);
			}
			return image;
		}
		finally
		{
			if (image != null)
			{
				dest.endDraw(gc);
				gc.dispose();
			}
		}
	}

	private double round(double num)
	{
		int precision = 1000;
		int rounded = (int) (num * precision);
		return ((double) rounded) / precision;
	}
}
