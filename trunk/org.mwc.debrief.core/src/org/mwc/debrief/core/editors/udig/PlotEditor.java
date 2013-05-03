/**
 * 
 */
package org.mwc.debrief.core.editors.udig;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import net.refractions.udig.project.internal.render.ViewportModel;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
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
import org.mwc.debrief.core.DebriefPlugin;
import org.mwc.debrief.core.interfaces.IPlotEditor;
import org.mwc.debrief.core.interfaces.IPlotLoader;
import org.mwc.debrief.core.interfaces.IPlotLoader.BaseLoader;
import org.mwc.debrief.core.interfaces.IPlotLoader.DeferredPlotLoader;
import org.mwc.debrief.core.loaders.LoaderManager;
import org.mwc.debrief.core.loaders.ReplayLoader;
import org.mwc.debrief.core.loaders.xml_handlers.DebriefEclipseXMLReaderWriter;
import org.osgi.framework.Bundle;

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
import MWC.GenericData.WorldDistance;
import MWC.TacticalData.IRollingNarrativeProvider;

/**
 * @author Jesse
 */
public class PlotEditor extends CorePlotEditor implements IPlotEditor
{

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
	private EditorType _myEditor;

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
				// fireDirty();
			}
		});

		// create the time manager. cool
		_timeManager = new TimeManager();
		// _timeManager.addListener(_timeListener,
		// TimeProvider.TIME_CHANGED_PROPERTY_NAME);

		// and how time is managed
		_timePreferences = new TimeControlProperties();

		_myEditor = new PlotEditorEditorType(this);
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException
	{
		setSite(site);
		setInputWithNotify(input);

		// ok - declare and load the supplemental plugins which can load
		// datafiles
		initialiseFileLoaders();

		// and start the load
		loadThisFile(input);
//
//		ViewportModel viewportModel = _map.getViewportModelInternal();
//		try
//		{
//			viewportModel.eSetDeliver(false); // we don't need event to rerender yet
//			ReferencedEnvelope envelope = JtsAdapter
//					.toEnvelope(_myLayers.getBounds());
//			viewportModel.setBounds(envelope);
//		}
//		finally
//		{
//			viewportModel.eSetDeliver(true);
//		}

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
	public void doSave(IProgressMonitor monitor)
	{
		IEditorInput input = getEditorInput();
		String ext = null;

		// do we have an input
		if (input.exists())
		{
			// get the file suffix
			if (input instanceof IFileEditorInput)
			{
				IFile file = null;
				file = ((IFileEditorInput) getEditorInput()).getFile();
				IPath path = file.getFullPath();
				ext = path.getFileExtension();
			}
			else if (input instanceof FileStoreEditorInput)
			{
				FileStoreEditorInput fi = (FileStoreEditorInput) input;
				URI uri = fi.getURI();
				Path path = new Path(uri.getPath());
				ext = path.getFileExtension();
			}

			// right, have a look at it.
			if ((ext == null) || (!ext.equalsIgnoreCase("xml")))
			{
				String msg = "Debrief stores data in a structured (xml) file format,";
				msg += "\nwhich is different to the format you've used to load the data.";
				msg += "\nThus you must specify a new folder to "
						+ "store the plot, and a new filename.";
				msg += "\nNote: it's important that you give the file a .xml file suffix";
				MessageDialog md = new MessageDialog(getEditorSite().getShell(),
						"Save as", null, msg, MessageDialog.WARNING, new String[]
						{ "Ok" }, 0);
				md.open();

				// not, we have to do a save-as
				doSaveAs("Can't store this file-type, select a target folder, and remember to save as Debrief plot-file (*.xml)");
			}
			else
			{

				OutputStream tmpOS = null;

				try
				{
					// NEW STRATEGY. Save to tmp first, then overwrite existing on
					// success.

					// 1. create the temp file
					File tmpFile = File.createTempFile("DebNG_tmp", ".xml");
					tmpFile.createNewFile();
					tmpFile.deleteOnExit();

					// 1a. record the name of the tmp file in the log
					String filePath = tmpFile.getAbsolutePath();
					CorePlugin.logError(Status.INFO, "Created temp save file at:"
							+ filePath, null);

					// 2. open the file as a stream
					tmpOS = new FileOutputStream(tmpFile);

					// 3. save to this stream
					doSaveTo(tmpOS, monitor);

					tmpOS.close();
					tmpOS = null;

					// sort out the file size
					CorePlugin.logError(Status.INFO,
							"Saved file size is:" + tmpFile.length() / 1024 + " Kb", null);

					// 4. Check there's something in the temp file
					if (tmpFile.exists())
						if (tmpFile.length() == 0)
						{
							// save failed throw exception (to be collected shortly
							// afterwards)
							throw new RuntimeException("Stored file is of zero size");
						}
						else
						{

							// save worked. cool.

							// 5. overwrite the existing file with the saved file
							// - note we will only reach this point if the save succeeded.
							// sort out where we're saving to
							if (input instanceof IFileEditorInput)
							{
								CorePlugin.logError(Status.INFO,
										"Performing IFileEditorInput save", null);

								IFile file = ((IFileEditorInput) getEditorInput()).getFile();

								// get the current path (since we're going to be moving the temp
								// one to it
								IPath thePath = file.getLocation();

								// create a backup path
								IPath bakPath = file.getFullPath().addFileExtension("bak");

								// delete any existing backup file
								File existingBackupFile = new File(file.getLocation()
										.addFileExtension("bak").toOSString());
								if (existingBackupFile.exists())
								{
									CorePlugin.logError(Status.INFO,
											"Existing back file still there, having to delete"
													+ existingBackupFile.getAbsolutePath(), null);
									existingBackupFile.delete();
								}

								// now rename the existing file as the backup
								file.move(bakPath, true, monitor);

								// move the temp file to be our real working file
								tmpFile.renameTo(thePath.toFile().getAbsoluteFile());

								// finally, delete the backup file
								if (existingBackupFile.exists())
								{
									CorePlugin.logError(Status.INFO,
											"Save operation completed successfully, deleting backup file"
													+ existingBackupFile.getAbsolutePath(), null);
									existingBackupFile.delete();
								}

								// throw in a refresh - since we've done the save outside
								// Eclipse
								file.getParent()
										.refreshLocal(IResource.DEPTH_INFINITE, monitor);

							}
							else if (input instanceof FileStoreEditorInput)
							{

								CorePlugin.logError(Status.INFO,
										"Performing FileStoreEditorInput save", null);

								// get the data-file
								FileStoreEditorInput fi = (FileStoreEditorInput) input;
								URI _uri = fi.getURI();
								Path _p = new Path(_uri.getPath());

								// create pointers to the existing file, and the backup file
								IFileStore existingFile = EFS.getLocalFileSystem().getStore(_p);
								IFileStore backupFile = EFS.getLocalFileSystem().getStore(
										_p.addFileExtension("bak"));

								// delete any existing backup file
								IFileInfo backupStatus = backupFile.fetchInfo();
								if (backupStatus.exists())
								{
									CorePlugin.logError(Status.INFO,
											"Existing back file still there, having to delete"
													+ backupFile.toURI().getRawPath(), null);
									backupFile.delete(EFS.NONE, monitor);
								}

								// now rename the existing file as the backup
								existingFile.move(backupFile, EFS.OVERWRITE, monitor);

								// and rename the temp file as the working file
								tmpFile.renameTo(existingFile.toLocalFile(EFS.NONE, monitor));

								if (backupStatus.exists())
								{
									CorePlugin.logError(Status.INFO,
											"Save operation successful, deleting backup file"
													+ backupFile.toURI().getRawPath(), null);
									backupFile.delete(EFS.NONE, monitor);
								}

							}
						}
				}
				catch (CoreException e)
				{
					CorePlugin.logError(Status.ERROR,
							"Failed whilst saving external file", e);
				}
				catch (FileNotFoundException e)
				{
					CorePlugin.logError(Status.ERROR,
							"Failed to find local file to save to", e);
				}
				catch (Exception e)
				{
					CorePlugin.logError(Status.ERROR, "Unknown file-save error occurred",
							e);
				}
				finally
				{
					try
					{
						if (tmpOS != null)
							tmpOS.close();
					}
					catch (IOException e)
					{
						CorePlugin.logError(Status.ERROR, "Whilst performing save", e);
					}
				}
			}
		}
	}

	public void doSaveAs(String message)
	{
		// do we have a project?
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject[] projects = workspace.getRoot().getProjects();
		if ((projects == null) || (projects.length == 0))
		{
			String msg = "Debrief plots are stored in 'Projects'";
			msg += "\nBut you do not yet have one defined.";
			msg += "\nPlease follow the 'Generating a project for your data' cheat";
			msg += "\nsheet, accessed from Help/Cheat Sheets then Debrief/Getting started.";
			msg += "\nOnce you have created your project, please start the Save process again.";
			msg += "\nNote: the cheat sheet will open automatically when you close this dialog.";
			MessageDialog md = new MessageDialog(getEditorSite().getShell(),
					"Save as", null, msg, MessageDialog.WARNING, new String[]
					{ "Ok" }, 0);
			md.open();

			// try to open the cheat sheet
			final String CHEAT_ID = "org.mwc.debrief.help.started.generate_project";

			Display.getCurrent().asyncExec(new Runnable()
			{
				public void run()
				{
					OpenCheatSheetAction action = new OpenCheatSheetAction(CHEAT_ID);
					action.run();
				}
			});

			// ok, drop out - we can't do a save anyway
			return;
		}

		// get the workspace

		SaveAsDialog dialog = new SaveAsDialog(getEditorSite().getShell());
		dialog.setTitle("Save Plot As");
		if (getEditorInput() instanceof FileEditorInput)
		{
			// this has been loaded from the navigator
			IFile oldFile = ((FileEditorInput) getEditorInput()).getFile();
			// dialog.setOriginalFile(oldFile);

			IPath oldPath = oldFile.getFullPath();
			IPath newStart = oldPath.removeFileExtension();
			IPath newPath = newStart.addFileExtension("xml");
			File asFile = newPath.toFile();
			String newName = asFile.getName();
			dialog.setOriginalName(newName);
		}
		else if (getEditorInput() instanceof FileStoreEditorInput)
		{
			// this has been dragged from an explorer window
			FileStoreEditorInput fi = (FileStoreEditorInput) getEditorInput();
			URI uri = fi.getURI();
			File thisFile = new File(uri.getPath());
			String newPath = fileNamePartOf(thisFile.getAbsolutePath());
			newPath += ".xml";
			dialog.setOriginalName(newPath);
		}

		dialog.create();
		if (message != null)
			dialog.setMessage(message, IMessageProvider.WARNING);
		else
			dialog.setMessage("Save file to another location.");
		dialog.open();
		IPath path = dialog.getResult();

		if (path == null)
		{
			return;
		}
		else
		{
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			if (!file.exists())
				try
				{
					System.out.println("creating:" + file.getName());
					file.create(new ByteArrayInputStream(new byte[]
					{}), false, null);
				}
				catch (CoreException e)
				{
					DebriefPlugin.logError(IStatus.ERROR,
							"Failed trying to create new file for save-as", e);
					return;
				}

			OutputStream os = null;
			try
			{
				os = new FileOutputStream(file.getLocation().toFile(), false);
				// ok, write to the file
				doSaveTo(os, new NullProgressMonitor());

				// also make this new file our input
				IFileEditorInput newInput = new FileEditorInput(file);
				setInputWithNotify(newInput);

				// lastly, trigger a navigator refresh
				IFile iff = newInput.getFile();
				iff.refreshLocal(IResource.DEPTH_ONE, null);

			}
			catch (FileNotFoundException e)
			{
				CorePlugin
						.logError(Status.ERROR, "Failed whilst performing Save As", e);
			}
			catch (CoreException e)
			{
				CorePlugin.logError(Status.ERROR,
						"Refresh failed after saving new file", e);
			}
			finally
			{
				// and close it
				try
				{
					if (os != null)
						os.close();
				}
				catch (IOException e)
				{
					CorePlugin.logError(Status.ERROR, "Whilst performaing save-as", e);
				}

			}

		}

		_plotIsDirty = false;
		firePropertyChange(PROP_DIRTY);
	}

	/**
	 * utility function to extact the root part of this filename
	 * 
	 * @param fileName
	 *          full file path
	 * @return root of file name (before the . marker)
	 */
	private String fileNamePartOf(String fileName)
	{
		if (fileName == null)
		{
			throw new IllegalArgumentException("file name == null");
		}

		// ok, extract the parent portion
		File wholeFile = new File(fileName);
		String parentSection = wholeFile.getParent();
		int parentLen = parentSection.length() + 1;
		int fileLen = fileName.length();
		String fileSection = fileName.substring(parentLen, fileLen);

		int pos = fileSection.lastIndexOf('.');
		if (pos > 0 && pos < fileSection.length() - 1)
		{
			return fileSection.substring(0, pos);
		}
		return "";
	}

	/**
	 * save our plot to the indicated location
	 * 
	 * @param destination
	 *          where to save plot to
	 * @param monitor
	 *          somebody/something to be informed about progress
	 */
	private void doSaveTo(OutputStream os, IProgressMonitor monitor)
	{
		if (os != null)
		{

			IProduct prod = Platform.getProduct();
			Bundle bund = prod.getDefiningBundle();
			String version = "" + new Date(bund.getLastModified());

			try
			{
				// ok, now write to the file
				DebriefEclipseXMLReaderWriter.exportThis(this, os, version);

				// ok, lastly indicate that the save worked (if it did!)
				_plotIsDirty = false;
				firePropertyChange(PROP_DIRTY);
			}
			catch (Exception e)
			{
				DebriefPlugin.logError(Status.ERROR, "Error exporting plot file", e);
			}

		}
		else
		{
			DebriefPlugin.logError(Status.ERROR,
					"Unable to identify source file for plot", null);
		}

	}

	public void doSaveAs()
	{
		doSaveAs("Save as");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed()
	{
		return true;
	}

	/**
	 * method called when a helper object has completed a plot-load operation
	 * 
	 * @param source
	 */
	public void loadingComplete(Object source)
	{
		// TODO
		// // ok, stop listening for dirty calls - since there will be so many and
		// // we
		// // don't want
		// // to start off with a dirty plot
		// startIgnoringDirtyCalls();
		//
		// DebriefPlugin.logError(Status.INFO, "File load received", null);
		//
		// // and update the time management bits
		// TimePeriod timePeriod = getPeriodFor(_myLayers);
		//
		// if (timePeriod != null)
		// {
		// _timeManager.setPeriod(this, timePeriod);
		//
		// // also give it a current DTG (if it doesn't have one)
		// if (_timeManager.getTime() == null)
		// _timeManager.setTime(this, timePeriod.getStartDTG(), false);
		// }
		//
		// // done - now we can process dirty calls again
		// stopIgnoringDirtyCalls();

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
			res = _canvas.getProjection();
		}
		// TODO
		// else if (adapter == TimeControllerOperationStore.class)
		// {
		// res = getTimeControllerOperations();
		// }
		// else if (adapter == LayerPainterManager.class)
		// {
		// res = _layerPainterManager;
		// }
		// else if (adapter == ControllablePeriod.class)
		// {
		// res = _myOperations;
		// }
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
		// else if (adapter == IGotoMarker.class)
		// {
		// return new IGotoMarker()
		// {
		// public void gotoMarker(IMarker marker)
		// {
		// String lineNum = marker.getAttribute(IMarker.LINE_NUMBER, "na");
		// if (lineNum != "na")
		// {
		// // right, convert to DTG
		// HiResDate tNow = new HiResDate(0, Long.parseLong(lineNum));
		// _timeManager.setTime(this, tNow, true);
		// }
		// }
		//
		// };
		// }

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
		// if (_myRelativeWrapper == null)
		// {
		// _myRelativeWrapper = new RelativeProjectionParent()
		// {
		//
		// public double getHeading()
		// {
		// double res1 = 0.0;
		// Watchable thePos = getFirstPosition(_trackDataProvider,
		// _timeManager);
		//
		// if (thePos != null)
		// {
		// // yup, get the centre point
		// res1 = thePos.getCourse();
		// }
		//
		// return res1;
		// }
		//
		// public WorldLocation getLocation()
		// {
		// MWC.GenericData.WorldLocation res1 = null;
		// Watchable thePos = getFirstPosition(_trackDataProvider,
		// _timeManager);
		//
		// if (thePos != null)
		// {
		// // yup, get the centre point
		// res1 = thePos.getBounds().getCentre();
		// }
		// return res1;
		// }
		//
		// private Watchable getFirstPosition(TrackDataProvider provider,
		// TimeManager manager)
		// {
		// Watchable res = null;
		//
		// // do we have a primary?
		// WatchableList priTrack = provider.getPrimaryTrack();
		// if (priTrack == null)
		// {
		// CorePlugin.logError(Status.ERROR,
		// "Can't do relative projection without primary track", null);
		// }
		// else
		// {
		// Watchable[] list = priTrack.getNearestTo(manager.getTime());
		// if (list != null)
		// if (list.length > 0)
		// res = list[0];
		// }
		//
		// return res;
		// }
		// };
		// }
		// res = _myRelativeWrapper;
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

	@Override
	public String getName()
	{
		return "PlotEditor";
	}

	@Override
	public boolean hasEditor()
	{
		return true;
	}

	@Override
	public EditorType getInfo()
	{
		return _myEditor;
	}

}
