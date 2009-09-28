package org.mwc.asset.scenarioplotter.editors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.SubActionBars2;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.mwc.asset.core.ASSETPlugin;
import org.mwc.asset.scenariocontroller2.views.ScenarioControllerView;
import org.mwc.cmap.core.interfaces.IControllableViewport;
import org.mwc.cmap.core.ui_support.PartMonitor;
import org.mwc.cmap.plotViewer.PlotViewerPlugin;
import org.mwc.cmap.plotViewer.actions.ExportWMF;
import org.mwc.cmap.plotViewer.actions.IChartBasedEditor;
import org.mwc.cmap.plotViewer.editors.chart.SWTChart;
import org.mwc.cmap.plotViewer.editors.chart.SWTChart.PlotMouseDragger;

import ASSET.ScenarioType;
import ASSET.Scenario.ScenarioSteppedListener;
import MWC.Algorithms.PlainProjection;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GUI.Plottable;
import MWC.GUI.Layers.DataListener;
import MWC.GenericData.WorldArea;

public class ASSETPlotEditor extends EditorPart implements
		IControllableViewport, IChartBasedEditor
{

	// //////////////////////////////
	// member data
	// //////////////////////////////

	/**
	 * the chart we store/manager
	 */
	protected SWTChart _myChart = null;

	/**
	 * the graphic data we know about
	 */
	protected Layers _myLayers = null;

	/**
	 * the scenario we're listening to
	 * 
	 */
	protected ScenarioType _myScenario = null;

	protected DataListener _listenForMods;

	private PartMonitor _myPartMonitor;

	private ScenarioSteppedListener _stepListener;

	// //////////////////////////////
	// constructor
	// //////////////////////////////

	public ASSETPlotEditor()
	{
		super();
	}

	private void startListeningTo(Layers layers)
	{
		if (_listenForMods == null)
		{
			_listenForMods = new DataListener()
			{
				public void dataModified(Layers theData, Layer changedLayer)
				{
					fireDirty();
				}

				public void dataExtended(Layers theData)
				{
					fireDirty();
				}

				public void dataReformatted(Layers theData, Layer changedLayer)
				{
					fireDirty();
				}
			};
		}

		layers.addDataExtendedListener(_listenForMods);
		layers.addDataModifiedListener(_listenForMods);
		layers.addDataReformattedListener(_listenForMods);

		// make sure we can see the data
		_myChart.rescale();
		
		// hey, push ourselves out a little, so we keep it in sight.
		_myChart.getCanvas().getProjection().zoom(2.0);
		
		// and ask for a refresh
		fireDirty();
	}

	protected void fireDirty()
	{
		update();
	}

	private void stopListeningToThis(Layers layers)
	{
		layers.removeDataExtendedListener(_listenForMods);
		layers.removeDataModifiedListener(_listenForMods);
		layers.removeDataReformattedListener(_listenForMods);
	}

	private void startListeningTo(ScenarioType scenario)
	{
		if (_stepListener == null)
			_stepListener = new ScenarioSteppedListener()
			{
				public void restart()
				{
					fireDirty();
				}

				public void step(long newTime)
				{
					fireDirty();
				}
			};

		scenario.addScenarioSteppedListener(_stepListener);
	}

	private void stopListeningToThis(ScenarioType scenario)
	{
		scenario.removeScenarioSteppedListener(_stepListener);
	}

	public void dispose()
	{
		super.dispose();

		// do we have a scenario?
		if (_myScenario != null)
			stopListeningToThis(_myScenario);

		// do we have layers?
		if (_myLayers != null)
			stopListeningToThis(_myLayers);

		_myScenario = null;
		_myLayers = null;

		// empty the part monitor
		_myPartMonitor.ditch();
		_myPartMonitor = null;
	}

	public void createPartControl(Composite parent)
	{
		// hey, create the chart
		_myChart = createTheChart(parent);

		// and over-ride the undo button
		IAction undoAction = new UndoActionHandler(getEditorSite(),
				ASSETPlugin.ASSET_CONTEXT);
		IAction redoAction = new RedoActionHandler(getEditorSite(),
				ASSETPlugin.ASSET_CONTEXT);

		getEditorSite().getActionBars().setGlobalActionHandler(
				ActionFactory.UNDO.getId(), undoAction);
		getEditorSite().getActionBars().setGlobalActionHandler(
				ActionFactory.REDO.getId(), redoAction);

		// put in the plot-copy support
		IAction _copyClipboardAction = new Action()
		{
			public void runWithEvent(Event event)
			{
				ExportWMF ew = new ExportWMF(true, false);
				ew.run(null);
			}
		};

		IActionBars actionBars = getEditorSite().getActionBars();
		actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(),
				_copyClipboardAction);
		
		// and start listening
		setupListeners();

	}

	/**
	 * create the chart we're after
	 * 
	 * @param parent
	 *          the parent object to stick it into
	 */
	protected SWTChart createTheChart(Composite parent)
	{
		SWTChart res = new SWTChart(_myLayers, parent)
		{

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public void chartFireSelectionChanged(ISelection sel)
			{
				//
			}
		};
		return res;
	}

	public void setFocus()
	{
		// just put some kind of blank object into the properties window
		// putBackdropIntoProperties();

		// ok, set the drag mode to whatever our common "mode" is.
		// - start off by getting the current mode
		PlotMouseDragger curMode = PlotViewerPlugin.getCurrentMode();

		// has one been set?
		if (curMode != null)
		{
			// yup, better observe it then
			_myChart.setDragMode(curMode);
		}

	}

	public WorldArea getViewport()
	{
		return getChart().getCanvas().getProjection().getDataArea();
	}

	public void setViewport(WorldArea target)
	{
		getChart().getCanvas().getProjection().setDataArea(target);
	}

	public PlainProjection getProjection()
	{
		return getChart().getCanvas().getProjection();
	}

	public void setProjection(PlainProjection proj)
	{
		// yes, just update it.
		_myChart.getCanvas().setProjection(proj);
	}

	public SWTChart getChart()
	{
		return _myChart;
	}

	/**
	 * Returns the ActionbarContributor for the Editor.
	 * 
	 * @return the ActionbarContributor for the Editor.
	 */
	public SubActionBars2 getActionbar()
	{
		return (SubActionBars2) getEditorSite().getActionBars();
	}

	/**
	 * hmm, are we dirty?
	 * 
	 * @return
	 */
	public boolean isDirty()
	{
		return false;
	}

	public void update()
	{
		_myChart.update();
	}

	/**
	 * get the chart to fit to window
	 * 
	 */
	public void rescale()
	{
		_myChart.rescale();
	}

	public void doSave(IProgressMonitor monitor)
	{
	}

	public void doSaveAs()
	{
	}

	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException
	{

		setSite(site);
		setInputWithNotify(input);
		
		// ok, try to pass this file to the scenario controller
		IWorkbenchPage page = this.getEditorSite().getPage();
		IViewReference timeRef = page.findViewReference(ASSETPlugin.SCENARIO_CONTROLLER2);

		// ok, try to pass the file to it.
		if(timeRef == null)
		{
			// ok, better open it
			page.showView(ASSETPlugin.SCENARIO_CONTROLLER, null,IWorkbenchPage.VIEW_VISIBLE );
			timeRef = page.findViewReference(ASSETPlugin.SCENARIO_CONTROLLER2);
		}
		
		if(timeRef == null)
		{
			// bugger, we can't create the scenario controller
			ASSETPlugin.logError(org.eclipse.core.runtime.Status.ERROR, "Can't find scenario controller to give it a file, dying", null);
		}
		else
		{
			ScenarioControllerView cont = (ScenarioControllerView) timeRef.getPart(true);
			
			// extract the path
			String thePath = null;
			
			if(input instanceof FileEditorInput)
			{
				FileEditorInput fi = (FileEditorInput) input;
				thePath = fi.getPath().toOSString();
			}
			
			// ok, now give it the file	
			if(thePath != null)
				cont.filesDropped(new String[]{thePath});
		}
		
		
	}

	public boolean isSaveAsAllowed()
	{
		return false;
	}

	private void setupListeners()
	{
		_myPartMonitor = new PartMonitor(getSite().getWorkbenchWindow()
				.getPartService());

		_myPartMonitor.addPartListener(ScenarioType.class, PartMonitor.ACTIVATED,
				new PartMonitor.ICallback()
				{
					public void eventTriggered(String type, Object part,
							IWorkbenchPart parentPart)
					{
						if (_myScenario != part)
						{
							// are we already listening to it?
							if (_myScenario != null)
								stopListeningToThis(_myScenario);

							// ok, let's start listening to it
							_myScenario = (ScenarioType) part;
							startListeningTo(_myScenario);
						}
					}
				});
		_myPartMonitor.addPartListener(ScenarioType.class, PartMonitor.CLOSED,
				new PartMonitor.ICallback()
				{
					public void eventTriggered(String type, Object part,
							IWorkbenchPart parentPart)
					{
						if (_myScenario == part)
						{
							stopListeningToThis(_myScenario);
							_myScenario = null;
						}
					}
				});
		
		_myPartMonitor.addPartListener(Layers.class, PartMonitor.ACTIVATED,
				new PartMonitor.ICallback()
				{
					public void eventTriggered(String type, Object part,
							IWorkbenchPart parentPart)
					{
						if (_myLayers != part)
						{
							// are we already listening to it?
							if (_myLayers != null)
								stopListeningToThis(_myLayers);

							_myLayers = (Layers) part;
							
							// give it to the chart
							_myChart.setLayers(_myLayers);
							
							// ok, let's start listening to it
							startListeningTo(_myLayers);
							
						}
					}
				});
		_myPartMonitor.addPartListener(Layers.class, PartMonitor.CLOSED,
				new PartMonitor.ICallback()
				{
					public void eventTriggered(String type, Object part,
							IWorkbenchPart parentPart)
					{
						if (_myLayers == part)
						{
							stopListeningToThis(_myLayers);
							_myLayers = null;
						}
					}
				});

	}

	public void selectPlottable(Plottable shape, Layer layer)
	{
		// ignore it, we don't need to see it here.
	}

}
