package org.mwc.cmap.plotViewer.editors.udig;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.List;

import net.refractions.udig.project.ILayer;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.mwc.cmap.core.ui_support.udig.ControlCanvasType;

import MWC.GUI.Layer;

public class UdigChart extends InteractiveChart
{

	private static final long serialVersionUID = 1L;
	private CorePlotEditor _editor;

	public UdigChart(CorePlotEditor editor)
	{
		super(editor.getLayers());
		this._editor = editor;
	}

	@Override
	public void rescale()
	{
		_editor._canvas.rescale();
	}

	@Override
	public void update()
	{
		_editor._viewer.getRenderManager().refresh(null);
	}

	@Override
	public void update(Layer changedLayer)
	{
		List<ILayer> layers = _editor._map.getMapLayers();

		NullProgressMonitor monitor = new NullProgressMonitor();
		for (ILayer iLayer : layers)
		{
			try
			{
				Layer layer = iLayer.getResource(Layer.class, monitor);
				if (layer == changedLayer)
				{
					iLayer.refresh(null);
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void repaint()
	{
		_editor._viewer.getViewport().repaint();
	}

	@Override
	public void repaintNow(Rectangle rect)
	{
		_editor._viewer.getViewport().repaint(rect.x, rect.y, rect.width,
				rect.height);
	}

	@Override
	public Dimension getScreenSize()
	{
		return _editor._viewer.getViewport().getDisplaySize();
	}

	@Override
	public ControlCanvasType getCanvas()
	{
		return _editor._canvas;
	}

	@Override
	public Component getPanel()
	{
		throw new UnsupportedOperationException("Method no implemented for a SWT based layer");
	}

}
