package org.mwc.cmap.core;

import java.awt.Color;
import java.util.*;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.*;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.*;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import Debrief.GUI.Frames.Application;
import Debrief.Tools.Tote.Calculations.rangeCalc;
import MWC.GUI.ToolParent;
import MWC.GUI.Chart.Painters.CoastPainter;
import MWC.GUI.Tools.Palette.*;
import MWC.GenericData.WorldLocation;

/**
 * The main plugin class to be used in the desktop.
 */
public class CorePlugin extends AbstractUIPlugin
{

	public static final String LAYER_MANAGER = "org.mwc.cmap.layer_manager.views.LayerManagerView";

	public static final String NARRATIVES = "org.mwc.cmap.narrative.views.NarrativeView";
    public static final String NARRATIVES2 = "com.borlander.ianmayo.nviewer.app.view";

	public static final String PLOT_3d = "org.mwc.cmap.plot3d.views.Plot3dView";

	public static final String TOTE = "org.mwc.cmap.tote.views.ToteView";

	public static final String TIME_CONTROLLER = "org.mwc.cmap.TimeController.views.TimeController";

	public static final String XY_PLOT = "org.mwc.cmap.xyplot.views.XYPlotView";

	public static final String OVERVIEW_PLOT = "org.mwc.cmap.overview.views.ChartOverview";

	public static final String STACKED_DOTS = "org.mwc.debrief.track_shift.views.StackedDotsView";

	public static final String POLYGON_EDITOR = "org.mwc.cmap.core.editor_views.PolygonEditorView";

	// The shared instance.
	private static CorePlugin plugin;

	// Resource bundle.
	private ResourceBundle resourceBundle;

	/**
	 * the Debrief tool-parent used to provide legacy access to properties
	 */
	private static DebriefToolParent _toolParent;

	/**
	 * where we cache our images
	 */
	private ImageRegistry _imageRegistry;

	/**
	 * our CMAP-wide clipboard
	 */
	private Clipboard _myClipboard;

	/**
	 * the undo buffer we manage/support
	 */
	private static IOperationHistory _myHistory;

	/**
	 * and the context used to describe our undo list
	 */
	public final static IUndoContext CMAP_CONTEXT = new ObjectUndoContext("CMAP");

	/**
	 * fixed string used to indicate a string is in our location format
	 */
	public static final String LOCATION_STRING_IDENTIFIER = "LOC:";

	/**
	 * The constructor.
	 */
	public CorePlugin()
	{
		super();
		plugin = this;

		// store our color property editor
		java.beans.PropertyEditorManager.registerEditor(Color.class,
				MWC.GUI.Properties.ColorPropertyEditor.class);
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception
	{
		super.start(context);

		// create something capable of handling legacy preferences
		_toolParent = new DebriefToolParent(getPreferenceStore(), getHistory());

		// tell the VPF generator where to get its preferences from
		CreateVPFLayers.initialise(_toolParent);

		// also initialise the ETOPO wrapper (if we have to)
		CreateTOPO.initialise(_toolParent);

		// and the range calculator
		rangeCalc.init(_toolParent);

		// and the coastline-reader
		CoastPainter.initialise(_toolParent);

		// and the application - so we can use our own toolparent for the properties
		Application.initialise(_toolParent);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception
	{
		super.stop(context);
		plugin = null;
		resourceBundle = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static CorePlugin getDefault()
	{
		return plugin;
	}

	/**
	 * retrieve the toolparent we're using
	 */
	public static ToolParent getToolParent()
	{
		return _toolParent;
	}

	/**
	 * Returns the string from the plugin's resource bundle, or 'key' if not
	 * found.
	 */
	public static String getResourceString(String key)
	{
		ResourceBundle bundle = CorePlugin.getDefault().getResourceBundle();
		try
		{
			return (bundle != null) ? bundle.getString(key) : key;
		} catch (MissingResourceException e)
		{
			return key;
		}
	}

	/**
	 * get the CMAP clipboard
	 * 
	 * @return
	 */
	public Clipboard getClipboard()
	{
		if (_myClipboard == null)
			_myClipboard = new Clipboard(Display.getCurrent());

		return _myClipboard;
	}

	/**
	 * get the undo buffer
	 * 
	 * @return the undo buffer (called a History in Eclipse)
	 */
	public static IOperationHistory getHistory()
	{
		if (_myHistory == null)
			_myHistory = OperationHistoryFactory.getOperationHistory();

		return _myHistory;
	}

	/**
	 * Returns the plugin's resource bundle,
	 */
	public ResourceBundle getResourceBundle()
	{
		try
		{
			if (resourceBundle == null)
				resourceBundle = ResourceBundle
						.getBundle("org.mwc.cmap.core.CorePluginResources");
		} catch (MissingResourceException x)
		{
			resourceBundle = null;
		}
		return resourceBundle;
	}

	/**
	 * @param asSelection
	 * @param parentPart
	 */
	public static void editThisInProperties(
			final Vector<ISelectionChangedListener> selectionListeners,
			final StructuredSelection asSelection,
			final ISelectionProvider selectionProvider, IWorkbenchPart parentPart)
	{
		// hey, better make sure the properties window is open
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
		final IWorkbenchPage page = win.getActivePage();

		// get ready for the start/end times
		// right, we need the time controller if we're going to get the
		// times
		// select the part that wants to do the editing (otherwise the properties
		// window just ignores it's selection)
		page.activate(parentPart);

		// fire the update async - so the current page is clearly activated before
		// marking the selection
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				try
				{
					// introduce a pause
					try
					{
						Thread.sleep(100);
					} catch (InterruptedException e)
					{
						e.printStackTrace();
					}
					
					// now update the selection
					if (selectionListeners != null)
					{
						SelectionChangedEvent sEvent = new SelectionChangedEvent(
								selectionProvider, asSelection);
						for (Iterator stepper = selectionListeners.iterator(); stepper
								.hasNext();)
						{
							ISelectionChangedListener thisL = (ISelectionChangedListener) stepper
									.next();
							if (thisL != null)
							{
								thisL.selectionChanged(sEvent);
							}
						}
					}
					// and show the properties view
					page.showView(IPageLayout.ID_PROP_SHEET, null,
							IWorkbenchPage.VIEW_VISIBLE);
				} catch (PartInitException e)
				{
					logError(Status.ERROR,
							"Failed to open properties view when showing timer properties", e);
				}
			}
		});
	}

	/**
	 * convenience method to assist in extracting a location from the clipboard
	 * 
	 * @param txt
	 * @return
	 */
	public static WorldLocation fromClipboard(String txt)
	{
		// get rid of the title
		String dataPart = txt.substring(LOCATION_STRING_IDENTIFIER.length(), txt
				.length());
		StringTokenizer st = new StringTokenizer(dataPart);
		String latP = st.nextToken(",");
		String longP = st.nextToken(",");
		String depthP = st.nextToken();
		Double _lat = new Double(latP);
		Double _long = new Double(longP);
		Double _depth = new Double(depthP);
		WorldLocation res = new WorldLocation(_lat.doubleValue(), _long
				.doubleValue(), _depth.doubleValue());
		return res;
	}

	/**
	 * convenience method to check if a string is in our format
	 * 
	 * @param txt
	 * @return
	 */
	public static boolean isLocation(String txt)
	{
		return txt.startsWith(LOCATION_STRING_IDENTIFIER);
	}

	/**
	 * convenience method to assist placing locations on the clipboard
	 * 
	 * @param loc
	 * @return
	 */
	public static String toClipboard(WorldLocation loc)
	{
		String res = LOCATION_STRING_IDENTIFIER + loc.getLat() + ","
				+ loc.getLong() + "," + loc.getDepth();
		return res;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path.
	 * 
	 * @param path
	 *          the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path)
	{
		return AbstractUIPlugin
				.imageDescriptorFromPlugin("org.mwc.cmap.core", path);
	}

	/**
	 * error logging utility
	 * 
	 * @param severity
	 *          the severity; one of <code>OK</code>, <code>ERROR</code>,
	 *          <code>INFO</code>, <code>WARNING</code>, or
	 *          <code>CANCEL</code>
	 * @param message
	 *          a human-readable message, localized to the current locale
	 * @param exception
	 *          a low-level exception, or <code>null</code> if not applicable
	 */
	public static void logError(int severity, String message, Throwable exception)
	{
		Status stat = new Status(severity, "org.mwc.cmap.core", Status.OK, message,
				exception);
		getDefault().getLog().log(stat);
	}

	private static ImageRegistry getRegistry()
	{
		return plugin._imageRegistry;
	}

	public static Image getImageFromRegistry(String name)
	{
		Image res = null;

		// do we already have an image
		if (getRegistry() == null)
		{
			plugin._imageRegistry = new ImageRegistry();
		}

		// ok - do we have it already?
		res = getRegistry().get(name);

		if (res == null)
		{
			ImageDescriptor desc = getImageDescriptor("icons/" + name);
			getRegistry().put(name, desc);
			res = getRegistry().get(name);
		}

		// and return it..
		return res;
	}

	/**
	 * show a message to the user
	 * 
	 * @param title
	 * @param message
	 */
	public static void showMessage(final String title, final String message)
	{
		MessageDialog.openInformation(Display.getCurrent().getActiveShell(), title,
				message);
	}

	/**
	 * run this supplied action, then add it to our undo buffer
	 * 
	 * @param theAction
	 *          the action to run...
	 */
	public static void run(IUndoableOperation theAction)
	{
		// check the action arrived...
		if (theAction != null)
		{
			// and now run it
			try
			{
				// add, then run the action to the buffer
				getHistory().execute(theAction, null, null);
			} catch (ExecutionException e)
			{
				logError(Status.ERROR, "Whilst adding new action to history buffer", e);
			}
		}
	}

	public static IViewPart openView(String viewName)
	{
		IViewPart res = null;
		try
		{
			IWorkbench wb = PlatformUI.getWorkbench();
			IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
			IWorkbenchPage page = win.getActivePage();
			// right, open the view.
			res = page.showView(viewName);
		} catch (PartInitException e)
		{
			e.printStackTrace();
			logError(Status.ERROR, "Failed to open " + viewName + "view", e);
		}
		return res;
	}

	/**
	 * create an action that we can stick in our manager
	 * 
	 * @param target
	 * @param description
	 * @param host
	 * @return
	 */
	public static Action createOpenHelpAction(final String target,
			String description, final ViewPart host)
	{
		// sort out the description
		if (description == null)
			description = "Help";

		Action res = new Action(description, Action.AS_PUSH_BUTTON)
		{
			public void runWithEvent(Event event)
			{
				host.getViewSite().getWorkbenchWindow().getWorkbench().getHelpSystem()
						.displayHelp(target);
			}
		};
		res.setToolTipText("View help on this component");
		res.setImageDescriptor(CorePlugin
				.getImageDescriptor("icons/linkto_help.gif"));
		return res;
	}

	/**
	 * make it easy to declare context sensitive help
	 * 
	 * @param parent
	 * @param context
	 */
	public static void declareContextHelp(final Composite parent,
			final String context)
	{
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, context);
	}

	public static IViewPart openSecondaryView(String viewName,
			String secondaryId, int state)
	{
		IViewPart res = null;
		try
		{
			IWorkbench wb = PlatformUI.getWorkbench();
			IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
			IWorkbenchPage page = win.getActivePage();
			// right, open the view.
			res = page.showView(viewName, secondaryId, state);
		} catch (PartInitException e)
		{
			e.printStackTrace();
			logError(Status.ERROR, "Failed to open secondary " + viewName + "view", e);
		}
		return res;
	}

}
