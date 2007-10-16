/**
 * 
 */
package org.mwc.cmap.plotViewer.actions;

import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.plotViewer.editors.chart.*;
import org.mwc.cmap.plotViewer.editors.chart.SWTChart.PlotMouseDragger;

import MWC.Algorithms.Conversions;
import MWC.GUI.*;
import MWC.GenericData.*;

/**
 * @author ian.mayo
 */
final public class RangeBearing extends CoreDragAction
{

	/**
	 * embedded class that handles the range/bearing measurement
	 * 
	 * @author Ian
	 */
	final public static class RangeBearingMode extends SWTChart.PlotMouseDragger
	{
		/**
		 * the start point, in world coordinates (so we don't have to calculate it
		 * as often)
		 */
		WorldLocation _startLocation;

		/**
		 * the start point, in screen coordinates - where we started our drag
		 */
		Point _startPoint;

		/**
		 * the last rectangle drawn, so we can erase it on the next update
		 */
		Rectangle _lastRect;

		/**
		 * the canvas we're updating..
		 */
		SWTCanvas _myCanvas;

		final public void doMouseDrag(Point pt, int JITTER, Layers theLayers, SWTCanvas theCanvas)
		{
			if (_startPoint != null)
			{
				GC gc = new GC(_myCanvas.getCanvas());

				// This is the same as a !XOR
				gc.setXORMode(true);
				gc.setForeground(gc.getBackground());

				// Erase existing rectangle
				if (_lastRect != null)
					plotUpdate(gc);

				int dx = pt.x - _startPoint.x;
				int dy = pt.y - _startPoint.y;

				// Draw selection rectangle
				_lastRect = new Rectangle(_startPoint.x, _startPoint.y, dx, dy);
				plotUpdate(gc);
				
				// and ditch the GC
				gc.dispose();

			}
			else
			{
				// System.out.println("no point.");
			}

		}

		final public void doMouseUp(Point point, int keyState)
		{
			GC gc = new GC(_myCanvas.getCanvas());

			// This is the same as a !XOR
			gc.setXORMode(true);
			gc.setForeground(gc.getBackground());

			// Erase existing rectangle
			if (_lastRect != null)
			{
				// hmm, we've finished plotting. see if the ctrl button is
				// down
				if ((keyState & SWT.CTRL) == 0)
					plotUpdate(gc);
			}

			_startPoint = null;
			_lastRect = null;
			_myCanvas = null;
			_startLocation = null;
		}

		final public void mouseDown(Point point, SWTCanvas canvas, PlainChart theChart)
		{
			_startPoint = point;
			_myCanvas = canvas;
			_startLocation = new WorldLocation(_myCanvas.getProjection().toWorld(
					new java.awt.Point(point.x, point.y)));
		}

		final private void plotUpdate(GC dest)
		{

			java.awt.Point endPoint = new java.awt.Point(_lastRect.x + _lastRect.width,
					_lastRect.y + _lastRect.height);

			dest.setForeground(new Color(Display.getDefault(), 111, 111, 111));
			dest.setLineWidth(2);
			dest.drawLine(_lastRect.x, _lastRect.y, _lastRect.x + _lastRect.width, _lastRect.y
					+ _lastRect.height);

			// also put in a text-label
			WorldLocation endLocation = _myCanvas.getProjection().toWorld(endPoint);
			WorldVector sep = endLocation.subtract(_startLocation);

			String myUnits = CorePlugin.getToolParent().getProperty(
					MWC.GUI.Properties.UnitsPropertyEditor.UNITS_PROPERTY);

			double rng = Conversions.convertRange(sep.getRange(), myUnits);
			double brg = sep.getBearing();
			brg = brg * 180 / Math.PI;
			if(brg < 0)
				 brg += 360;
			DecimalFormat df = new DecimalFormat("0.00");
			String numComponent = df.format(rng);
			String txt = "[" + numComponent + myUnits + " " + (int) brg + "\u00b0]";

			// decide the mid-point
			java.awt.Point loc = new java.awt.Point(_lastRect.x + _lastRect.width / 2,
					_lastRect.y + _lastRect.height / 2);

			// find out how big the text is
			FontMetrics fm = dest.getFontMetrics();

			loc.translate(0, fm.getHeight() / 2);
			loc.translate(-txt.length() / 2 * fm.getAverageCharWidth(), 0);

			dest.setForeground(new Color(Display.getDefault(), 200, 200, 200));
			// dest.setForeground(dest.getBackground());

			dest.drawText(txt, loc.x, loc.y, SWT.DRAW_TRANSPARENT);

		}
	}

	public PlotMouseDragger getDragMode()
	{
		return new RangeBearingMode();
	}
}