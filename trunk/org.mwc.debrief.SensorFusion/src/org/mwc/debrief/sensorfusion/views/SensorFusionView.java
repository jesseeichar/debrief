package org.mwc.debrief.sensorfusion.views;

import java.awt.event.InputEvent;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.core.DataTypes.TrackData.TrackManager;
import org.mwc.cmap.core.preferences.SelectionHelper;
import org.mwc.cmap.core.property_support.EditableWrapper;
import org.mwc.cmap.core.ui_support.PartMonitor;
import org.mwc.debrief.sensorfusion.Activator;
import org.mwc.debrief.sensorfusion.views.DataSupport.SensorSeries;
import org.mwc.debrief.sensorfusion.views.DataSupport.TacticalSeries;
import org.mwc.debrief.sensorfusion.views.FusionPlotRenderer.FusionHelper;

import Debrief.Wrappers.SensorWrapper;
import Debrief.Wrappers.TrackWrapper;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GUI.Layers.DataListener;
import MWC.GenericData.WatchableList;

public class SensorFusionView extends ViewPart implements ISelectionProvider,
		FusionHelper
{

	private static final String CHART_NAME = "Bearing data";

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.mwc.debrief.SensorFusion";

	/**
	 * helper application to help track creation/activation of new plots
	 */
	private PartMonitor _myPartMonitor = null;

	protected TrackManager _trackData;

	private ChartComposite _myChartFrame;

	private Action _useOriginalColors;

	final private XYLineAndShapeRenderer _plotRenderer;

	final private Vector<SensorSeries> _selectedTracks;

	private Layers _currentLayers;

	/**
	 * helper - handle the selection a little better
	 */
	private SelectionHelper _selectionHelper;

	/**
	 * listen out for new data being added or removed
	 * 
	 */
	protected DataListener _layerListener;

	/**
	 * The constructor.
	 */
	public SensorFusionView()
	{
		_selectedTracks = new Vector<SensorSeries>(0, 1);
		_plotRenderer = new FusionPlotRenderer(this);

	}

	protected void setupListeners()
	{
		_myPartMonitor = new PartMonitor(getSite().getWorkbenchWindow()
				.getPartService());
		_myPartMonitor.addPartListener(TrackManager.class, PartMonitor.ACTIVATED,
				new PartMonitor.ICallback()
				{
					public void eventTriggered(String type, Object part,
							IWorkbenchPart parentPart)
					{
						TrackManager provider = (TrackManager) part;

						// is this different to our current one?
						if (provider != _trackData)
							storeDetails(provider, parentPart);
					}
				});

		_myPartMonitor.addPartListener(TrackManager.class, PartMonitor.CLOSED,
				new PartMonitor.ICallback()
				{
					public void eventTriggered(String type, Object part,
							IWorkbenchPart parentPart)
					{
						if (part == _trackData)
						{
							_trackData = null;
							resetPlot();
						}
					}
				});
		_myPartMonitor.addPartListener(Layers.class, PartMonitor.ACTIVATED,
				new PartMonitor.ICallback()
				{
					public void eventTriggered(String type, Object part,
							IWorkbenchPart parentPart)
					{

						// is this different to our current one?
						if (part != _currentLayers)
						{
							if (_layerListener == null)
								_layerListener = new DataListener()
								{

									@Override
									public void dataExtended(Layers theData)
									{
										// redo the data
										recalculateData();
									}

									@Override
									public void dataModified(Layers theData, Layer changedLayer)
									{
									}

									@Override
									public void dataReformatted(Layers theData, Layer changedLayer)
									{
										// redo the presentation
										redrawPlot();
									}
								};

							_currentLayers = (Layers) part;
							_currentLayers.addDataModifiedListener(_layerListener);
							_currentLayers.addDataReformattedListener(_layerListener);
						}
					}
				});

		_myPartMonitor.addPartListener(Layers.class, PartMonitor.CLOSED,
				new PartMonitor.ICallback()
				{
					public void eventTriggered(String type, Object part,
							IWorkbenchPart parentPart)
					{
						if (part == _currentLayers)
						{
							_currentLayers.removeDataModifiedListener(_layerListener);
							_currentLayers.removeDataReformattedListener(_layerListener);
							_currentLayers = null;
						}
					}
				});

	}

	protected void storeDetails(TrackManager provider, IWorkbenchPart parentPart)
	{
		// ok, we've got a new plot to watch. better watch it...
		_trackData = provider;

		// clear our list
		_selectedTracks.removeAllElements();

		recalculateData();

	}

	private void recalculateData()
	{

		// which is the primary?
		WatchableList primary = _trackData.getPrimaryTrack();

		if (primary == null)
			_myChartFrame.getChart().setTitle("Primary track missing");
		else
			_myChartFrame.getChart().setTitle(CHART_NAME);

		// check it's a track
		if (!(primary instanceof TrackWrapper))
		{
			CorePlugin.logError(Status.WARNING,
					"Primary track not suitable for watching", null);
		}
		else
		{
			TrackWrapper _primary = (TrackWrapper) primary;
			// and which are the secondaries?
			WatchableList[] secondaries = _trackData.getSecondaryTracks();

			// sort out the bearing tracks
			TimeSeriesCollection newData = new TimeSeriesCollection();
			DataSupport.tracksFor(_primary, secondaries, newData);

			DataSupport.sensorDataFor(_primary, newData);

			// and now the sensor data
			_myChartFrame.getChart().getXYPlot().setDataset(newData);
		}

	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent)
	{
		makeActions();
		contributeToActionBars();
		setupListeners();

		// and the selection provider bits
		_selectionHelper = new SelectionHelper();
		getSite().setSelectionProvider(_selectionHelper);

		parent.setLayout(new FillLayout());

		// ok, let's mockup the UI
		JFreeChart myChart = DataSupport.createChart(null);
		myChart.setTitle(CHART_NAME);
		myChart.getXYPlot().setRenderer(_plotRenderer);

		// and the chart frame
		_myChartFrame = new ChartComposite(parent, SWT.NONE, myChart, true);
		_myChartFrame.setDisplayToolTips(false);
		_myChartFrame.setHorizontalAxisTrace(false);
		_myChartFrame.setVerticalAxisTrace(false);

		_myChartFrame.addChartMouseListener(new ChartMouseListener()
		{

			public void chartMouseClicked(ChartMouseEvent event)
			{
				ChartEntity entity = event.getEntity();
				if (entity instanceof XYItemEntity)
				{
					XYItemEntity xyi = (XYItemEntity) entity;
					TimeSeriesCollection coll = (TimeSeriesCollection) xyi.getDataset();
					TacticalSeries ts = (TacticalSeries) coll
							.getSeries(((XYItemEntity) entity).getSeriesIndex());
					if (ts instanceof SensorSeries)
					{
						SensorSeries ss = (SensorSeries) ts;

						// right, is ctrl-key pressed
						int mods = event.getTrigger().getModifiers();
						if ((mods & InputEvent.CTRL_MASK) == 0)
						{
							_selectedTracks.removeAllElements();
							_selectedTracks.add(ss);
						}
						else
						{
							if (_selectedTracks.contains(ts))
								_selectedTracks.remove(ts);
							else
								_selectedTracks.add(ss);

						}

						// and update the UI
						updatedSelection();

						// ok, we need to redraw
						redrawPlot();
					}
				}
				else
				{
					_selectedTracks.removeAllElements();

					// and update the UI
					updatedSelection();
					// ok, we need to redraw
					redrawPlot();
				}
			}

			@Override
			public void chartMouseMoved(ChartMouseEvent event)
			{
			}
		});

	}

	protected void updatedSelection()
	{
		Vector<EditableWrapper> wrappers = new Vector<EditableWrapper>(0, 1);
		Iterator<SensorSeries> it = _selectedTracks.iterator();
		while (it.hasNext())
		{
			SensorSeries ss = (SensorSeries) it.next();
			SensorWrapper sw = ss.getSensor();
			EditableWrapper ed = new EditableWrapper(sw);
			wrappers.add(ed);
		}

		if (wrappers.size() > 0)
		{
			// and provide the selection object
			StructuredSelection trackSelection = new StructuredSelection(wrappers);
			setSelection(trackSelection);
		}
	}

	private void contributeToActionBars()
	{
		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(_useOriginalColors);
	}

	private void makeActions()
	{
		_useOriginalColors = new Action("Original colors", SWT.TOGGLE)
		{
			@Override
			public void run()
			{
				// we don't need to do any fancy processing. If we trigger redraw,
				// it will pick up the new value
				redrawPlot();
			}
		};
		_useOriginalColors.setImageDescriptor(Activator
				.getImageDescriptor("icons/ColorPalette.png"));
	}

	protected void redrawPlot()
	{
		if (_plotRenderer != null)
		{
			_plotRenderer.setSeriesShapesVisible(0, true);
		}
	}

	protected void resetPlot()
	{
		_myChartFrame.getChart().getXYPlot().setDataset(null);
		_myChartFrame.getChart().setTitle("Pending");
	}

	// protected void resetData()
	// {
	//
	// final long _start = _primary.getStartDTG().getDate().getTime();
	// final long _end = _primary.getEndDTG().getDate().getTime();
	//
	// int ctr = 0;
	//
	// int MAX_SENSORS = 110;
	// for (int i = 0; i < MAX_SENSORS; i++)
	// {
	// final long _step = DataSupport.stepInterval();
	// final long _thisStart = _start + DataSupport.delay();
	// long _thisEnd = _thisStart + DataSupport.duration();
	// _thisEnd = Math.min(_thisEnd, _end);
	// long _this = _thisStart;
	//
	// SensorWrapper sw = new SensorWrapper("sensor 3:" + i);
	// sw.setColor(null);
	// double theVal = Math.random() * 360;
	// while (_this < _thisEnd)
	// {
	// theVal = theVal - 1 + (Math.random() * 2);
	// SensorContactWrapper scw = new SensorContactWrapper(_primary.getName(),
	// new HiResDate(_this), null, theVal, null, null, null, null,
	// "some label:", 0, sw.getName());
	// sw.add(scw);
	// ctr++;
	// _this += _step;
	// }
	//
	// _primary.add(sw);
	//
	// }
	// System.out.println("created " + ctr + " cuts");
	// }

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus()
	{
	}

	public void addSelectionChangedListener(ISelectionChangedListener listener)
	{
		_selectionHelper.addSelectionChangedListener(listener);
	}

	@Override
	public ISelection getSelection()
	{
		return _selectionHelper.getSelection();
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener)
	{
		_selectionHelper.removeSelectionChangedListener(listener);
	}

	@Override
	public void setSelection(ISelection selection)
	{
		_selectionHelper.fireNewSelection(selection);
	}

	@Override
	public Vector<SensorSeries> getSelectedItems()
	{
		return _selectedTracks;
	}

	@Override
	public boolean useOriginalColors()
	{
		return _useOriginalColors.isChecked();
	}
}