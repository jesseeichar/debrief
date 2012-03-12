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

import Debrief.ReaderWriter.FlatFile.FlatFileExporter;

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
	protected Double _speedOfSound = null;

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

	// and the sensor type
	private final String[][] sensor2Types = new String[][]
	{
	{ "Towed-LF", "TL" },
	{ "Towed-HF", "TH" },
	{ "HM-Bow", "HB" },
	{ "HM-Flank", "HF" },
	{ "HM-Intercept", "HI" } };

	// and the sensor type
	private final String[][] sensor1Types = new String[][]
	{
	{ "Towed Array", "T" },
	{ "Hull mounted array", "H" } };

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
		if (fileVersion.equals(FlatFileExporter.INITIAL_VERSION))
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

	@Override
	public boolean isPageComplete()
	{
		boolean done = true;

		if ((_filePath == null) || (_filePath.length() == 0)
				|| (_serialName == null) || (_serialName.length() == 0))
			done = false;

		if (_sensorType1 == null)
			done = false;

		// new or old?
		// do we want an aft depth?
		if (_fileVersion.equals(FlatFileExporter.UPDATED_VERSION))
		{
			if (_speedOfSound == null)
				done = false;

			if (_protMarking == null)
				done = false;

			if (_sensorType1 != null)
			{
				if (_sensor1Fwd == null)
					done = false;
				// is aft depth relevant?
				if (_sensorType1.startsWith("T"))
					if (_sensor1Aft == null)
						done = false;
			}
			// do we have a second sensor?
			if (_numSensors == 2)
			{
				// has it been declared?
				if (_sensorType2 != null)
				{
					if (_sensor2Fwd == null)
						done = false;
					// is aft depth relevant?
					if (_sensorType2.startsWith("T"))
						if (_sensor2Aft == null)
							done = false;
				}

			}
		}

		return done;
	}

	/**
	 * sort out whether to show the aft editor
	 * 
	 * @param container
	 * @param editor
	 * @param sensorType
	 */
	private static void enableAftEditor(final Composite container,
			final StringFieldEditor editor, final String sensorType)
	{
		if (editor != null)
			editor.setEnabled(!sensorType.startsWith("H"), container);
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

		String filenameKey = "3Debrief.FlatFileOutput";
		String sensor1Key = "3Debrief.FlatFileSensorType1";
		String sensor2Key = "3Debrief.FlatFileSensorType2";
		String protMarkKey = "3Debrief.FlatFileProtMarking";
		String serialKey = "3Debrief.FlatFileSerialName";
		String sensor1fwdKey = "3Debrief.FlatFileSensor1fwd";
		String sensor1aftKey = "3Debrief.FlatFileSensor1aft";
		String sensor2fwdKey = "3Debrief.FlatFileSensor2fwd";
		String sensor2aftKey = "3Debrief.FlatFileSensor2aft";
		String speedOfSoundKey = "3Debrief.speedOfSoundKey";

		String title = "Output directory:";
		_fileFieldEditor = new DirectoryFieldEditor(filenameKey, title, container)
		{
			@Override
			protected void doLoad()
			{
				super.doLoad();
				fireValueChanged(FieldEditor.VALUE, null, this.getStringValue());
			}

			protected void fireValueChanged(String property, Object oldValue,
					Object newValue)
			{
				super.fireValueChanged(property, oldValue, newValue);

				// is this the value property?
				if (!property.equals(FieldEditor.VALUE))
					return;

				// tell the ui to update itself
				_filePath = (String) newValue;

				dialogChanged();
				this.store();

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

		// ok, we also need the sensor depth attribute
		if (_fileVersion.equals(FlatFileExporter.UPDATED_VERSION))
		{
			// ok, get the sensor1 depth
			StringFieldEditor speedOfSoundEditor = new StringFieldEditor(
					speedOfSoundKey, "Speed of Sound (m/sec):", container)
			{
				@Override
				protected void doLoad()
				{
					super.doLoad();
					fireValueChanged(FieldEditor.VALUE, null, this.getStringValue());
				}

				protected void fireValueChanged(String property, Object oldValue,
						Object newValue)
				{
					super.fireValueChanged(property, oldValue, newValue);

					// is this the value property?
					if (!property.equals(FieldEditor.VALUE))
						return;

					// is this the value property?
					if (isNumber(newValue))
						_speedOfSound = Double.valueOf(newValue.toString());

					dialogChanged();
					// and remember the new value
					store();
				}

				@Override
				protected boolean doCheckState()
				{
					return _speedOfSound != null;
				}
			};
			speedOfSoundEditor.setEmptyStringAllowed(false);
			speedOfSoundEditor.setPreferenceStore(getPreferenceStore());
			speedOfSoundEditor.setPage(this);
			speedOfSoundEditor
					.setErrorMessage("A value for speed of sound must be supplied");
			speedOfSoundEditor.setStringValue("");
			speedOfSoundEditor.load();
			if (speedOfSoundEditor.getStringValue() != null)
				if (isNumber(speedOfSoundEditor.getStringValue()))
					_speedOfSound = Double.valueOf(speedOfSoundEditor.getStringValue());

			@SuppressWarnings("unused")
			Label lbl3 = new Label(container, SWT.None);

		}

		// sort out the correct selection lists
		String[][] sensorTypes;
		if (_fileVersion.equals(FlatFileExporter.INITIAL_VERSION))
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

				// is this the value property?
				if (!property.equals(FieldEditor.VALUE))
					return;

				_sensorType1 = (String) newValue;
				enableAftEditor(container, _sensor1AftEditor, _sensorType1);
				dialogChanged();

				// remember the value
				this.store();

			}

		};
		_sensor1TypeEditor.setPreferenceStore(getPreferenceStore());
		_sensor1TypeEditor.setPage(this);
		_sensor1TypeEditor.load();
		_sensorType1 = getPreferenceStore().getString(sensor1Key);

		@SuppressWarnings("unused")
		Label lbl = new Label(container, SWT.None);

		// ok, we also need the sensor depth attribute
		if (_fileVersion.equals(FlatFileExporter.UPDATED_VERSION))
		{

			// ok, get the sensor1 depth
			StringFieldEditor sensor1FwdEditor = new StringFieldEditor(sensor1fwdKey,
					"Sensor 1 fwd depth (m):", container)
			{
				@Override
				protected void doLoad()
				{
					super.doLoad();
					fireValueChanged(FieldEditor.VALUE, null, this.getStringValue());
				}

				protected void fireValueChanged(String property, Object oldValue,
						Object newValue)
				{
					super.fireValueChanged(property, oldValue, newValue);

					// is this the value property?
					if (!property.equals(FieldEditor.VALUE))
						return;

					if (isNumber(newValue))
						_sensor1Fwd = Double.valueOf(newValue.toString());
					else
						_sensor1Fwd = null;

					if (_sensor1AftEditor != null)
						_sensor1AftEditor.setEnabled(!_sensorType1.startsWith("H"),
								container);

					// we may not have a second editor = get checking
					// if (_sensor2AftEditor != null)
					// _sensor2AftEditor.setEnabled(!_sensorType2.startsWith("H"),
					// container);

					dialogChanged();

					// remember the value
					this.store();
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
			if (sensor1FwdEditor.getStringValue() != null)
				if (isNumber(sensor1FwdEditor.getStringValue()))
					_sensor1Fwd = Double.valueOf(sensor1FwdEditor.getStringValue());

			@SuppressWarnings("unused")
			Label lbl2 = new Label(container, SWT.None);

			// ok, get the sensor1 depth
			_sensor1AftEditor = new StringFieldEditor(sensor1aftKey,
					"Sensor 1 aft depth (m):", container)
			{
				@Override
				protected void doLoad()
				{
					super.doLoad();
					fireValueChanged(FieldEditor.VALUE, null, this.getStringValue());
				}

				protected void fireValueChanged(String property, Object oldValue,
						Object newValue)
				{
					super.fireValueChanged(property, oldValue, newValue);

					// is this the value property?
					if (!property.equals(FieldEditor.VALUE))
						return;

					// is this the value property?
					if (isNumber(newValue))
						_sensor1Aft = Double.valueOf(newValue.toString());
					else
						_sensor1Aft = null;
					dialogChanged();

					// remember this value
					this.store();
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
			enableAftEditor(container, _sensor1AftEditor, _sensorType1);
			if (_sensor1AftEditor.getStringValue() != null)
				if (isNumber(_sensor1AftEditor.getStringValue()))
					_sensor1Aft = Double.valueOf(_sensor1AftEditor.getStringValue());

			@SuppressWarnings("unused")
			Label lbl3b = new Label(container, SWT.None);

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

					// is this the value property?
					if (!property.equals(FieldEditor.VALUE))
						return;

					_sensorType2 = (String) newValue;

					enableAftEditor(container, _sensor2AftEditor, _sensorType2);
					dialogChanged();
					// remember this value
					this.store();

				}
			};
			_sensor2TypeEditor.setPreferenceStore(getPreferenceStore());
			_sensor2TypeEditor.setPage(this);
			_sensor2TypeEditor.load();
			_sensorType2 = getPreferenceStore().getString(sensor2Key);

			@SuppressWarnings("unused")
			Label lbl2 = new Label(container, SWT.None);

			// ok, we also need the sensor depth attribute
			if (_fileVersion.equals(FlatFileExporter.UPDATED_VERSION))
			{
				// ok, get the sensor1 depth
				StringFieldEditor sensor2FwdEditor = new StringFieldEditor(
						sensor2fwdKey, "Sensor 2 fwd depth (m):", container)
				{
					@Override
					protected void doLoad()
					{
						super.doLoad();
						fireValueChanged(FieldEditor.VALUE, null, this.getStringValue());
					}

					protected void fireValueChanged(String property, Object oldValue,
							Object newValue)
					{
						super.fireValueChanged(property, oldValue, newValue);

						// is this the value property?
						if (!property.equals(FieldEditor.VALUE))
							return;

						// is this the value property?
						if (isNumber(newValue))
						{
							_sensor2Fwd = Double.valueOf(newValue.toString());
							dialogChanged();
							// remember this value
							this.store();
						}
						else
						{
							_sensor2Fwd = null;
						}
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
				sensor2FwdEditor.load();
				if (sensor2FwdEditor.getStringValue() != null)
					if (isNumber(sensor2FwdEditor.getStringValue()))
						_sensor2Fwd = Double.valueOf(sensor2FwdEditor.getStringValue());

				@SuppressWarnings("unused")
				Label lbl3 = new Label(container, SWT.None);

				// ok, get the sensor1 depth
				_sensor2AftEditor = new StringFieldEditor(sensor2aftKey,
						"Sensor 2 aft depth (m):", container)
				{
					@Override
					protected void doLoad()
					{
						super.doLoad();
						fireValueChanged(FieldEditor.VALUE, null, this.getStringValue());
					}

					protected void fireValueChanged(String property, Object oldValue,
							Object newValue)
					{
						super.fireValueChanged(property, oldValue, newValue);

						// is this the value property?
						if (!property.equals(FieldEditor.VALUE))
							return;

						// is this the value property?
						if (isNumber(newValue))
						{
							_sensor2Aft = Double.valueOf(newValue.toString());
						}
						else
						{
							_sensor2Aft = null;
						}
						// remember this value
						this.store();

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
				_sensor2AftEditor.load();
				enableAftEditor(container, _sensor2AftEditor, _sensorType2);

				if (_sensor2AftEditor.getStringValue() != null)
					if (isNumber(_sensor2AftEditor.getStringValue()))
						_sensor2Aft = Double.valueOf(_sensor2AftEditor.getStringValue());

				@SuppressWarnings("unused")
				Label lbl4 = new Label(container, SWT.None);

			}
		}

		if (_fileVersion.equals(FlatFileExporter.UPDATED_VERSION))
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
					if (!property.equals(FieldEditor.VALUE))
						return;

					_protMarking = (String) newValue;
					dialogChanged();

					// remember this value
					this.store();
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
			_protMarking = _protMarkingEditor.getStringValue();

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
				if (!property.equals(FieldEditor.VALUE))
					return;

				_serialName = (String) newValue;
				dialogChanged();

				// remember this value
				this.store();
			}

			@Override
			protected boolean doCheckState()
			{
				return _serialName != null;
			}

		};
		_serialNameEditor.setPreferenceStore(getPreferenceStore());
		_serialNameEditor.setPage(this);
		_serialNameEditor.setEmptyStringAllowed(false);
		_serialNameEditor.setErrorMessage("The serial name must be supplied");
		_serialNameEditor.load();
		_serialName = _serialNameEditor.getStringValue();

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

		if (_fileVersion.equals(FlatFileExporter.UPDATED_VERSION))
		{
			if (_speedOfSound != null)
			{
				updateStatus("Speed of sound must be specified");
				return;
			}

			if (_sensor1Fwd == null)
			{
				updateStatus("Sensor 1 fwd depth must be specified");
				return;
			}
			if (_sensorType1.startsWith("T"))
				if (_sensor1Aft == null)
				{
					updateStatus("Sensor 1 aft depth must be specified");
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
				if (_sensor2Fwd == null)
				{
					updateStatus("Sensor 2 fwd depth must be specified");
					return;
				}
				if (_sensorType2.startsWith("T"))
					if (_sensor2Aft == null)
					{
						updateStatus("Sensor 2 aft depth must be specified");
						return;
					}
			}

		}

		updateStatus(null);
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
		String res = "0";
		if (_sensorType1 != null)
			res = _sensorType1;
		return res;
	}

	public Double getSensor1Fwd()
	{
		return _sensor1Fwd;
	}

	public Double getSensor1Aft()
	{
		return _sensor1Aft;
	}

	public Double getSensor2Fwd()
	{
		return _sensor2Fwd;
	}

	public Double getSensor2Aft()
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
		String res = "0";
		if (_sensorType2 != null)
			res = _sensorType2;
		return res;
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

	public String getSerialName()
	{
		return _serialName;
	}

	public Double getSpeedOfSound()
	{
		return _speedOfSound;
	}

}