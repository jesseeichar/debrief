package org.mwc.cmap.plotViewer.editors.udig;

import java.awt.Color;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.IProject;
import net.refractions.udig.project.ProjectBlackboardConstants;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.CreateMapCommand;
import net.refractions.udig.project.render.IViewportModel;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.viewers.MapViewer;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.EditorPart;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.core.interfaces.IControllableViewport;
import org.mwc.cmap.core.property_support.EditableWrapper;
import org.mwc.cmap.core.property_support.RightClickSupport;
import org.mwc.cmap.plotViewer.actions.IChartBasedEditor;
import org.mwc.cmap.plotViewer.editors.chart.SWTCanvas.LocationSelectedAction;

import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GUI.PlainChart;
import MWC.GUI.Plottable;
import MWC.GUI.Layers.DataListener2;
import MWC.GUI.Tools.Chart.HitTester;
import MWC.GUI.Tools.Chart.RightClickEdit;
import MWC.GUI.Tools.Chart.RightClickEdit.ObjectConstruct;
import MWC.GenericData.WorldLocation;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class CorePlotEditor extends EditorPart implements MapPart,
		ISelectionProvider, Editable, IChartBasedEditor
{
	private final class AddLayerThread extends Thread
	{
		BlockingQueue<MWC.GUI.Layer> queue = new LinkedBlockingDeque<MWC.GUI.Layer>();
		{
			setDaemon(true);
			setName("AddLayerThread");
		}

		public void run()
		{
			while (true)
			{
				try
				{
					MWC.GUI.Layer next = queue.take();
					sleep(200); // sleep to give system time to put all layers in queue
					LinkedList<IGeoResource> resources = new LinkedList<IGeoResource>();

					while (next != null)
					{
						PlottableGeoResource resource = PlottableService.INSTANCE
								.addLayer(next);
						resources.add(resource);
						next = queue.poll();
					}

					AddLayersCommand cmd = new AddLayersCommand(resources);
					cmd.setMap(_map);
					cmd.run(new NullProgressMonitor());
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.exit(1);
				}
			}
		}

		public void add(MWC.GUI.Layer theLayer)
		{
			queue.add(theLayer);
		}
	}

	protected MapViewer _viewer;
	protected Map _map;
	private Vector<ISelectionChangedListener> _selectionListeners;

	AddLayerThread _addLayerThread = new AddLayerThread();

	/**
	 * the graphic data we know about
	 */
	protected Layers _myLayers;
	protected UdigViewportCanvasAdaptor _canvas;
	private LocationSelectedAction _copyLocation;
	private DataListener2 _listenForMods;
	private boolean _ignoreDirtyCalls = false;
	protected boolean _plotIsDirty = false;
	private Color _backgroundColor;
	private ISelection _currentSelection;
	private float _lineThickness;
	private UdigChart _chart;

	public CorePlotEditor()
	{
		_addLayerThread.start();
		IProject activeProject = ApplicationGIS.getActiveProject();

		CreateMapCommand command = new CreateMapCommand("NewMap",
				Collections.<IGeoResource> emptyList(), activeProject);
		try
		{
			command.run(new NullProgressMonitor());
		}
		catch (Exception e1)
		{
			throw new RuntimeException(e1);
		}
		_map = (Map) command.getCreatedMap();

		_myLayers = new Layers()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void addThisLayer(final MWC.GUI.Layer theLayer)
			{
				_addLayerThread.add(theLayer);
				super.addThisLayer(theLayer);
			}

			@Override
			public void removeThisLayer(MWC.GUI.Layer theLayer)
			{
				Iterator<net.refractions.udig.project.internal.Layer> layersInternal = _map
						.getLayersInternal().iterator();
				while (layersInternal.hasNext())
				{
					net.refractions.udig.project.internal.Layer layer = layersInternal
							.next();
					if (theLayer.equals(layer.getAdapter(MWC.GUI.Layer.class)))
					{
						layersInternal.remove();
					}
				}
				super.removeThisLayer(theLayer);
			}
		};
		

		_listenForMods = new DataListener2()
		{

			public void dataModified(Layers theData, Layer changedLayer)
			{
				fireDirty();
			}

			public void dataExtended(Layers theData)
			{
				layersExtended();
				fireDirty();
			}

			public void dataReformatted(Layers theData, Layer changedLayer)
			{
				fireDirty();
			}

			@Override
			public void dataExtended(Layers theData, Plottable newItem, Layer parent)
			{
				layersExtended();
				fireDirty();
			}

		};
		_myLayers.addDataExtendedListener(_listenForMods);
		_myLayers.addDataModifiedListener(_listenForMods);
		_myLayers.addDataReformattedListener(_listenForMods);

		
	}
	/**
	 * make a note that the data is now dirty, and needs saving.
	 */
	public void fireDirty()
	{
		if (!_ignoreDirtyCalls )
		{
			_plotIsDirty  = true;
			Display.getDefault().asyncExec(new Runnable()
			{

				@SuppressWarnings("synthetic-access")
				public void run()
				{
					firePropertyChange(PROP_DIRTY);
				}
			});
		}
	}

	/**
	 * new data has been added - have a look at the times
	 */
	protected void layersExtended()
	{

	}
	/**
	 * hmm, are we dirty?
	 * 
	 * @return
	 */
	public final boolean isDirty()
	{
		return _plotIsDirty;
	}
	// TODO
	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener)
	{
		if (_selectionListeners == null)
			_selectionListeners = new Vector<ISelectionChangedListener>(0, 1);

		// see if we don't already contain it..
		if (!_selectionListeners.contains(listener))
			_selectionListeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets
	 * .Composite)
	 */
	@Override
	public void createPartControl(Composite parent)
	{
		getSite().setSelectionProvider(this);
		_viewer = new MapViewer(parent, SWT.DOUBLE_BUFFERED);

		_viewer.setMap(_map);
		_viewer.init(this);

		if(_backgroundColor != null) {
			setBackgroundColor(_backgroundColor);
		}

		this._canvas = new UdigViewportCanvasAdaptor(_viewer);
		this._chart = new UdigChart(this);
		
		_viewer.getViewport().addMouseListener(new DebriefMapMouseListener(this));
		_viewer.getViewport().addMouseMotionListener(
				new DebriefMapMouseMotionListener(this));
		_viewer.getViewport().addPaneListener(new DebriefMapDisplayListener(this));
	}

	@Override
	public void dispose()
	{
		super.dispose();
		_viewer.dispose();
		_viewer = null;
		_myLayers.close();
		_myLayers = null;
	}

	@Override
	@SuppressWarnings(
	{ "rawtypes" })
	public Object getAdapter(Class adapter)
	{
		Object res = null;

		// so, is he looking for the layers?
		if (adapter == CorePlotEditor.class)
		{
			res = this;
		}
		else if (adapter == ISelectionProvider.class)
		{
			res = this;
		}
		else if (adapter == IControllableViewport.class)
		{
			res = this;
		}
		else if (adapter == IControllableViewport.class)
		{
			res = this;
		}

		return res;
	}

	@Override
	public Map getMap()
	{
		return _map;
	}

	@Override
	public ISelection getSelection()
	{
		// TODO Auto-generated method stub
		return new StructuredSelection();
	}

	@Override
	public IStatusLineManager getStatusLineManager()
	{
		return getEditorSite().getActionBars().getStatusLineManager();
	}

	@Override
	public void openContextMenu()
	{
		Point size = _viewer.getControl().getSize();
		int x = size.x / 2;
		int y = size.y / 2;

		openContextMenu(x,y);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener)
	{
		_selectionListeners.remove(listener);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus()
	{
		_viewer.getControl().setFocus();
	}

	@Override
	public void setFont(Control textArea)
	{
		_viewer.setFont(textArea);
	}

	@Override
	public void setSelection(ISelection selection)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setSelectionProvider(IMapEditorSelectionProvider selectionProvider)
	{
		// TODO Auto-generated method stub

	}

	public void openContextMenu(int x, int y)
	{
		MenuManager mmgr = new MenuManager();
		Point scrPoint = new Point(x, y);
 		IViewportModel viewportModel = _map.getViewportModel();
		Coordinate worldCoordinate = viewportModel.pixelToWorld(x, y);
		WorldLocation loc = JtsAdapter.toWorldLocation(worldCoordinate);

		addCopyLocationItem(mmgr, scrPoint, loc);
		
		Layers theData = _myLayers;

		double layerDist = -1;

		// find the nearest editable item
		ObjectConstruct vals = new ObjectConstruct();
		int num = theData.size();
		for (int i = 0; i < num; i++)
		{
			Layer thisL = theData.elementAt(i);
			if (thisL.getVisible())
			{
				// find the nearest items, this method call will recursively pass down
				// through the layers
				RightClickEdit.findNearest(thisL, loc, vals);

				if ((layerDist == -1) || (vals.distance < layerDist))
				{
					layerDist = vals.distance;
				}
			}
		}

		// ok, now retrieve the values produced by the range-finding algorithm
		Plottable res = vals.object;
		Layer theParent = vals.parent;
		double dist = vals.distance;
		Vector<Plottable> noPoints = vals.rangeIndependent;

		// see if this is in our dbl-click range
		if (HitTester.doesHit(new java.awt.Point(scrPoint.x, scrPoint.y), loc,
				dist, _canvas.getProjection()))
		{
			// do nothing, we're all happy
		}
		else
		{
			res = null;
		}

		// have we found something editable?
		if (res != null)
		{
			// so get the editor
			Editable.EditorType e = res.getInfo();
			if (e != null)
			{
				RightClickSupport.getDropdownListFor(mmgr, new Editable[]
				{ res }, new Layer[]
				{ theParent }, new Layer[]
				{ theParent }, _myLayers, false);

			// JESSE - this method is undefined in the current build:	doSupplementalRightClickProcessing(mmgr, res, theParent);
			}
		}
		else
		{
			// not found anything useful,
			// so edit just the projection

			RightClickSupport.getDropdownListFor(mmgr, new Editable[]
			{ _canvas.getProjection() }, null, null, _myLayers, true);

			// also see if there are any other non-position-related items
			if (noPoints != null)
			{
				// stick in a separator
				mmgr.add(new Separator());

				for (Iterator<Plottable> iter = noPoints.iterator(); iter.hasNext();)
				{
					final Plottable pl = iter.next();
					RightClickSupport.getDropdownListFor(mmgr, new Editable[]
					{ pl }, null, null, _myLayers, true);

					// ok, is it editable
					if (pl.getInfo() != null)
					{
						// ok, also insert an "Edit..." item
						Action editThis = new Action("Edit " + pl.getName() + " ...")
						{
							@Override
							public void run()
							{
								// ok, wrap the editab
								EditableWrapper pw = new EditableWrapper(pl, _myLayers);
								ISelection selected = new StructuredSelection(pw);
								fireSelectionChanged(selected);
							}
						};

						mmgr.add(editThis);
						// hey, stick in another separator
						mmgr.add(new Separator());
					}
				}
			}
		}

		Action editBaseChart = new Action("Edit base chart")
		{
			@Override
			public void run()
			{
				 EditableWrapper wrapped = new EditableWrapper(CorePlotEditor.this, _myLayers);
				 ISelection selected = new StructuredSelection(wrapped);
				 fireSelectionChanged(selected);
			}

		};
		mmgr.add(editBaseChart);

		Action editProjection = new Action("Edit Projection")
		{
			@Override
			public void run()
			{
				EditableWrapper wrapped = new EditableWrapper(_canvas.getProjection(),
						_myLayers);
				ISelection selected = new StructuredSelection(wrapped);
				fireSelectionChanged(selected);
			}

		};
		mmgr.add(editProjection);

		Menu thisM = mmgr.createContextMenu(_viewer.getControl());
		thisM.setVisible(true);

	}

	private void addCopyLocationItem(MenuManager mmgr, Point scrPoint,
			WorldLocation loc)
	{
		// right, we create the actions afresh each time here. We can't
		// automatically calculate it.
		_copyLocation = new LocationSelectedAction("Copy cursor location",
				SWT.PUSH, loc)
		{
			/**
			 * @param loc
			 *          the converted world location for the mouse-click
			 * @param pt
			 *          the screen coordinate of the click
			 */
			public void run(WorldLocation theLoc)
			{
				// represent the location as a text-string
				String locText = CorePlugin.toClipboard(theLoc);

				// right, copy the location to the clipboard
				Clipboard clip = CorePlugin.getDefault().getClipboard();
				Object[] data = new Object[]
				{ locText };
				Transfer[] types = new Transfer[]
				{ TextTransfer.getInstance() };
				clip.setContents(data, types);

			}
		};

		mmgr.add(_copyLocation);
		mmgr.add(new Separator());

	}

	protected void notImplementedDialog()
	{
		MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
				"Not implemented", "I have not implemented this yet");
		new UnsupportedOperationException("I have not implemented this yet").printStackTrace();
	}

	public Color getBackgroundColor()
	{
		return _backgroundColor;
	}
	public void setBackgroundColor(Color backgroundColor)
	{
		this._backgroundColor = backgroundColor;
		if (_viewer != null) {
			_viewer.getMap().getBlackboard().put(ProjectBlackboardConstants.MAP__BACKGROUND_COLOR, _backgroundColor);
			_viewer.getRenderManager().refresh(null);
		}
	}

	public void setLineThickness (float lineThickness) {
		this._lineThickness = lineThickness;
	}
	
	public float getLineThickness () {
		return _lineThickness;
	}
	
	public void fireSelectionChanged(ISelection sel)
	{
		// just double-check that we're not already processing this
		if (sel != _currentSelection)
		{
			_currentSelection = sel;
			if (_selectionListeners != null)
			{
				SelectionChangedEvent sEvent = new SelectionChangedEvent(this, sel);
				for (Iterator<ISelectionChangedListener> stepper = _selectionListeners
						.iterator(); stepper.hasNext();)
				{
					ISelectionChangedListener thisL = stepper.next();
					if (thisL != null)
					{
						thisL.selectionChanged(sEvent);
					}
				}
			}
		}
	}
	public Layers getLayers()
	{
		return _myLayers;
	}

	@Override
	public InteractiveChart getChart()
	{
		return _chart;
	}

	@Override
	public void selectPlottable(Plottable shape, Layer layer)
	{
		// TODO Auto-generated method stub
		
	}
}
