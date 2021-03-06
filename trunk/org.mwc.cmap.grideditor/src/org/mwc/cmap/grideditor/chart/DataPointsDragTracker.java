package org.mwc.cmap.grideditor.chart;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.eclipse.swt.graphics.Rectangle;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

public abstract class DataPointsDragTracker implements
		ChartMouseListenerExtension
{

	private final JFreeChartComposite myChartPanel;

	private final DragSubject myDragSubject;

	private final boolean myAllowVerticalMovesOnly;

	protected abstract void dragCompleted(BackedChartItem item, double finalX,
			double finalY);

	public DataPointsDragTracker(JFreeChartComposite chartPanel,
			boolean allowYOnly)
	{
		myChartPanel = chartPanel;
		myAllowVerticalMovesOnly = allowYOnly;
		myDragSubject = new DragSubject();
	}

	public void chartMouseClicked(ChartMouseEvent event)
	{
		myDragSubject.setSubject(event.getEntity());
		myChartPanel.redrawCanvas();
	}

	public void chartMouseMoved(ChartMouseEvent event)
	{
		if (!myDragSubject.isEmpty())
		{
			myChartPanel.forgetZoomPoints();

			// Rectangle clientArea = myChartPanel.getClientArea();
			// int screenX = event.getTrigger().getX() - clientArea.x;
			// int screenY = event.getTrigger().getY() - clientArea.y;

			// [IM] don't bother with sorting out the client area offset
			// - we've stopped using it in the FixedChartComposite calling method
			int screenX = event.getTrigger().getX();
			int screenY = event.getTrigger().getY();

			// deliberately switch axes for following line, now that we've switched
			// the axes to put time
			// down the LH side.
			Point2D point2d = new Point2D.Double(screenY, screenX);
			XYPlot xyplot = myChartPanel.getChart().getXYPlot();
			ChartRenderingInfo renderingInfo = myChartPanel.getChartRenderingInfo();
			Rectangle2D dataArea = renderingInfo.getPlotInfo().getDataArea();

			// WORKAROUND: when the grid graph gets really wide, the labels on the
			// y-axis get stretched.
			// but, the dataArea value doesn't reflect this.
			// So, get the width values from the getScreenDataArea method - which
			// does reflect the scaling applied to the y axis.
			// - and all works well now.
			Rectangle dataArea2 = myChartPanel.getScreenDataArea();
			dataArea = new Rectangle2D.Double(dataArea2.x, dataArea.getY(),
					dataArea2.width, dataArea.getHeight());

			ValueAxis domainAxis = xyplot.getDomainAxis();
			RectangleEdge domainEdge = xyplot.getDomainAxisEdge();
			ValueAxis valueAxis = xyplot.getRangeAxis();
			RectangleEdge valueEdge = xyplot.getRangeAxisEdge();
			double domainX = domainAxis.java2DToValue(point2d.getX(), dataArea,
					domainEdge);
			double domainY = valueAxis.java2DToValue(point2d.getY(), dataArea,
					valueEdge);

			if (myAllowVerticalMovesOnly)
			{
				domainX = myDragSubject.getDraggedItem().getXValue();
			}

			if (!myDragSubject.isEmpty())
				myDragSubject.setProposedValues(domainX, domainY);
			myChartPanel.redrawCanvas();
		}
	}

	public void chartMouseReleased(ChartMouseEvent event)
	{
		if (!myDragSubject.isEmpty() && myDragSubject.getLastDomainPoint() != null)
		{
			try
			{
				Point2D finalPoint = myDragSubject.getLastDomainPoint();
				dragCompleted(myDragSubject.getDraggedItem(), finalPoint.getX(),
						finalPoint.getY());
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (!myDragSubject.isEmpty())
		{
			myDragSubject.setSubject(null);
			myChartPanel.redrawCanvas();
		}
	}

	private RendererWithDynamicFeedback getFeedbackRenderer()
	{
		if (myChartPanel.getChart() == null)
		{
			return null;
		}
		XYItemRenderer renderer = myChartPanel.getChart().getXYPlot()
				.getRenderer(0);
		return renderer instanceof RendererWithDynamicFeedback ? (RendererWithDynamicFeedback) renderer
				: null;
	}

	private class DragSubject
	{

		private BackedChartItem myDraggedItem;

		private XYItemEntity myDraggedEntity;

		private Point2D.Double myLastDomainPoint;

		public void setSubject(ChartEntity chartEntity)
		{
			clear();
			if (chartEntity instanceof XYItemEntity)
			{
				myDraggedEntity = (XYItemEntity) chartEntity;
				myDraggedItem = extractBackedChartItem(myDraggedEntity);
				if (myDraggedItem == null)
				{
					clear();
				}
			}

			RendererWithDynamicFeedback renderer = getFeedbackRenderer();
			if (renderer != null)
			{
				renderer.setFeedbackSubject(myDraggedEntity);
				renderer.setFeedBackValue(null);
			}
		}

		public void setProposedValues(double x, double y)
		{
			if (isEmpty())
			{
				return;
			}

			RendererWithDynamicFeedback renderer = getFeedbackRenderer();
			if (renderer == null)
			{
				return;
			}
			if (myLastDomainPoint == null)
			{
				myLastDomainPoint = new Point2D.Double();
			}
			myLastDomainPoint.setLocation(x, y);
			renderer.setFeedBackValue((Point2D.Double) myLastDomainPoint.clone());
		}

		public BackedChartItem getDraggedItem()
		{
			return myDraggedItem;
		}

		@SuppressWarnings("unused")
		public XYItemEntity getDraggedEntity()
		{
			return myDraggedEntity;
		}

		public boolean isEmpty()
		{
			return myDraggedEntity == null;
		}

		private void clear()
		{
			myDraggedEntity = null;
			myDraggedItem = null;
			myLastDomainPoint = null;
		}

		public Point2D.Double getLastDomainPoint()
		{
			return myLastDomainPoint;
		}

		@SuppressWarnings("rawtypes")
		private BackedChartItem extractBackedChartItem(XYItemEntity xyEntity)
		{
			Comparable seriesKey = xyEntity.getDataset().getSeriesKey(
					xyEntity.getSeriesIndex());
			if (xyEntity.getDataset() instanceof XYSeriesCollection)
			{
				XYSeries series = ((XYSeriesCollection) xyEntity.getDataset())
						.getSeries(seriesKey);
				XYDataItem dataItem = series.getDataItem(xyEntity.getItem());
				if (dataItem instanceof BackedChartItem)
				{
					return (BackedChartItem) dataItem;
				}
			}
			else if (xyEntity.getDataset() instanceof TimeSeriesCollection)
			{
				TimeSeries series = ((TimeSeriesCollection) xyEntity.getDataset())
						.getSeries(seriesKey);
				TimeSeriesDataItem dataItem = series.getDataItem(xyEntity.getItem());
				if (dataItem instanceof BackedChartItem)
				{
					return (BackedChartItem) dataItem;
				}
			}
			return null;
		}

	}

}
