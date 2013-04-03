package org.mwc.cmap.plotViewer.editors.udig;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.project.IProject;
import net.refractions.udig.project.command.UndoableComposite;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import net.refractions.udig.project.internal.commands.AddLayerCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.CreateMapCommand;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.viewers.MapViewer;
import net.refractions.udig.ui.PlatformGIS;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.geotools.data.DataUtilities;
import org.mwc.cmap.core.interfaces.IControllableViewport;

import MWC.GUI.Layers;

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
					PlottableGeoResource resource = PlottableService.INSTANCE
							.addLayer(next);
					AddLayersCommand cmd = new AddLayersCommand(
							Collections.singletonList(resource));
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
				Iterator<Layer> layersInternal = _map.getLayersInternal().iterator();
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

		FileDialog dialog = new FileDialog(PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getShell());
		dialog.setFilterExtensions(new String[]
		{ "*.shp" });
		String path = dialog.open();
		File file = new File(path);
		final URL url = DataUtilities.fileToURL(file);

		_viewer.setMap(_map);
		_viewer.init(this);

		PlatformGIS.run(new IRunnableWithProgress()
		{

			@Override
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException
			{

				try
				{
					IService service = CatalogPlugin.getDefault().getLocalCatalog()
							.acquire(url, monitor);

					Layer layer = _viewer.getMap().getLayerFactory()
							.createLayer(service.resources(monitor).get(0));
					UndoableComposite cmds = new UndoableComposite();
					cmds.add(new AddLayerCommand(layer));

					cmds.add(new SetViewportBBoxCommand(JtsAdapter.toEnvelope(_myLayers
							.getBounds())));
					_viewer.getMap().sendCommandASync(cmds);

				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.exit(-1);
				}
			}
		});

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

		return res;
	}

	@Override
	public Map getMap()
	{
		// TODO Auto-generated method stub
		return _viewer.getMap();
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
}
