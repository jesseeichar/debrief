package com.borlander.rac525791.draw2d.ext;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.Transform;
import org.eclipse.swt.graphics.Color;

/**
 * A rotatable, polygon shaped decoration most commonly used for decorating the
 * ends of
 * {@link com.borlander.rac525791.draw2d.ext.Polyline polylines}.
 */
public class PolygonDecoration extends Polygon implements RotatableDecorationExt {

	/**
	 * Template for a triangle that points to the right when the rotation angle
	 * is 0
	 */
	public static final PointList TRIANGLE_TIP = new PointList();

	/**
	 * Template for a triangle that points to the left when the rotation angle
	 * is 0
	 */
	public static final PointList INVERTED_TRIANGLE_TIP = new PointList();

	static {
		TRIANGLE_TIP.addPoint(0, 0);
		TRIANGLE_TIP.addPoint(-1, 1);
		TRIANGLE_TIP.addPoint(-1, -1);

		INVERTED_TRIANGLE_TIP.addPoint(0, 1);
		INVERTED_TRIANGLE_TIP.addPoint(0, -1);
		INVERTED_TRIANGLE_TIP.addPoint(-1, 0);
	}

	private Point location = new Point();

	private PointList template = TRIANGLE_TIP;

	private Transform transform = new Transform();

	/**
	 * Constructs a PolygonDecoration. Defaults the PolygonDecoration to fill
	 * its region with black.
	 * 
	 * @since 2.0
	 */
	public PolygonDecoration() {
		setFill(true);
		setScale(1, 1);
	}

	/**
	 * @see org.eclipse.draw2d.IFigure#getBackgroundColor()
	 */
	public Color getLocalBackgroundColor() {
		if (super.getLocalBackgroundColor() == null)
			return getForegroundColor();
		return super.getLocalBackgroundColor();
	}

	/**
	 * Returns the points in the PolygonDecoration as a PointList.
	 * 
	 * @return the points in this PolygonDecoration
	 * @since 2.0
	 */
	public PointList getPoints() {
		if (points == null) {
			points = new PointList();
			for (int i = 0; i < template.size(); i++)
				points.addPoint(transform.getTransformed(template.getPoint(i)));
		}
		return points;
	}

	/**
	 * Sets the location of this PolygonDecoration.
	 * 
	 * @param p
	 *            the new location
	 */
	public void setLocation(Point p) {
		points = null;
		bounds = null;
		location.setLocation(p);
		transform.setTranslation(p.x, p.y);
	}

	/**
	 * Sets the PolygonDecorations point template to the passed PointList. This
	 * template is an outline of the PolygonDecoration's region. (The default
	 * value is TRIANGLE_TIP which is a triangle whose tip is pointing to the
	 * right).
	 * 
	 * @param pl
	 *            the PointList outline to use as the PolygonDecoration's region
	 * @since 2.0
	 */
	public void setTemplate(PointList pl) {
		erase();
		template = pl;
		points = null;
		bounds = null;
		repaint();
	}

	/**
	 * Sets the amount of scaling to be done along X and Y axes on the
	 * PolygonDecoration's template.
	 * 
	 * @param x
	 *            X scaling
	 * @param y
	 *            Y scaling
	 * @since 2.0
	 */
	public void setScale(double x, double y) {
		points = null;
		bounds = null;
		transform.setScale(x, y);
	}

	/**
	 * Sets the rotation of this decoration so that the decoration points toward
	 * the given reference point.
	 * 
	 * @param ref
	 *            the reference point
	 */
	public void setReferencePoint(Point ref) {
		Point pt = Point.SINGLETON;
		pt.setLocation(ref);
		pt.negate().translate(location);
		setRotation(Math.atan2(pt.y, pt.x));
	}

	/**
	 * Sets the angle by which rotation is to be done on the PolygonDecoration.
	 * 
	 * @param angle
	 *            Angle of rotation
	 * @since 2.0
	 */
	public void setRotation(double angle) {
		points = null;
		bounds = null;
		transform.setRotation(angle);
	}

	public Point transformPoint(Point pt) {
		return transform.getTransformed(pt);
	}
	
	public Point getTemplatePoint(int index, Point output){
		return template.getPoint(output, index);
	}
	
	public PointList getTemplateCopy(){
		return template.getCopy();
	}
	
}
