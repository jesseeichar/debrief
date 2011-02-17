package org.mwc.asset.scenariocontroller2.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.mwc.asset.scenariocontroller2.views.MultiScenarioView.UIDisplay;
import org.eclipse.swt.layout.GridData;

public class UISkeleton2 extends Composite implements UIDisplay
{

	/**
	 * Auto-generated method to display this org.eclipse.swt.widgets.Composite
	 * inside a new Shell.
	 */
	public static void showGUI()
	{
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		UISkeleton2 inst = new UISkeleton2(shell, SWT.NULL);
		Point size = inst.getSize();
		shell.setLayout(new FillLayout());
		shell.layout();
		if (size.x == 0 && size.y == 0)
		{
			inst.pack();
			shell.pack();
		}
		else
		{
			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
			shell.setSize(shellBounds.width, shellBounds.height);
		}
		shell.open();
		while (!shell.isDisposed())
		{
			if (!display.readAndDispatch())
				display.sleep();
		}
	}

	private Label scenarioVal;
	private Label controlVal;
	private Button btnGenerate;
	private Button btnRunAll;
	private Button btnInit;
	private Button btnStep;
	private Button btnPlay;
	private Composite multiTableHolder;
	private Label lblpending;
	private Font _timeFont;
	private Color _timeFore;
	private Color _timeBack;

	/**
	 * Create the composite.
	 * 
	 * @param parent
	 * @param style
	 */
	public UISkeleton2(Composite parent, int style)
	{
		super(parent, style);
		setLayout(new FormLayout());

		Composite topPanel = new Composite(this, SWT.NONE);
		FormData fd_topPanel = new FormData();
		fd_topPanel.right = new FormAttachment(100, -10);
		fd_topPanel.top = new FormAttachment(0, 3);
		fd_topPanel.left = new FormAttachment(0, 3);
		topPanel.setLayoutData(fd_topPanel);
		topPanel.setLayout(new RowLayout(SWT.HORIZONTAL));

		Composite filenameHolder = new Composite(topPanel, SWT.BORDER);
		filenameHolder.setLayout(new GridLayout(2, false));

		Label lblScenarioFile = new Label(filenameHolder, SWT.NONE);
		lblScenarioFile.setBounds(0, 0, 59, 14);
		lblScenarioFile.setText("Scenario file:");

		scenarioVal = new Label(filenameHolder, SWT.NONE);
		scenarioVal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,
				1, 1));
		scenarioVal.setBounds(0, 0, 59, 14);
		scenarioVal.setText("[pending]                     ");

		Label lblControlFile = new Label(filenameHolder, SWT.NONE);
		lblControlFile.setBounds(0, 0, 59, 14);
		lblControlFile.setText("Control file:");

		controlVal = new Label(filenameHolder, SWT.BORDER);
		controlVal.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,
				1, 1));
		controlVal.setBounds(0, 0, 59, 14);
		controlVal.setText("[pending]               ");

		Group grpManageScenarios = new Group(topPanel, SWT.NONE);
		grpManageScenarios.setText("Manage scenarios");
		grpManageScenarios.setLayout(new RowLayout(SWT.HORIZONTAL));

		Group grpAllScenarios = new Group(grpManageScenarios, SWT.NONE);
		grpAllScenarios.setText("All scenarios");
		grpAllScenarios.setLayout(new RowLayout(SWT.VERTICAL));

		btnGenerate = new Button(grpAllScenarios, SWT.NONE);
		btnGenerate.setText("Generate");

		btnRunAll = new Button(grpAllScenarios, SWT.NONE);
		btnRunAll.setText("Run all");

		Group grpSelectedScenario = new Group(grpManageScenarios, SWT.NONE);
		grpSelectedScenario.setText("Selected scenario");
		grpSelectedScenario.setLayout(new GridLayout(2, false));

		btnInit = new Button(grpSelectedScenario, SWT.NONE);
		btnInit.setText("Init");

		btnPlay = new Button(grpSelectedScenario, SWT.NONE);
		btnPlay.setText("Play");

		btnStep = new Button(grpSelectedScenario, SWT.NONE);
		btnStep.setText("Step");

		lblpending = new Label(grpSelectedScenario, SWT.NONE);
		_timeFont = SWTResourceManager.getFont("Courier New", 11, SWT.BOLD);
		_timeFore = SWTResourceManager.getColor(SWT.COLOR_GREEN);
		_timeBack = SWTResourceManager.getColor(105, 105, 105);
		lblpending.setFont(_timeFont);
		lblpending.setForeground(_timeFore);
		lblpending.setBackground(_timeBack);
		lblpending.setText("00:00:00");

		multiTableHolder = new Composite(this, SWT.BORDER);
		multiTableHolder.setLayout(new GridLayout(1, false));
		FormData fd_multiTableHolder = new FormData();
		fd_multiTableHolder.bottom = new FormAttachment(100, -10);
		fd_multiTableHolder.right = new FormAttachment(topPanel, 0, SWT.RIGHT);
		fd_multiTableHolder.top = new FormAttachment(topPanel, 6);
		fd_multiTableHolder.left = new FormAttachment(0, 10);
		multiTableHolder.setLayoutData(fd_multiTableHolder);

	}

	@Override
	public void dispose()
	{
		super.dispose();
		
		// and ditch our custom objects
		_timeFont.dispose();
		_timeFore.dispose();
		_timeBack.dispose();
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	@Override
	public Composite getMultiTableHolder()
	{
		return multiTableHolder;
	}

	@Override
	public void setScenario(String name)
	{
		scenarioVal.setText(name);
	}

	@Override
	public void setControl(String name)
	{
		controlVal.setText(name);
	}

	@Override
	public void addGenerateListener(SelectionListener listener)
	{
		btnGenerate.addSelectionListener(listener);
	}

	@Override
	public void addRunAllListener(SelectionListener listener)
	{
		btnRunAll.addSelectionListener(listener);
	}

	@Override
	public void setRunAllEnabled(boolean b)
	{
		btnRunAll.setEnabled(b);
	}

	@Override
	public void setGenerateEnabled(boolean b)
	{
		btnGenerate.setEnabled(b);
	}
	
	@Override
	public void setInitEnabled(boolean enabled)
	{
		btnInit.setEnabled(enabled);
	}

	@Override
	public void setStepEnabled(boolean enabled)
	{
		btnStep.setEnabled(enabled);
	}

	@Override
	public void setPlayEnabled(boolean enabled)
	{
		btnPlay.setEnabled(enabled);
	}

}
