package org.mwc.cmap.plotViewer.editors.udig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.refractions.udig.project.internal.render.impl.MultiLayerRendererImpl;
import net.refractions.udig.project.render.ICompositeRenderContext;
import net.refractions.udig.project.render.IRenderContext;
import net.refractions.udig.project.render.RenderException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import MWC.Algorithms.PlainProjection;
import MWC.GUI.CanvasType;
import MWC.GUI.Layer;
import MWC.GUI.Canvas.CanvasAdaptor;

public class PlottableRenderer extends MultiLayerRendererImpl
{

	@Override
	public ICompositeRenderContext getContext()
	{
		return (ICompositeRenderContext) super.getContext();
	}

	List<MWC.GUI.Layer> getLayers()
	{
		LinkedList<Layer> layers = new LinkedList<MWC.GUI.Layer>();

		for (IRenderContext c : getContext().getContexts())
		{
			try
			{
				layers.add(c.getGeoResource().resolve(MWC.GUI.Layer.class,
						new NullProgressMonitor()));
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}

		return layers;
	}

	@Override
	public void render(Graphics2D destination, IProgressMonitor monitor)
			throws RenderException
	{
		
		destination.setColor(Color.RED);

		PlainProjection proj = new UDigRendererProjection(getContext());
		proj.setDataArea(JtsAdapter.toWorldArea(context.getViewportModel().getBounds()));
		proj.setScreenArea(context.getImageSize());

		CanvasType dest = new CanvasAdaptor(proj, destination);
		
		for (MWC.GUI.Layer layer : getLayers())
		{
			layer.paint(dest);
		}
	}

	@Override
	public void render(IProgressMonitor monitor) throws RenderException
	{
		Graphics2D graphics2d = getContext().getImage().createGraphics();
		try
		{
			render(graphics2d, monitor);
		}
		finally
		{
			graphics2d.dispose();
		}
	}
}
