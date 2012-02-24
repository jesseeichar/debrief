package org.mwc.debrief.core.operations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.core.interfaces.TimeControllerOperation;
import org.mwc.cmap.core.ui_support.wizards.SimplePageListWizard;
import org.mwc.debrief.core.DebriefPlugin;
import org.mwc.debrief.core.wizards.FlatFilenameWizardPage;
import Debrief.ReaderWriter.FlatFile.FlatFileExporter;
import Debrief.Wrappers.SensorWrapper;
import Debrief.Wrappers.TrackWrapper;
import MWC.GUI.BaseLayer;
import MWC.GUI.Editable;
import MWC.GenericData.TimePeriod;
import MWC.GenericData.WatchableList;

public class ExportToFlatFile extends TimeControllerOperation
{
	/**
	 * the flat-file format we're going to use
	 */
	private final String _fileVersion;
	


	/**
	 * default constructor - sticks to old format
	 * 
	 */
	public ExportToFlatFile()
	{
		this("Export to flat file (SAM Format)", FlatFileExporter.INITIAL_VERSION, false);
	}

	/**
	 * constructor requiring version & double-sensor status to be specified.
	 * 
	 * @param title
	 * @param fileVersion
	 * @param doubleSensor
	 */
	protected ExportToFlatFile(String title, String fileVersion,
			boolean doubleSensor)
	{
		super(title, true, !doubleSensor, doubleSensor, true);
		_fileVersion = fileVersion;
	}

	@Override
	public ImageDescriptor getDescriptor()
	{
		return DebriefPlugin.getImageDescriptor("icons/NewFile.png");
	}

	@Override
	public void executeExport(WatchableList primaryTrack,
			WatchableList[] secondaryTracks, TimePeriod period)
	{
		// sort out the destination file name
		String filePath = null;
		// sort out what type of file it is
		String sensor1Type = null;
		String sensor2Type = null;
		
		// don't forget the sensor depths
		Double s1fwd = null;
		Double s1aft = null;
		Double s2fwd = null;
		Double s2aft = null;
		
		// and speed of sound
		Double speedOfSoundMS = null;

		// the protective marking on the data
		String protMarking = null;

		// the name of the mission
		String serialName = null;

		// just check that at least one of the sensors has an offset
		TrackWrapper primary = (TrackWrapper) primaryTrack;
		BaseLayer sensors = primary.getSensors();
		Enumeration<Editable> sList = sensors.elements();
		boolean foundOne = false;
		int sensorCount = 0;
		while (sList.hasMoreElements())
		{
			SensorWrapper thisS = (SensorWrapper) sList.nextElement();

			// is it visible?
			if (thisS.getVisible())
				sensorCount++;

			// does it have an array offset
			if (thisS.getSensorOffset() != null)
				if (thisS.getSensorOffset().getValue() > 0)
					foundOne = true;
		}

		// right, special handling depending on run mode
		if (sensorCount == 0)
		{
			Shell shell = Display.getCurrent().getActiveShell();
			String title = "Export flat file";
			Image image = null;
			String message = "At least one primary sensor must be visible";
			int imageType = MessageDialog.ERROR; // check the user knows what
																						// he's
			// doing
			MessageDialog dl = new MessageDialog(shell, title, image, message,
					imageType, null, 0);
			dl.open();
			return;

		}
		if (_fileVersion.equals(FlatFileExporter.INITIAL_VERSION))
		{
			// ok, we can only have one sensor visible
			if (sensorCount > 1)
			{

				Shell shell = Display.getCurrent().getActiveShell();
				String title = "Export flat file";
				String message = "Only one of the sensors on the primary track must be visible";
				// pop it up
				MessageDialog.openError(shell, title, message);
				return;
			}
		}
		else
		{
			// ok, we can only have two sensors visible
			if (sensorCount > 2)
			{
				Shell shell = Display.getCurrent().getActiveShell();
				String title = "Export flat file";
				String message = "Only one or two of the sensors on the primary track must be visible";
				// pop it up
				MessageDialog.openError(shell, title, message);
				return;
			}
		}

		// and do the warning for no array offsets found

		if (!foundOne)
		{
			Shell shell = Display.getCurrent().getActiveShell();
			String title = "Export flat file";
			Image image = null;
			String message = "None of the sensor data has a towed array offset applied.\nDo you wish to continue?";
			String[] labels = new String[]
			{ "Yes", "No" };
			int index = 1;
			int imageType = MessageDialog.QUESTION; // check the user knows what he's
																							// doing
			MessageDialog dl = new MessageDialog(shell, title, image, message,
					imageType, labels, index);
			int res = dl.open();
			if (res == MessageDialog.CANCEL)
				return;
		}

		// prepare the export wizard
		SimplePageListWizard wizard = new SimplePageListWizard();
		wizard.addWizard(new FlatFilenameWizardPage(_fileVersion, sensorCount));
		WizardDialog dialog = new WizardDialog(Display.getCurrent()
				.getActiveShell(), wizard);
		dialog.create();
		dialog.open();
		// did it work?
		if (dialog.getReturnCode() == WizardDialog.OK)
		{
			FlatFilenameWizardPage exportPage = (FlatFilenameWizardPage) wizard
					.getPage(FlatFilenameWizardPage.PAGENAME);
			if (exportPage != null)
			{
				if (exportPage.isPageComplete())
				{
					filePath = exportPage.getFileName();
					sensor1Type = exportPage.getSensor1Type();
					sensor2Type = exportPage.getSensor2Type();
					protMarking = exportPage.getProtMarking();
					serialName = exportPage.getSerialName();
					
					s1fwd = exportPage.getSensor1Fwd();
					s1aft = exportPage.getSensor1Aft();
					s2fwd = exportPage.getSensor2Fwd();
					s2aft = exportPage.getSensor2Aft();
					speedOfSoundMS = exportPage.getSpeedOfSound();
				}
			}
			FlatFileExporter ff = new FlatFileExporter();
			String theData = ff.export(primaryTrack, secondaryTracks, period,
					sensor1Type, sensor2Type,s1fwd,s1aft,s2fwd,s2aft,
					_fileVersion, protMarking, serialName, speedOfSoundMS);

			// now write the data to file
			final String HOST_NAME = primaryTrack.getName();
			final String HOST_DATE = MWC.Utilities.TextFormatting.FormatRNDateTime
					.toMediumString(period.getStartDTG().getDate().getTime());
			final String fileName = filePath + File.separator + HOST_NAME + "_"
					+ HOST_DATE + "." + FlatFilenameWizardPage.FILE_SUFFIX;
			BufferedWriter out = null;
			try
			{
				out = new BufferedWriter(new FileWriter(fileName));
				out.write(theData);
				CorePlugin.showMessage("Export to SAM",
						"Tracks successfullly exported to SAM format");
			}
			catch (FileNotFoundException e)
			{
				DebriefPlugin.logError(Status.ERROR, "Unable to find output file:"
						+ fileName, e);
			}
			catch (IOException e)
			{
				DebriefPlugin.logError(Status.ERROR, "Whilst writing to output file:"
						+ fileName, e);
			}
			finally
			{
				try
				{
					if (out != null)
						out.close();
				}
				catch (IOException e)
				{
					DebriefPlugin.logError(Status.ERROR, "Whilst closing output file:"
							+ fileName, e);
				}

			}
		}
	}
}