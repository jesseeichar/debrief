package org.mwc.cmap.plotViewer.editors.udig;

import java.io.IOException;

import net.refractions.udig.project.render.AbstractRenderMetrics;
import net.refractions.udig.project.render.IRenderContext;
import net.refractions.udig.project.render.IRenderMetricsFactory;
import net.refractions.udig.project.render.IRenderer;

public class PlottableRenderMetricsFactory implements IRenderMetricsFactory
{

	@Override
	public boolean canRender(IRenderContext context) throws IOException
	{
		return context.getGeoResource().canResolve(MWC.GUI.Layer.class);
	}

	@Override
	public AbstractRenderMetrics createMetrics(IRenderContext context)
	{
		return new PlottableRenderMetrics(context, this);
	}

	@Override
	public Class<? extends IRenderer> getRendererType()
	{
		return PlottableRenderer.class;
	}

}
