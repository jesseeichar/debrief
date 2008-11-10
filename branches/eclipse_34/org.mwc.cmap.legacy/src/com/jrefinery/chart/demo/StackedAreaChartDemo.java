/* ===============
 * JFreeChart Demo
 * ===============
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
 * -------------------------
 * StackedAreaChartDemo.java
 * -------------------------
 * (C) Copyright 2002, by Simba Management Limited and Contributors.
 *
 * Original Author:  David Gilbert (for Simba Management Limited);
 * Contributor(s):   -;
 *
 * $Id: StackedAreaChartDemo.java,v 1.1.1.1 2003/07/17 10:06:36 Ian.Mayo Exp $
 *
 * Changes
 * -------
 * 23-Sep-2002 : Version 1 (DG);
 * 11-Oct-2002 : Fixed errors reported by Checkstyle (DG);
 *
 */

package com.jrefinery.chart.demo;

import java.awt.Paint;
import java.awt.Color;
import com.jrefinery.chart.JFreeChart;
import com.jrefinery.chart.ChartFactory;
import com.jrefinery.chart.ChartPanel;
import com.jrefinery.chart.CategoryPlot;
import com.jrefinery.chart.HorizontalCategoryAxis;
import com.jrefinery.chart.NumberAxis;
import com.jrefinery.data.DefaultCategoryDataset;
import com.jrefinery.ui.ApplicationFrame;
import com.jrefinery.ui.RefineryUtilities;

/**
 * A simple demonstration application showing how to create a stacked area chart using data from a
 * CategoryDataset.
 *
 * @author DG
 */
public class StackedAreaChartDemo extends ApplicationFrame {

    /**
     * Creates a new demo.
     *
     * @param title  the frame title.
     */
    public StackedAreaChartDemo(String title) {

        super(title);

        // create a dataset...
        double[][] data = new double[][] {
            { 1.0, 4.0, 3.0, 5.0, 5.0, 7.0, 7.0, 8.0 },
            { 5.0, 7.0, 6.0, 8.0, 4.0, 4.0, 2.0, 1.0 },
            { 4.0, 3.0, 2.0, 3.0, 6.0, 3.0, 4.0, 3.0 }
        };

        DefaultCategoryDataset dataset = new DefaultCategoryDataset(data);

        // set the series names...
        String[] seriesNames = new String[] { "First", "Second", "Third" };
        dataset.setSeriesNames(seriesNames);

        // set the category names...
        String[] categories = new String[] { "Type 1", "Type 2", "Type 3", "Type 4",
                                             "Type 5", "Type 6", "Type 7", "Type 8"  };
        dataset.setCategories(categories);

        // create the chart...
        JFreeChart chart = ChartFactory.createStackedAreaChart("Stacked Area Chart", // chart title
                                                               "Category",    // domain axis label
                                                               "Value",       // range axis label
                                                                dataset,      // data
                                                                true          // include legend
                                                        );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

        // set the background color for the chart...
        chart.setBackgroundPaint(Color.yellow);

        // get a reference to the plot for further customisation...
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setForegroundAlpha(0.5f);
        plot.setIntroGapPercent(0.0);
        plot.setTrailGapPercent(0.0);
        // label data points with values...
        plot.setLabelsVisible(true);

        // set the color for each series...
        plot.setSeriesPaint(new Paint[] { Color.green, Color.orange, Color.red });


        // change the auto tick unit selection to integer units only...
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        HorizontalCategoryAxis domainAxis = (HorizontalCategoryAxis) plot.getDomainAxis();
        domainAxis.setVerticalCategoryLabels(true);
        // OPTIONAL CUSTOMISATION COMPLETED.

        // add the chart to a panel...
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));

        setContentPane(chartPanel);

    }

    /**
     * Starting point for the demonstration application.
     *
     * @param args  ignored.
     */
    public static void main(String[] args) {

        StackedAreaChartDemo demo = new StackedAreaChartDemo("Stacked Area Chart Demo");
        demo.pack();
        RefineryUtilities.centerFrameOnScreen(demo);
        demo.setVisible(true);

    }

}
