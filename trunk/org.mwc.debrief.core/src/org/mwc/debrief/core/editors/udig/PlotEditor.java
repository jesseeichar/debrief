/**
 * 
 */
package org.mwc.debrief.core.editors.udig;

import java.awt.Color;
import java.io.InputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.Vector;

import net.refractions.udig.project.internal.render.ViewportModel;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.core.DataTypes.Temporal.ControllableTime;
import org.mwc.cmap.core.DataTypes.Temporal.TimeControlPreferences;
import org.mwc.cmap.core.DataTypes.Temporal.TimeControlProperties;
import org.mwc.cmap.core.DataTypes.Temporal.TimeManager;
import org.mwc.cmap.core.DataTypes.Temporal.TimeProvider;
import org.mwc.cmap.core.DataTypes.TrackData.TrackDataProvider;
import org.mwc.cmap.core.DataTypes.TrackData.TrackDataProvider.TrackDataListener;
import org.mwc.cmap.core.DataTypes.TrackData.TrackManager;
import org.mwc.cmap.core.interfaces.INamedItem;
import org.mwc.cmap.core.ui_support.wizards.SimplePageListWizard;
import org.mwc.cmap.core.wizards.EnterBooleanPage;
import org.mwc.cmap.core.wizards.EnterRangePage;
import org.mwc.cmap.core.wizards.EnterStringPage;
import org.mwc.cmap.core.wizards.SelectColorPage;
import org.mwc.cmap.plotViewer.editors.udig.CorePlotEditor;
import org.mwc.cmap.plotViewer.editors.udig.JtsAdapter;
import org.mwc.debrief.core.interfaces.IPlotEditor;
import org.mwc.debrief.core.interfaces.IPlotLoader;
import org.mwc.debrief.core.interfaces.IPlotLoader.BaseLoader;
import org.mwc.debrief.core.interfaces.IPlotLoader.DeferredPlotLoader;
import org.mwc.debrief.core.loaders.LoaderManager;
import org.mwc.debrief.core.loaders.ReplayLoader;

import Debrief.ReaderWriter.Replay.ImportReplay;
import Debrief.Wrappers.NarrativeWrapper;
import Debrief.Wrappers.SensorContactWrapper;
import Debrief.Wrappers.SensorWrapper;
import MWC.Algorithms.PlainProjection;
import MWC.Algorithms.PlainProjection.RelativeProjectionParent;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GenericData.WatchableList;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldDistance;
import MWC.TacticalData.IRollingNarrativeProvider;

/**
 * @author Jesse
 */
public class PlotEditor extends CorePlotEditor implements IPlotEditor {

	private static final String PLUGIN_ID = "org.mwc.debrief.core";
	// Extension point tag and attributes in plugin.xml
	private static final String EXTENSION_POINT_ID = "DebriefPlotLoader";

	private static final String EXTENSION_TAG = "loader";

	private static final String EXTENSION_TAG_LABEL_ATTRIB = "name";

	private static final String EXTENSION_TAG_EXTENSIONS_ATTRIB = "extensions";

	private static final String EXTENSION_TAG_ICON_ATTRIB = "icon";

	private LoaderManager _loader;

	/**
	 * we keep the reference to our track-type adapter
	 */
	TrackDataProvider _trackDataProvider;

	/**
	 * an object to look after all of the time bits
	 */
	private TimeManager _timeManager;
	private TimeControlProperties _timePreferences;
	
	public PlotEditor()
	{

		// create the track manager to manage the primary & secondary tracks
		_trackDataProvider = new TrackManager(_myLayers);

		// and listen out form modifications, because we want to mark ourselves
		// as
		// dirty once they've updated
		_trackDataProvider.addTrackDataListener(new TrackDataListener()
		{
			public void tracksUpdated(WatchableList primary,
					WatchableList[] secondaries)
			{
				notImplementedDialog();
//				fireDirty();
			}
		});

		// create the time manager. cool
		_timeManager = new TimeManager();
//		_timeManager.addListener(_timeListener,
//				TimeProvider.TIME_CHANGED_PROPERTY_NAME);

		// and how time is managed
		_timePreferences = new TimeControlProperties();

	}
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInputWithNotify(input);

		// ok - declare and load the supplemental plugins which can load
		// datafiles
		initialiseFileLoaders();

		// and start the load
		loadThisFile(input);

		ViewportModel viewportModel = _map.getViewportModelInternal();
		try {
			viewportModel.eSetDeliver(false); // we don't need event to rerender yet
			ReferencedEnvelope envelope = JtsAdapter.toEnvelope(_myLayers.getBounds());
			viewportModel.setBounds(envelope);
		} finally {
			viewportModel.eSetDeliver(true);			
		}

		// lastly, set the title (if we have one)
		this.setPartName(input.getName());
	}
	private void initialiseFileLoaders()
	{
		// hey - sort out our plot readers
		_loader = new LoaderManager(EXTENSION_POINT_ID, EXTENSION_TAG, PLUGIN_ID)
		{

			public INamedItem createInstance(IConfigurationElement configElement,
					String label)
			{
				// get the attributes
				label = configElement.getAttribute(EXTENSION_TAG_LABEL_ATTRIB);
				String icon = configElement.getAttribute(EXTENSION_TAG_ICON_ATTRIB);
				String fileTypes = configElement
						.getAttribute(EXTENSION_TAG_EXTENSIONS_ATTRIB);

				// create the instance
				INamedItem res = new IPlotLoader.DeferredPlotLoader(configElement,
						label, icon, fileTypes);

				// and return it.
				return res;
			}

		};
	}

	/**
	 * @param input
	 *          the file to insert
	 */
	private void loadThisFile(IEditorInput input)
	{
		InputStream is = null;
		if (!input.exists())
		{
			CorePlugin.logError(Status.ERROR,
					"File cannot be found:" + input.getName(), null);
			return;
		}
		try
		{
			IPersistableElement persist = input.getPersistable();
			if (input instanceof IFileEditorInput)
			{
				IFileEditorInput ife = (IFileEditorInput) input;
				IFile iff = ife.getFile();
				iff.refreshLocal(IResource.DEPTH_ONE, null);
				is = iff.getContents();
			}
			else if (persist instanceof IFileEditorInput)
			{
				IFileEditorInput iff = (IFileEditorInput) persist;
				is = iff.getFile().getContents();
			}
			else if (input instanceof FileStoreEditorInput)
			{
				FileStoreEditorInput _input = (FileStoreEditorInput) input;
				URI _uri = _input.getURI();
				Path _p = new Path(_uri.getPath());
				IFileStore _ifs = EFS.getLocalFileSystem().getStore(_p);
				is = _ifs.openInputStream(EFS.NONE, null);
			}

			if (is != null)
				loadThisStream(is, input.getName());
			else
			{
				CorePlugin.logError(Status.INFO, "Failed to load file from:" + input,
						null);
			}

		}
		catch (ResourceException e)
		{
			CorePlugin.logError(Status.ERROR,
					"Resource out of sync:" + input.getName() + " REFRESH the workspace",
					null);
			MessageDialog
					.openError(
							Display.getDefault().getActiveShell(),
							"File out of sync",
							"This file has been edited or removed:"
									+ input.getName()
									+ "\nPlease right-click on your navigator project and press Refresh");
		}
		catch (CoreException e)
		{
			CorePlugin.logError(Status.ERROR, "Problem loading data file", e);
		}
	}
	private void loadThisStream(InputStream is, String fileName)
	{
		// right, see if any of them will do our edit
		IPlotLoader[] loaders = _loader.findLoadersFor(fileName);
		// did we find any?
		if (loaders.length > 0)
		{
			// cool, give them a go...
			try
			{
				for (int i = 0; i < loaders.length; i++)
				{
					IPlotLoader thisLoader = loaders[i];

					// get it to load. Just in case it's an asychronous load
					// operation, we
					// rely on it calling us back (loadingComplete)
					thisLoader.loadFile(this, is, fileName);

					// special handling - popup a dialog to allow sensor name/color to be
					// set if there's just one sensor
					if (thisLoader instanceof DeferredPlotLoader)
					{
						DeferredPlotLoader ld = (DeferredPlotLoader) thisLoader;
						BaseLoader loader = ld.getLoader();
						if (loader != null)
						{
							if (loader instanceof ReplayLoader)
							{
								ReplayLoader rl = (ReplayLoader) loader;
								ImportReplay ir = rl.getReplayLoader();
								Vector<SensorWrapper> sensors = ir.getNewlyLoadedSensors();
								if (sensors.size() == 1)
								{
									SensorWrapper thisS = sensors.firstElement();
									nameThisSensor(thisS);
								}
							}
						}
					}
				}
			}
			catch (RuntimeException e)
			{
				CorePlugin.logError(Status.ERROR, "Problem loading data file:"
						+ fileName, e);
			}
		}
	}

	private void nameThisSensor(SensorWrapper thisS)
	{
		// create the wizard to color/name this
		SimplePageListWizard wizard = new SimplePageListWizard();

		// right, just have a quick look and see if the sensor has range data -
		// because
		// if it doesn't we'll let the user set a default
		Enumeration<Editable> cuts = thisS.elements();
		boolean needsRange = false;
		if (cuts.hasMoreElements())
		{
			Editable firstCut = cuts.nextElement();
			SensorContactWrapper scw = (SensorContactWrapper) firstCut;
			// do we have bearing?
			if (scw.getHasBearing())
			{
				// yes. now are we waiting for a range?
				if (scw.getRange() == null)
				{
					needsRange = true;
				}
			}
		}
		final String imagePath = "images/NameSensor.jpg";

		EnterStringPage getName = new EnterStringPage(null, thisS.getName(),
				"Import Sensor data", "Please provide the name for this sensor",
				"a one-word title for this block of sensor contacts (e.g. S2046)",
				imagePath, null, false);
		SelectColorPage getColor = new SelectColorPage(null, thisS.getColor(),
				"Import Sensor data", "Now format the new sensor",
				"The default color for the cuts for this new sensor", imagePath, null);
		EnterBooleanPage getVis = new EnterBooleanPage(null, false,
				"Import Sensor data",
				"Please specify if this sensor should be displayed once loaded",
				"yes/no", imagePath, null);
		WorldDistance defRange = new WorldDistance(5000, WorldDistance.YARDS);
		EnterRangePage getRange = new EnterRangePage(
				null,
				"Import Sensor data",
				"Please provide a default range for the sensor cuts \n(or enter 0.0 to leave them as infinite length)",
				"Default range", defRange, imagePath, null);
		wizard.addWizard(getName);
		wizard.addWizard(getColor);
		if (needsRange)
			wizard.addWizard(getRange);
		wizard.addWizard(getVis);
		WizardDialog dialog = new WizardDialog(Display.getCurrent()
				.getActiveShell(), wizard);
		dialog.create();
		dialog.setBlockOnOpen(true);
		dialog.open();
		// did it work?
		if (dialog.getReturnCode() == WizardDialog.OK)
		{
			// ok, use the name
			thisS.setName(getName.getString());
			thisS.setColor(getColor.getColor());
			thisS.setVisible(getVis.getBoolean());

			// are we doing range?
			if (needsRange)
			{
				WorldDistance theRange = getRange.getRange();

				// did a range get entered?
				if ((theRange != null) && (theRange.getValue() != 0))
				{
					Enumeration<Editable> iter = thisS.elements();
					while (iter.hasMoreElements())
					{
						SensorContactWrapper cut = (SensorContactWrapper) iter
								.nextElement();
						cut.setRange(new WorldDistance(theRange));
					}
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.
	 * IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * method called when a helper object has completed a plot-load operation
	 * 
	 * @param source
	 */
	public void loadingComplete(Object source)
	{
//  TODO
//		// ok, stop listening for dirty calls - since there will be so many and
//		// we
//		// don't want
//		// to start off with a dirty plot
//		startIgnoringDirtyCalls();
//
//		DebriefPlugin.logError(Status.INFO, "File load received", null);
//
//		// and update the time management bits
//		TimePeriod timePeriod = getPeriodFor(_myLayers);
//
//		if (timePeriod != null)
//		{
//			_timeManager.setPeriod(this, timePeriod);
//
//			// also give it a current DTG (if it doesn't have one)
//			if (_timeManager.getTime() == null)
//				_timeManager.setTime(this, timePeriod.getStartDTG(), false);
//		}
//
//		// done - now we can process dirty calls again
//		stopIgnoringDirtyCalls();

	}
	@SuppressWarnings("rawtypes")
	public Object getAdapter(Class adapter)
	{
		Object res = null;

		if (adapter == Layers.class)
		{
			if (_myLayers != null)
				res = _myLayers;
		}
		else if (adapter == TrackManager.class)
		{
			res = _trackDataProvider;
		}
		else if (adapter == TrackDataProvider.class)
		{
			res = _trackDataProvider;
		}
		else if (adapter == PlainProjection.class)
		{
			res = _chart.getProjection();
		}
		// TODO
//		else if (adapter == TimeControllerOperationStore.class)
//		{
//			res = getTimeControllerOperations();
//		}
//		else if (adapter == LayerPainterManager.class)
//		{
//			res = _layerPainterManager;
//		}
//		else if (adapter == ControllablePeriod.class)
//		{
//			res = _myOperations;
//		}
		else if (adapter == TimeControlPreferences.class)
		{
			res = _timePreferences;
		}
		else if (adapter == ControllableTime.class)
		{
			res = _timeManager;
		}
		else if (adapter == TimeProvider.class)
		{
			res = _timeManager;
		}
//		else if (adapter == IGotoMarker.class)
//		{
//			return new IGotoMarker()
//			{
//				public void gotoMarker(IMarker marker)
//				{
//					String lineNum = marker.getAttribute(IMarker.LINE_NUMBER, "na");
//					if (lineNum != "na")
//					{
//						// right, convert to DTG
//						HiResDate tNow = new HiResDate(0, Long.parseLong(lineNum));
//						_timeManager.setTime(this, tNow, true);
//					}
//				}
//
//			};
//		}

		else if (adapter == IRollingNarrativeProvider.class)
		{
			// so, do we have any narrative data?
			Layer narr = _myLayers.findLayer(ImportReplay.NARRATIVE_LAYER);

			if (narr != null)
			{
				// did we find it?
				// cool, cast to object
				final NarrativeWrapper wrapper = (NarrativeWrapper) narr;

				res = wrapper;
			}
			else
			{
				// create an empty narrative warpper
				res = new NarrativeWrapper("Empty");
			}
		}
		else if (adapter == RelativeProjectionParent.class)
		{ // TODO
//			if (_myRelativeWrapper == null)
//			{
//				_myRelativeWrapper = new RelativeProjectionParent()
//				{
//
//					public double getHeading()
//					{
//						double res1 = 0.0;
//						Watchable thePos = getFirstPosition(_trackDataProvider,
//								_timeManager);
//
//						if (thePos != null)
//						{
//							// yup, get the centre point
//							res1 = thePos.getCourse();
//						}
//
//						return res1;
//					}
//
//					public WorldLocation getLocation()
//					{
//						MWC.GenericData.WorldLocation res1 = null;
//						Watchable thePos = getFirstPosition(_trackDataProvider,
//								_timeManager);
//
//						if (thePos != null)
//						{
//							// yup, get the centre point
//							res1 = thePos.getBounds().getCentre();
//						}
//						return res1;
//					}
//
//					private Watchable getFirstPosition(TrackDataProvider provider,
//							TimeManager manager)
//					{
//						Watchable res = null;
//
//						// do we have a primary?
//						WatchableList priTrack = provider.getPrimaryTrack();
//						if (priTrack == null)
//						{
//							CorePlugin.logError(Status.ERROR,
//									"Can't do relative projection without primary track", null);
//						}
//						else
//						{
//							Watchable[] list = priTrack.getNearestTo(manager.getTime());
//							if (list != null)
//								if (list.length > 0)
//									res = list[0];
//						}
//
//						return res;
//					}
//				};
//			}
//			res = _myRelativeWrapper;
		}

		// did we find anything?
		if (res == null)
		{
			// nope, don't bother.
			res = super.getAdapter(adapter);
		}

		// ok, done
		return res;
	}
	
	// TODO implement the following methods
	@Override
	public void setViewport(WorldArea target) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public WorldArea getViewport() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setProjection(PlainProjection proj) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public PlainProjection getProjection() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void rescale() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void setBackgroundColor(Color theColor) {
		// TODO Auto-generated method stub
		
	}
}
