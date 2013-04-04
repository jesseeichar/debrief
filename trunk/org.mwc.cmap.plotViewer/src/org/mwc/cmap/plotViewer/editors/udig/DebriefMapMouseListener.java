package org.mwc.cmap.plotViewer.editors.udig;

import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseListener;

public class DebriefMapMouseListener implements MapMouseListener
{

	private CorePlotEditor _editor;

	public DebriefMapMouseListener(CorePlotEditor	editor)
	{
		this._editor = editor;
	}

	@Override
	public void mousePressed(MapMouseEvent event)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseReleased(MapMouseEvent event)
	{
		if(event.buttons == 0 && event.button == MapMouseEvent.BUTTON3) {
			_editor.openContextMenu(event.x, event.y);
		}

	}

	@Override
	public void mouseEntered(MapMouseEvent event)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MapMouseEvent event)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseDoubleClicked(MapMouseEvent event)
	{
		// TODO Auto-generated method stub

	}

}
