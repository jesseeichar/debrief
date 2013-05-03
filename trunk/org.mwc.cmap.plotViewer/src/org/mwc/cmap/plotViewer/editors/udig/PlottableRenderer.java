package org.mwc.cmap.plotViewer.editors.udig;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.refractions.udig.project.internal.render.ViewportModel;
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
		
		configureGraphics(destination);
		
		UDigRendererProjection proj = new UDigRendererProjection();
		proj.setViewportModel((ViewportModel) getContext().getMap().getViewportModel());

		CanvasType dest = new CanvasAdaptor(proj, destination) {
			@Override
			public void setLineWidth(float width)
			{
				if (width <= 1) {
					width += .5;
				}
				super.setLineWidth(width);
			}
		};
		
		for (MWC.GUI.Layer layer : getLayers())
		{
			synchronized (layer)
			{
				layer.paint(dest);
			}
		}
	}

	private void configureGraphics(Graphics2D destination)
	{
		RenderingHints hints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
    hints.add(new RenderingHints(RenderingHints.KEY_DITHERING,
            RenderingHints.VALUE_DITHER_DISABLE));
    hints.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION,
            RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED));
    hints.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING,
            RenderingHints.VALUE_COLOR_RENDER_SPEED));
    hints.add(new RenderingHints(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR));
    hints.add(new RenderingHints(RenderingHints.KEY_STROKE_CONTROL,
            RenderingHints.VALUE_STROKE_PURE));
    hints.add(new RenderingHints(RenderingHints.KEY_FRACTIONALMETRICS,
            RenderingHints.VALUE_FRACTIONALMETRICS_OFF));

    hints.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));

    destination.addRenderingHints(hints);
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
