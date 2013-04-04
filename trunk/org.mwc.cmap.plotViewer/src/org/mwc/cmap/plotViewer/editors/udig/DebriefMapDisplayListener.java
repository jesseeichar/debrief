package org.mwc.cmap.plotViewer.editors.udig;

import net.refractions.udig.project.render.displayAdapter.IMapDisplayListener;
import net.refractions.udig.project.render.displayAdapter.MapDisplayEvent;

public class DebriefMapDisplayListener implements IMapDisplayListener
{

	private CorePlotEditor _editor;

	public DebriefMapDisplayListener(CorePlotEditor editor)
	{
		this._editor = editor;
	}

	@Override
	public void sizeChanged(MapDisplayEvent event)
	{
		// TODO Auto-generated method stub

	}

}
