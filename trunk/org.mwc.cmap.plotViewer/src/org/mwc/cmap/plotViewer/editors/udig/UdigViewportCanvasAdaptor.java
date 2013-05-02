/*
 * CanvasAdaptor.java
 *
 * Created on 22 September 2000, 11:49
 */

package org.mwc.cmap.plotViewer.editors.udig;

import java.awt.BasicStroke;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.geom.Arc2D;
import java.awt.image.ImageObserver;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.mwc.cmap.core.ui_support.udig.ControlCanvasType;

import com.lowagie.text.pdf.internal.PolylineShape;
import com.vividsolutions.jts.geom.Coordinate;

import net.refractions.udig.project.render.IViewportModel;
import net.refractions.udig.project.ui.viewers.MapViewer;
import net.refractions.udig.ui.graphics.SWTGraphics;
import net.refractions.udig.ui.graphics.ViewportGraphics;

import MWC.GUI.CanvasType;
import MWC.GenericData.WorldLocation;

/**
 * Wraps the udig viewport model
 * 
 * @author Jesse Eichar
 * @version
 */

public class UdigViewportCanvasAdaptor implements ControlCanvasType
{

	private MapViewer _viewer;
	private ViewportGraphics _dest;

	public UdigViewportCanvasAdaptor(MapViewer viewer)
	{
		_viewer = viewer;
		this._dest = new SWTGraphics(new GC(_viewer.getControl()),
				Display.getCurrent());
	}

	public void addPainter(CanvasType.PaintListener listener)
	{
		// nada
	}

	public void drawLine(int x1, int y1, int x2, int y2)
	{

		_dest.drawLine(x1, y1, x2, y2);
	}

	public boolean drawImage(Image img, int x, int y, int width, int height,
			ImageObserver observer)
	{
		if (_dest == null)
			return false;

		_dest.drawImage(img, 0, 0, img.getWidth(observer), img.getHeight(observer),
				x, y, width, height);

		return true;
	}

	/**
	 * draw a filled polygon
	 * 
	 * @param xPoints
	 *          list of x coordinates
	 * @param yPoints
	 *          list of y coordinates
	 * @param nPoints
	 *          length of list
	 */
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints)
	{

		_dest.fill(new Polygon(xPoints, yPoints, nPoints));
	}

	/**
	 * drawPolyline
	 * 
	 * @param xPoints
	 *          list of x coordinates
	 * @param yPoints
	 *          list of y coordinates
	 * @param nPoints
	 *          length of list
	 */
	public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints)
	{
		_dest.draw(new PolylineShape(xPoints, yPoints, nPoints));
	}

	/**
	 * drawPolygon
	 * 
	 * @param xPoints
	 *          list of x coordinates
	 * @param yPoints
	 *          list of y coordinates
	 * @param nPoints
	 *          length of list
	 */
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints)
	{
		_dest.draw(new Polygon(xPoints, yPoints, nPoints));
	}

	public void drawText(java.awt.Font theFont, String theStr, int x, int y)
	{
		_dest.setFont(theFont);
		_dest.drawString(theStr, x, y, ViewportGraphics.ALIGN_BOTTOM,
				ViewportGraphics.ALIGN_LEFT);
	}

	/**
	 * set/get the background colour
	 */
	public java.awt.Color getBackgroundColor()
	{
		return null;
	}

	/**
	 * expose the graphics object, used only for plotting non-persistent graphics
	 * (temporary lines, etc).
	 */
	public java.awt.Graphics getGraphicsTemp()
	{
		throw new UnsupportedOperationException(
				"Method getGraphicsTemp is not supported by this canvas implementation");
	}

	@SuppressWarnings(
	{ "unchecked", "rawtypes" })
	public java.util.Enumeration getPainters()
	{
		return null;
	}

	public MWC.Algorithms.PlainProjection getProjection()
	{
		return new UDigRendererProjection(_viewer.getMap().getViewportModelInternal());
	}

	public java.awt.Dimension getSize()
	{
		return _viewer.getViewport().getDisplaySize();
	}

	public int getStringHeight(java.awt.Font theFont)
	{
		_dest.setFont(theFont);
		return _dest.getFontHeight();
	}

	public int getStringWidth(java.awt.Font theFont, String theString)
	{
		_dest.setFont(theFont);
		return _dest.stringWidth(theString);
	}

	public void removePainter(CanvasType.PaintListener listener)
	{
		//
	}

	/**
	 * retrieve the full data area, and do a fit to window
	 */
	public void rescale()
	{
		//
	}

	public void setBackgroundColor(java.awt.Color theColor)
	{
		//
	}

	public void setProjection(MWC.Algorithms.PlainProjection val)
	{
		//
	}

	public void setTooltipHandler(CanvasType.TooltipHandler handler)
	{
		//
	}

	public java.awt.Point toScreen(WorldLocation val)
	{
		IViewportModel viewportModel = _viewer.getMap().getViewportModel();
		return viewportModel.worldToPixel(JtsAdapter.toCoord(val));
	}

	public WorldLocation toWorld(java.awt.Point val)
	{
		IViewportModel viewportModel = _viewer.getMap().getViewportModel();
		Coordinate world = viewportModel.pixelToWorld(val.x, val.y);
		return JtsAdapter.toWorldLocation(world);
	}

	public void updateMe()
	{
		//
	}

	public void drawOval(int x, int y, int width, int height)
	{
		//
		_dest.drawOval(x, y, width, height);
	}

	public void fillOval(int x, int y, int width, int height)
	{
		//
		_dest.fillOval(x, y, width, height);
	}

	public void drawText(String str, int x, int y)
	{
		//
		_dest.drawString(str, x, y, ViewportGraphics.ALIGN_BOTTOM, ViewportGraphics.ALIGN_LEFT);
	}

	public void drawRect(int x1, int y1, int wid, int height)
	{
		//
		_dest.drawRect(x1, y1, wid, height);
	}

	public void fillRect(int x, int y, int wid, int height)
	{
		//
		_dest.fillRect(x, y, wid, height);
	}

	/** client has finished drawing operation */
	public void endDraw(Object theVal)
	{
		//
	}

	/** client is about to start drawing operation */
	public void startDraw(Object theVal)
	{
		//
	}

	/**
	 * set the style for the line, using our constants
	 * 
	 */
	public void setLineStyle(int style)
	{
		java.awt.BasicStroke stk = MWC.GUI.Canvas.Swing.SwingCanvas
				.getStrokeFor(style);
		java.awt.Graphics2D g2 = (java.awt.Graphics2D) _dest;
		g2.setStroke(stk);
	}

	/**
	 * set the width of the line, in pixels
	 * 
	 */
	public void setLineWidth(float width)
	{
		java.awt.BasicStroke stk = new BasicStroke(width);
		java.awt.Graphics2D g2 = (java.awt.Graphics2D) _dest;
		g2.setStroke(stk);
	}

	public float getLineWidth()
	{
		java.awt.Graphics2D g2 = (java.awt.Graphics2D) _dest;
		BasicStroke bs = (BasicStroke) g2.getStroke();
		return bs.getLineWidth();
	}

	public void setColor(java.awt.Color theCol)
	{
		//
		_dest.setColor(theCol);
	}

	public void fillArc(int x, int y, int width, int height, int startAngle,
			int arcAngle)
	{
		//
		_dest.fill(new Arc2D.Float(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN));
	}

	public void drawArc(int x, int y, int width, int height, int startAngle,
			int arcAngle)
	{
		//
		_dest.draw(new Arc2D.Float(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN));
	}

	final public void drawPolyline(int[] points)
	{
		// get the convenience function to plot this for us
		UdigViewportCanvasAdaptor.drawPolylineForMe(points, this);
	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////
	// testing for this class
	// ////////////////////////////////////////////////////////////////////////////////////////////////
	static public final class testMe extends junit.framework.TestCase
	{
		static public final String TEST_ALL_TEST_TYPE = "UNIT";

		public testMe(final String val)
		{
			super(val);
		}

		public final void testPolygonMgt()
		{
			int[] points =
			{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
			// convert to normal format
			int len = points.length / 2;
			int[] xP = new int[len];
			int[] yP = new int[len];

			for (int i = 0; i < points.length; i += 2)
			{
				xP[i / 2] = points[i];
				yP[i / 2] = points[i + 1];
			}
			assertEquals("array wrong length", len, 5);
			assertEquals("wrong first x", xP[0], 1);
			assertEquals("wrong first y", yP[0], 2);
			assertEquals("wrong last x", xP[len - 1], 9);
			assertEquals("wrong last y", yP[len - 1], 10);
		}
	}

	/**
	 * convenience method that allows a canvas implementation to support the new
	 * polyline method by just converting the data and calling the old method
	 * 
	 * @param points
	 *          the series of points in the new format
	 * @param canvas
	 *          the canvas implementation that wants to plot it.
	 */
	public static void drawPolylineForMe(final int[] points,
			final CanvasType canvas)
	{
		int len = points.length / 2;
		int[] xP = new int[len];
		int[] yP = new int[len];

		// copy bits in to new arrays
		for (int i = 0; i < points.length; i += 2)
		{
			xP[i / 2] = points[i];
			yP[i / 2] = points[i + 1];
		}

		// do the old-fashioned copy operation
		canvas.drawPolyline(xP, yP, len);

	}

	@Override
	public Control getControl()
	{
		return _viewer.getControl();
	}
}
