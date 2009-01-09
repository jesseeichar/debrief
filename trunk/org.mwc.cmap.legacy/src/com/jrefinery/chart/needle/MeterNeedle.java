/* ============================================
 * JFreeChart : a free Java chart class library
 * ============================================
 *
 * Project Info:  http://www.object-refinery.com/jfreechart/index.html
 * Project Lead:  David Gilbert (david.gilbert@object-refinery.com);
 *
 * (C) Copyright 2000-2002, by Simba Management Limited and Contributors.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 * ----------------
 * MeterNeedle.java
 * ----------------
 * (C) Copyright 2002, by the Australian Antarctic Division and Contributors.
 *
 * Original Author:  Bryan Scott (for the Australian Antarctic Division);
 * Contributor(s):   David Gilbert (for Simba Management Limited);
 *
 * $Id: MeterNeedle.java,v 1.1.1.1 2003/07/17 10:06:43 Ian.Mayo Exp $
 *
 * Changes:
 * --------
 * 25-Sep-2002 : Version 1, contributed by Bryan Scott (DG);
 *
 */

package com.jrefinery.chart.needle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A needle...
 *
 * @author BS
 */
public abstract class MeterNeedle {

    protected Paint outlinePaint = Color.black;

    protected Stroke outlineStroke = new BasicStroke(2);

    protected Paint fillPaint = null;

    protected Paint highlightPaint = null;

    protected int size = 5;

    /// Scalars to aply to locate the rotation point
    protected double rotateX = 0.5;

    protected double rotateY = 0.5;

    protected static AffineTransform t = new AffineTransform();

    public MeterNeedle() {
        //this(null, null, null);
    }

    public MeterNeedle(Paint outline, Paint fill, Paint highlight) {
        fillPaint = fill;
        highlightPaint = highlight;
        outlinePaint = outline;
    }

    public void draw(Graphics2D g2, Rectangle2D plotArea) {
        draw(g2, plotArea, 0);
    }

    public void draw(Graphics2D g2, Rectangle2D plotArea, double angle) {
        Point2D.Double pt = new Point2D.Double();
        pt.setLocation(plotArea.getMinX() + rotateX * plotArea.getWidth(),
                       plotArea.getMinY() + rotateY * plotArea.getHeight());
        draw(g2, plotArea, pt, angle);
    }

    public void draw(Graphics2D g2, Rectangle2D plotArea, Point2D rotate, double angle) {
        Paint savePaint = g2.getColor();
        Stroke saveStroke = g2.getStroke();

        drawNeedle(g2, plotArea, rotate, Math.toRadians(angle));

        g2.setStroke(saveStroke);
        g2.setPaint(savePaint);
    }

    /**
     * Draws the needle.
     *
     * @param g2  the graphics device.
     * @param plotArea  the plot area.
     * @param rotate  the rotation point.
     * @param angle  the angle.
     */
    protected abstract void drawNeedle(Graphics2D g2,
                                       Rectangle2D plotArea, Point2D rotate, double angle);

    public void setOutlinePaint(Paint p) {
        if (p != null) {
            outlinePaint = p;
        }
    }

    protected void defaultDisplay(Graphics2D g2, Shape shape) {

        if (fillPaint != null) {
            g2.setPaint(fillPaint);
            g2.fill(shape);
        }

        if (outlinePaint != null) {
            g2.setStroke(outlineStroke);
            g2.setPaint(outlinePaint);
            g2.draw(shape);
        }

    }

    public void setOutlineStroke(Stroke s) {
        if (s != null) {
            outlineStroke = s;
        }
    }

    public void setFillPaint(Paint p) {
        if (p != null) {
            fillPaint = p;
        }
    }

    public void setHighlightPaint(Paint p) {
        if (p != null) {
            highlightPaint = p;
        }
    }

    public void setSize(int pixels) {
        size = pixels;
    }

}
