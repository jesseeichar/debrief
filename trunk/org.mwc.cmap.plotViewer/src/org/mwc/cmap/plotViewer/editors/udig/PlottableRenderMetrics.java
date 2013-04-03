package org.mwc.cmap.plotViewer.editors.udig;

import java.io.IOException;
import java.util.Collections;

import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.internal.render.Renderer;
import net.refractions.udig.project.render.AbstractRenderMetrics;
import net.refractions.udig.project.render.IRenderContext;
import net.refractions.udig.project.render.IRenderMetricsFactory;

import org.eclipse.core.runtime.NullProgressMonitor;

public class PlottableRenderMetrics extends AbstractRenderMetrics
{

	protected PlottableRenderMetrics(IRenderContext context,
			IRenderMetricsFactory factory)
	{
		super(context, factory, Collections.<String> emptyList());
	}

	@Override
	public boolean canAddLayer(ILayer layer)
	{
		try
		{
			return layer.getResource(MWC.GUI.Layer.class, new NullProgressMonitor()) != null;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean canStyle(String styleID, Object value)
	{
		return true;
	}

	@Override
	public Renderer createRenderer()
	{
		PlottableRenderer renderer = new PlottableRenderer();
		renderer.setContext(context);
		return renderer;
	}
}
