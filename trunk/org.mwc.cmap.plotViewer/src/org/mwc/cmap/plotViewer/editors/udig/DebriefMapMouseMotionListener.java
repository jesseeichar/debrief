package org.mwc.cmap.plotViewer.editors.udig;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;

import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseMotionListener;

public class DebriefMapMouseMotionListener implements MapMouseMotionListener
{

	private CorePlotEditor _editor;

	public DebriefMapMouseMotionListener(CorePlotEditor editor)
	{
		this._editor = editor;
	}

	@Override
	public void mouseMoved(MapMouseEvent event)
	{
		MouseEvent me = convertToMouseEvent(event, _editor);
		_editor.getChart().doMouseMove(me);
	}

	@Override
	public void mouseDragged(MapMouseEvent event)
	{
		MouseEvent me = convertToMouseEvent(event, _editor);
		_editor.getChart().doMouseMove(me);
	}

	@Override
	public void mouseHovered(MapMouseEvent event)
	{
		// TODO Auto-generated method stub

	}

	static MouseEvent convertToMouseEvent(MapMouseEvent event, CorePlotEditor editor)
	{
		Event e = new Event();
		e.button = event.button;
		e.x = event.x;
		e.y = event.y;
		e.display = Display.getCurrent();
		e.widget = editor._viewer.getControl();
		
		MouseEvent me = new MouseEvent(e);
		me.button = event.button;
		me.stateMask = event.modifiers;
		me.display = Display.getCurrent();
		me.widget = editor._viewer.getControl();
		me.x = event.x;
		me.y = event.y;
		return me;
	}

}
