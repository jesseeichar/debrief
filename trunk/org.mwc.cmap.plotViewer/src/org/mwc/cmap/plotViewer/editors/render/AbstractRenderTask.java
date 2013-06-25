package org.mwc.cmap.plotViewer.editors.render;

import java.util.concurrent.Callable;

import org.mwc.cmap.gt2plot.proj.GtProjection;

import MWC.Algorithms.PlainProjection;
import MWC.GUI.CanvasType;

/**
 * Renders object and returns a tile containing the image.
 *
 * @author Jesse
 */
public abstract class AbstractRenderTask implements Callable<RenderTaskResult>
{
	protected CanvasType _destCanvas;
	protected PlainProjection _projection;
	public void setDestCanvas(CanvasType dest)
	{
		this._destCanvas = dest;
	}

	public void setDataProjection(PlainProjection projection)
	{
		this._projection = new GtProjection();
		this._projection.setDataArea(projection.getDataArea());
		this._projection.setDataBorder(projection.getDataBorder());
		this._projection.setName("RenderTask copied project: "+projection.getName());
		this._projection.setRelativeMode(projection.getPrimaryCentred(), projection.getPrimaryOriented());
		this._projection.setScreenArea(projection.getScreenArea());
	}
}
