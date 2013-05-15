package org.mwc.cmap.plotViewer.editors.chart;

import java.awt.Dimension;
import java.util.Enumeration;
import java.util.Vector;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.mwc.cmap.core.ui_support.swt.SWTCanvasAdapter;

import MWC.Algorithms.PlainProjection;
import MWC.GUI.CanvasType;

public class PaintUpdateCanvas extends SWTCanvasAdapter
{
	private static final long serialVersionUID = 1L;
	private final Rectangle _drawBounds;
	private GC _gc;
	private SWTCanvas _parent;

	public PaintUpdateCanvas(SWTCanvas parent, PlainProjection projection, GC gc,
			Vector<PaintListener> painters, Rectangle drawBounds)
	{
		super(projection);
		this._parent = parent;
		this._gc = gc;
		this._drawBounds = drawBounds;
		this._thePainters = painters;
		startDraw(gc);
		gc.setClipping(drawBounds);
	}

	public void execute()
	{
		// and paint into it
		paintPlot(this);
	}

	public void paintPlot(CanvasType dest)
	{
		// go through our painters
		final Enumeration<PaintListener> enumer = _thePainters.elements();
		while (enumer.hasMoreElements())
		{
			final CanvasType.PaintListener thisPainter = enumer.nextElement();

			// check the screen has been defined
			final Dimension area = this.getProjection().getScreenArea();
			if ((area == null) || (area.getWidth() <= 0) || (area.getHeight() <= 0))
			{
				return;
			}

			// it must be ok
			thisPainter.paintMe(dest);
		}
	}

	@Override
	public void flush()
	{
		_parent.flush();
	}
	@Override
	public void updateMe()
	{
		_parent.flush();
	}

	@Override
	public void close()
	{
		if (_gc != null)
		{
			final Enumeration<PaintListener> enumer = _thePainters.elements();
			while (enumer.hasMoreElements())
			{
				PaintListener thisPainter = enumer.nextElement();
				thisPainter.cancel();
			}
			endDraw(_gc);
			_gc.dispose();
			_gc = null;
			super.close();
		}
	}
}
