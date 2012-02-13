package org.mwc.debrief.core.wizards;

import java.io.File;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.mwc.debrief.core.DebriefPlugin;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (xml).
 */

public class FlatFilenameWizardPage extends WizardPage
{

	public static final String PAGENAME = "FlatFileExport";

	protected String _filePath;
	/**
	 * the type of the first sensor
	 * 
	 */
	protected String _sensorType1;

	/**
	 * the type of the second sensor
	 * 
	 */
	protected String _sensorType2;

	/**
	 * the protective marking on the data
	 * 
	 */
	private String _protMarking;

	/**
	 * the name of when data was recorded
	 * 
	 */
	private String _serialName;

	private DirectoryFieldEditor _fileFieldEditor;

	private RadioGroupFieldEditor _sensor1TypeEditor;
	private RadioGroupFieldEditor _sensor2TypeEditor;
	private StringFieldEditor _protMarkingEditor;
	private StringFieldEditor _serialNameEditor;

	/**
	 * how many sensors to support
	 * 
	 */
	private final int _numSensors;

	private final String _fileVersion;

	protected Double _sensor1Fwd = null;
	protected Double _sensor1Aft = null;
	protected Double _sensor2Fwd = null;
	protected Double _sensor2Aft = null;

	private StringFieldEditor _sensor2AftEditor;

	private StringFieldEditor _sensor1AftEditor;

	public static final String FILE_SUFFIX = "txt";

	private static final String SINGLE_SENSOR = "This wizard allows you to indicate the type of sensor used, and the "
			+ "directory\nin which to place the output file. "
			+ "The output file will take the name of the primary \nfile with a "
			+ FILE_SUFFIX
			+ " suffix added. See online help for more details on the export format.";

	private static final String DOUBLE_SENSOR = "This wizard allows you to indicate the type of sensors used, "
			+ "and the "
			+ "directory in which to place the output file. "
			+ "The output file will take the name of the primary file with a "
			+ FILE_SUFFIX
			+ " suffix added. See online help for more details on the export format.";

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param fileVersion
	 * 
	 * @param pageName
	 */
	public FlatFilenameWizardPage(String fileVersion, int numSensors)
	{
		super(PAGENAME);
		_numSensors = numSensors;
		_fileVersion = fileVersion;
		setTitle("Export data to flat file");
		final String msgStr;
		if (fileVersion.equals("1.0"))
			msgStr = SINGLE_SENSOR;
		else
			msgStr = DOUBLE_SENSOR;
		setDescription(msgStr);

		super.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(
				"org.mwc.debrief.core", "images/newplot_wizard.gif"));
	}

	private static boolean isNumber(Object i)
	{
		try
		{
			Double.parseDouble(i.toString());
			return true;
		}
		catch (NumberFormatException nfe)
		{
			return false;
		}
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent)
	{

		final Composite container = new Composite(parent, SWT.NULL);
		final GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		String filenameKey = "Debrief.FlatFileOutput";
		String sensor1Key = "Debrief.FlatFileSensorType1";
		String sensor2Key = "Debrief.FlatFileSensorType2";
		String protMarkKey = "Debrief.FlatFileProtMarking";
		String serialKey = "Debrief.FlatFileSerialName";
		String sensor1fwdKey = "Debrief.FlatFileSensor1fwd";
		String sensor1aftKey = "Debrief.FlatFileSensor1aft";
		String sensor2fwdKey = "Debrief.FlatFileSensor2fwd";
		String sensor2aftKey = "Debrief.FlatFileSensor2aft";

		String title = "Output directory:";
		_fileFieldEditor = new DirectoryFieldEditor(filenameKey, title, container)
		{
			protected void fireValueChanged(String property, Object oldValue,
					Object newValue)
			{
				super.fireValueChanged(property, oldValue, newValue);

				if (property.equals("field_editor_value"))
				{
					// tell the ui to update itself
					_filePath = (String) newValue;
				}
				dialogChanged();

			}

			@Override
			protected boolean doCheckState()
			{
				return _filePath != null;
			}
		};
		_fileFieldEditor.fillIntoGrid(container, 3);
		_fileFieldEditor.setPreferenceStore(getPreferenceStore());
		_fileFieldEditor.setPage(this);
		_fileFieldEditor.setEmptyStringAllowed(false);
		_fileFieldEditor.load();

		// store the current editor value
		_filePath = _fileFieldEditor.getStringValue();

		// and the sensor type
		final String[][] sensor2Types = new String[][]
		{
		{ "Towed-LF", "TL" },
		{ "Towed-HF", "TH" },
		{ "HM-Bow", "HB" },
		{ "HM-Flank", "HF" },
		{ "HM-Intercept", "HI" } };

		// and the sensor type
		final String[][] sensor1Types = new String[][]
		{
		{ "Towed Array", "T" },
		{ "Hull mounted array", "H" } };

		// sort out the correct selection lists
		String[][] sensorTypes;
		if (_fileVersion.equals("1.0"))
		{
			sensorTypes = sensor1Types;
		}
		else
		{
			sensorTypes = sensor2Types;
		}

		// sort out the first sensor
		_sensor1TypeEditor = new RadioGroupFieldEditor(sensor1Key,
				"Sensor 1 type:", 2, sensorTypes, container)
		{
			protected void fireValueChanged(String property, Object oldValue,
					Object newValue)
			{
				super.fireValueChanged(property, oldValue, newValue);
				_sensorType1 = (String) newValue;
				char firstChar = _sensorType1.charAt(0);
				if (firstChar == 'H')
					_sensor1AftEditor.setEnabled(false, container);
				else
					_sensor1AftEditor.setEnabled(true, container);
				dialogChanged();
			}
		};
		_sensor1TypeEditor.setPreferenceStore(getPreferenceStore());
		_sensor1TypeEditor.setPage(this);
		_sensor1TypeEditor.load();
		_sensorType1 = sensorTypes[0][1];

		@SuppressWarnings("unused")
		Label lbl = new Label(container, SWT.None);

		// ok, we also need the sensor depth attribute
		if (!_fileVersion.equals("1.0"))
		{
			// ok, get the sensor1 depth
			StringFieldEditor sensor1FwdEditor = new StringFieldEditor(sensor1fwdKey,
					"Sensor 1 fwd depth:", container)
			{
				protected void fireValueChanged(String property, Object oldValue,
						Object newValue)
				{
					super.fireValueChanged(property, oldValue, newValue);

					// is this the value property?
					if (isNumber(newValue))
						_sensor1Fwd = Double.valueOf(newValue.toString());
					dialogChanged();
				}

				@Override
				protected boolean doCheckState()
				{
					return _sensor1Fwd != null;
				}
			};
			sensor1FwdEditor.setEmptyStringAllowed(false);
			sensor1FwdEditor.setPreferenceStore(getPreferenceStore());
			sensor1FwdEditor.setPage(this);
			sensor1FwdEditor
					.setErrorMessage("A value for Sensor 1 fwd depth must be supplied");
			sensor1FwdEditor.setStringValue("");
			sensor1FwdEditor.load();

			@SuppressWarnings("unused")
			Label lbl2 = new Label(container, SWT.None);

			// ok, get the sensor1 depth
			_sensor1AftEditor = new StringFieldEditor(sensor1aftKey,
					"Sensor 1 aft depth:", container)
			{
				protected void fireValueChanged(String property, Object oldValue,
						Object newValue)
				{
					super.fireValueChanged(property, oldValue, newValue);

					// is this the value property?
					if (isNumber(newValue))
						_sensor1Aft = Double.valueOf(newValue.toString());
					dialogChanged();
				}

				@Override
				protected boolean doCheckState()
				{
					return _sensor1Aft != null;
				}
			};
			_sensor1AftEditor.setEmptyStringAllowed(false);
			_sensor1AftEditor.setPreferenceStore(getPreferenceStore());
			_sensor1AftEditor.setPage(this);
			_sensor1AftEditor
					.setErrorMessage("A value for Sensor 1 aft depth must be supplied");
			_sensor1AftEditor.setStringValue("");
			_sensor1AftEditor.load();

			@SuppressWarnings("unused")
			Label lbl3 = new Label(container, SWT.None);

		}

		// and now the second sensor
		if (_numSensors > 1)
		{
			_sensor2TypeEditor = new RadioGroupFieldEditor(sensor2Key,
					"Sensor 2 type:", 2, sensorTypes, container)
			{
				protected void fireValueChanged(String property, Object oldValue,
						Object newValue)
				{
					super.fireValueChanged(property, oldValue, newValue);
					_sensorType2 = (String) newValue;
					char firstChar = _sensorType2.charAt(0);
					if (firstChar == 'H')
						_sensor2AftEditor.setEnabled(false, container);
					else
						_sensor2AftEditor.setEnabled(true, container);

					dialogChanged();
				}
			};
			_sensor2TypeEditor.setPreferenceStore(getPreferenceStore());
			_sensor2TypeEditor.setPage(this);
			_sensor2TypeEditor.load();
			_sensorType2 = sensorTypes[0][1];

			@SuppressWarnings("unused")
			Label lbl2 = new Label(container, SWT.None);

			// ok, we also need the sensor depth attribute
			if (!_fileVersion.equals("1.0"))
			{
				// ok, get the sensor1 depth
				StringFieldEditor sensor2FwdEditor = new StringFieldEditor(
						sensor2fwdKey, "Sensor 2 fwd depth:", container)
				{
					protected void fireValueChanged(String property, Object oldValue,
							Object newValue)
					{
						super.fireValueChanged(property, oldValue, newValue);

						// is this the value property?
						if (isNumber(newValue))
							_sensor2Fwd = Double.valueOf(newValue.toString());
						dialogChanged();
					}

					@Override
					protected boolean doCheckState()
					{
						return _sensor2Fwd != null;
					}
				};
				sensor2FwdEditor.setEmptyStringAllowed(false);
				sensor2FwdEditor.setPreferenceStore(getPreferenceStore());
				sensor2FwdEditor.setPage(this);
				sensor2FwdEditor
						.setErrorMessage("A value for Sensor 2 fwd depth must be supplied");
				sensor2FwdEditor.setStringValue("");
				sensor2FwdEditor.load();

				@SuppressWarnings("unused")
				Label lbl3 = new Label(container, SWT.None);

				// ok, get the sensor1 depth
				_sensor2AftEditor = new StringFieldEditor(sensor2aftKey,
						"Sensor 2 aft depth:", container)
				{
					protected void fireValueChanged(String property, Object oldValue,
							Object newValue)
					{
						super.fireValueChanged(property, oldValue, newValue);

						// is this the value property?
						if (isNumber(newValue))
							_sensor2Aft = Double.valueOf(newValue.toString());
						dialogChanged();
					}

					@Override
					protected boolean doCheckState()
					{
						return _sensor2Aft != null;
					}
				};
				_sensor2AftEditor.setEmptyStringAllowed(false);
				_sensor2AftEditor.setPreferenceStore(getPreferenceStore());
				_sensor2AftEditor.setPage(this);
				_sensor2AftEditor
						.setErrorMessage("A value for Sensor 2 aft depth must be supplied");
				_sensor2AftEditor.setStringValue("");
				_sensor2AftEditor.load();

				@SuppressWarnings("unused")
				Label lbl4 = new Label(container, SWT.None);

			}

		}

		if (!_fileVersion.equals("1.0"))
		{

			// we also want to specify the prot marking editor
			_protMarkingEditor = new StringFieldEditor(protMarkKey,
					"Protective Marking:", container)
			{
				protected void fireValueChanged(String property, Object oldValue,
						Object newValue)
				{
					super.fireValueChanged(property, oldValue, newValue);

					// is this the value property?
					if (FieldEditor.VALUE.equals(property))
						_protMarking = (String) newValue;
					dialogChanged();
				}

				@Override
				protected boolean doCheckState()
				{
					return _protMarking != null;
				}
			};
			_protMarkingEditor.setEmptyStringAllowed(false);
			_protMarkingEditor.setPreferenceStore(getPreferenceStore());
			_protMarkingEditor.setPage(this);
			_protMarkingEditor
					.setErrorMessage("A value for protective marking must be supplied");
			_protMarkingEditor.setStringValue("");
			_protMarkingEditor.load();

			_protMarking = "PENDING";

			@SuppressWarnings("unused")
			Label lbl3 = new Label(container, SWT.None);

		}

		// we also want to specify the serial nane (for single or double sensors)
		_serialNameEditor = new StringFieldEditor(serialKey, "Serial name:",
				container)
		{
			protected void fireValueChanged(String property, Object oldValue,
					Object newValue)
			{
				super.fireValueChanged(property, oldValue, newValue);

				// is this the value property?
				if (FieldEditor.VALUE.equals(property))
					_serialName = (String) newValue;
				dialogChanged();
			}

			@Override
			protected boolean doCheckState()
			{
				return _serialName != null;
			}

		};
		_serialNameEditor.setPreferenceStore(getPreferenceStore());
		_serialNameEditor.setPage(this);
		_serialNameEditor.setStringValue("");
		_serialNameEditor.setEmptyStringAllowed(false);
		_serialNameEditor
				.setErrorMessage("A value for serial name must be supplied");
		_serialNameEditor.load();
		_serialName = "PENDING";

		GridLayout urlLayout = (GridLayout) container.getLayout();
		urlLayout.numColumns = 3;

		container.layout();
		setControl(container);
	}

	private IPreferenceStore getPreferenceStore()
	{
		return DebriefPlugin.getDefault().getPreferenceStore();
	}

	/**
	 * Ensures that both text fields are set.
	 */

	void dialogChanged()
	{

		final String targetDir = getFileName();

		if ((targetDir == null) || (targetDir.length() == 0))
		{
			updateStatus("Target directory must be specified");
			return;
		}

		// just check it's a directory, not a file
		File testFile = new File(targetDir);
		if (!testFile.isDirectory())
		{
			updateStatus("Target must be a directory, not a file");
			return;
		}

		final String sensorType1 = getSensor1Type();
		if (sensorType1 == null)
		{
			updateStatus("Sensor 1 type must be selected");
			return;
		}

		if (_numSensors > 1)
		{
			final String sensorType2 = getSensor2Type();
			if (sensorType2 == null)
			{
				updateStatus("Sensor 2 type must be selected");
				return;
			}
			_sensor2TypeEditor.store();
		}

		// so, we've got valid data. better store them
		_sensor1TypeEditor.store();
		_fileFieldEditor.store();

		updateStatus(null);
	}

	public String getFileName()
	{
		return _filePath;
	}

	/**
	 * retrieve the selected sensor type
	 * 
	 * @return
	 */
	public String getSensor1Type()
	{
		return _sensorType1;
	}

	
	public double getSensor1Fwd()
	{
		return _sensor1Fwd;
	}
	public double getSensor1Aft()
	{
		return _sensor1Aft;
	}
	public double getSensor2Fwd()
	{
		return _sensor2Fwd;
	}
	public double getSensor2Aft()
	{
		return _sensor2Aft;
	}
	

	
	/**
	 * retrieve the selected sensor type
	 * 
	 * @return
	 */
	public String getSensor2Type()
	{
		return _sensorType2;
	}

	/**
	 * get the protective marking on the data
	 * 
	 * @return
	 */
	public String getProtMarking()
	{
		return _protMarking;
	}

	private void updateStatus(String message)
	{
		setErrorMessage(message);
		if (message == null)
		{
			this.setMessage("Press Finish to complete export",
					IMessageProvider.INFORMATION);
			setPageComplete(true);
		}
		else
			setPageComplete(false);
	}

	public String getSerialName()
	{
		return _serialName;
	}

}