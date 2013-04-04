package org.mwc.cmap.plotViewer.editors.udig;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.IProject;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.command.navigation.SetViewportBBoxCommand;
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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.part.EditorPart;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mwc.cmap.core.interfaces.IControllableViewport;
import org.mwc.cmap.core.property_support.EditableWrapper;
import org.mwc.cmap.core.property_support.RightClickSupport;

import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GUI.Plottable;
import MWC.GUI.Tools.Chart.HitTester;
import MWC.GUI.Tools.Chart.RightClickEdit;
import MWC.GUI.Tools.Chart.RightClickEdit.ObjectConstruct;
import MWC.GenericData.WorldLocation;

import com.vividsolutions.jts.geom.Coordinate;

public abstract class CorePlotEditor extends EditorPart implements MapPart,
		ISelectionProvider
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

	AddLayerThread _addLayerThread = new AddLayerThread();

	/**
	 * the graphic data we know about
	 */
	protected Layers _myLayers;
	protected UdigViewportCanvasAdaptor _chart;

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
	}

	// TODO
	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener)
	{
		// TODO Auto-generated method stub

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
		_viewer = new MapViewer(parent, SWT.DOUBLE_BUFFERED);

		_viewer.setMap(_map);
		_viewer.init(this);

		this._chart = new UdigViewportCanvasAdaptor(_viewer);
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
		// TODO Auto-generated method stub

	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener)
	{
		// TODO Auto-generated method stub

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
				dist, _chart.getProjection()))
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

				// TODO doSupplementalRightClickProcessing(mmgr, res, theParent);
				notImplementedDialog();
			}
		}
		else
		{
			// not found anything useful,
			// so edit just the projection

			RightClickSupport.getDropdownListFor(mmgr, new Editable[]
			{ _chart.getProjection() }, null, null, _myLayers, true);

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
								// TODO parentFireSelectionChanged(selected);
								notImplementedDialog();
							}
						};

						mmgr.add(editThis);
						// hey, stick in another separator
						mmgr.add(new Separator());
					}
				}
			}

			Menu thisM = mmgr.createContextMenu(_viewer.getControl());
			thisM.setVisible(true);
		}

		Action changeBackColor = new Action("Edit base chart")
		{
			@Override
			public void run()
			{
				notImplementedDialog();
				// EditableWrapper wrapped = new EditableWrapper(_chart, _myLayers);
				// ISelection selected = new StructuredSelection(wrapped);
				// TODO parentFireSelectionChanged(selected);
			}

		};
		mmgr.add(changeBackColor);

		Action editProjection = new Action("Edit Projection")
		{
			@Override
			public void run()
			{
				EditableWrapper wrapped = new EditableWrapper(_chart.getProjection(),
						_myLayers);
				ISelection selected = new StructuredSelection(wrapped);
				// TODO parentFireSelectionChanged(selected);
				notImplementedDialog();
			}

		};
		mmgr.add(editProjection);

	}

	protected void notImplementedDialog()
	{
		MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
				"Not implemented", "I have not implemented this yet");
	}

}
