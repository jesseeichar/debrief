package org.mwc.cmap.plotViewer.editors.udig;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.mwc.cmap.core.ui_support.udig.ControlCanvasType;
import org.mwc.cmap.plotViewer.actions.Pan;
import org.mwc.cmap.plotViewer.actions.ZoomIn;

import MWC.GUI.Layers;
import MWC.GUI.PlainChart;
import MWC.GenericData.WorldLocation;

public abstract class InteractiveChart extends PlainChart
{

	private static final long serialVersionUID = 5762248330971273946L;

	/**
	 * how far the mouse has to be dragged before it's registered as a drag
	 * operation
	 */
	private final int JITTER = 6;

	/**
	 * track drag operations
	 */
	private transient Point _startPoint = null;
	/**
	 * the last point dragged over
	 */
	private transient Point _draggedPoint = null;

	private transient PlotMouseDragger _myDragMode;

	private transient PlotMouseDragger _myAltDragMode = new Pan.PanMode();

	public InteractiveChart(Layers theLayers)
	{
		super(theLayers);

		// give us an initial zoom mode
		_myDragMode = new ZoomIn.ZoomInMode();
	}

	@Override
	public abstract ControlCanvasType getCanvas();

	public final PlotMouseDragger getDragMode()
	{
		return _myDragMode;
	}

	protected void doMouseDoubleClick(MouseEvent e)
	{

		// was this the right-hand button
		if (e.button == 3)
		{
			getCanvas().rescale();
			getCanvas().updateMe();
		}
		else
		{
			// right, find out which one it was.
			java.awt.Point pt = new java.awt.Point(e.x, e.y);

			// and now the WorldLocation
			WorldLocation loc = getCanvas().getProjection().toWorld(pt);

			// and now see if we are near anything..
			if (_theDblClickListeners.size() > 0)
			{
				// get the top one off the stack
				ChartDoubleClickListener lc = _theDblClickListeners.lastElement();
				lc.cursorDblClicked(this, loc, pt);
			}
		}
	}

	protected void doMouseDown(MouseEvent e)
	{
		// was this the right-hand button?
		if (e.button != 3)
		{
			_startPoint = new Point(e.x, e.y);
			_draggedPoint = null;

			final PlotMouseDragger theMode;
			if (e.button == 2)
				theMode = _myAltDragMode;
			else
				theMode = _myDragMode;

			if (theMode != null)
				theMode.mouseDown(_startPoint, getCanvas(), this);
		}
	}

	public void doMouseMove(MouseEvent e)
	{
		java.awt.Point thisPoint = new java.awt.Point(e.x, e.y);

		super.mouseMoved(thisPoint);

		Point swtPoint = new Point(e.x, e.y);

		final PlotMouseDragger theMode;
		if (e.button == 2)
			theMode = _myAltDragMode;
		else
			theMode = _myDragMode;

		// ok - pass the move event to our drag control (if it's interested...)
		if (theMode != null)
			theMode.doMouseMove(swtPoint, JITTER, super.getLayers(), getCanvas());

		if (_startPoint == null)
			return;

		// was this the right-hand button
		if (e.button != 3)
		{
			_draggedPoint = new Point(e.x, e.y);

			// ok - pass the drag to our drag control
			if (theMode != null)
				theMode.doMouseDrag(_draggedPoint, JITTER, super.getLayers(),
						getCanvas());
		}
	}

	protected void doMouseUp(MouseEvent e)
	{
		// was this the right-hand button
		if (e.button != 3)
		{

			final PlotMouseDragger theMode;
			if (e.button == 2)
				theMode = _myAltDragMode;
			else
				theMode = _myDragMode;

			// ok. did we move at all?
			if (_draggedPoint != null)
			{
				// yes, process the drag
				if (theMode != null)
				{
					theMode.doMouseUp(new Point(e.x, e.y), e.stateMask);

					// and restore the mouse mode cursor
					Cursor normalCursor = theMode.getNormalCursor();
					getCanvas().getControl().setCursor(normalCursor);
				}
			}
			else
			{
				// nope

				// hey, it was just a click - process it
				if (_theLeftClickListener != null)
				{
					// get the world location
					java.awt.Point jPoint = new java.awt.Point(e.x, e.y);
					WorldLocation loc = getCanvas().getProjection().toWorld(jPoint);
					_theLeftClickListener.CursorClicked(jPoint, loc, getCanvas(),
							_theLayers);
				}
			}
		}
		_startPoint = null;
	}

	public final void setDragMode(final PlotMouseDragger newMode)
	{
		_myDragMode = newMode;

		// and reset the start point so we know where we are.
		_startPoint = null;
	}

	/**
	 * embedded interface for classes that are able to handle drag events
	 * 
	 * @author ian.mayo
	 */
	abstract public static class PlotMouseDragger
	{

		protected Cursor _downCursor;
		protected Cursor _normalCursor;

		/**
		 * handle the mouse being dragged
		 * 
		 * @param pt
		 *          the new cursor location
		 * @param theCanvas
		 */
		abstract public void doMouseDrag(final org.eclipse.swt.graphics.Point pt,
				final int JITTER, final Layers theLayers, ControlCanvasType theCanvas);

		/**
		 * handle the mouse moving across the screen
		 * 
		 * @param pt
		 *          the new cursor location
		 * @param theCanvas
		 */
		public void doMouseMove(final org.eclipse.swt.graphics.Point pt,
				final int JITTER, final Layers theLayers, ControlCanvasType theCanvas)
		{
			// provide a dummy implementation - most of our modes don't use this...
		}

		/**
		 * handle the mouse drag finishing
		 * 
		 * @param keyState
		 * @param pt
		 *          the final cursor location
		 */
		abstract public void doMouseUp(org.eclipse.swt.graphics.Point point,
				int keyState);

		/**
		 * ok, assign the cursor for when we're just hovering
		 * 
		 * @return the new cursor to use, silly.
		 */
		public Cursor getNormalCursor()
		{
			// ok, return the 'normal' cursor
			if ((_normalCursor == null) || (_normalCursor.isDisposed()))
				_normalCursor = new Cursor(Display.getCurrent(), SWT.CURSOR_ARROW);

			return _normalCursor;
		}

		/**
		 * handle the mouse drag starting
		 * 
		 * @param canvas
		 *          the control it's dragging over
		 * @param theChart
		 * @param pt
		 *          the first cursor location
		 */
		abstract public void mouseDown(org.eclipse.swt.graphics.Point point,
				ControlCanvasType canvas, PlainChart theChart);

		public void close()
		{
			// ditch our objects
			if (_downCursor != null)
			{
				_downCursor.dispose();
				_downCursor = null;
			}
			if (_normalCursor != null)
			{
				_normalCursor.dispose();
				_normalCursor = null;
			}
		}

	}

}