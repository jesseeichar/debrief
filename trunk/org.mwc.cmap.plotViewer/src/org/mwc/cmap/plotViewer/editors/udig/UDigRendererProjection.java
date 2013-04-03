package org.mwc.cmap.plotViewer.editors.udig;

import java.awt.Point;

import net.refractions.udig.project.render.ICompositeRenderContext;

import MWC.Algorithms.PlainProjection;
import MWC.GenericData.WorldLocation;

public class UDigRendererProjection extends PlainProjection
{

	private static final long serialVersionUID = 1L;
	private ICompositeRenderContext context;
	private double zoom;

	public UDigRendererProjection(ICompositeRenderContext context)
	{
		super("udig");
		this.context = context;
	}

	@Override
	public Point toScreen(WorldLocation val)
	{
		return context.worldToPixel(JtsAdapter.toCoord(val));
	}

	@Override
	public WorldLocation toWorld(Point val)
	{
		return JtsAdapter.toWorldLocation(context.pixelToWorld(val.x, val.y));
	}

	@Override
	public void zoom(double value)
	{
		this.zoom = value;

	}

}
