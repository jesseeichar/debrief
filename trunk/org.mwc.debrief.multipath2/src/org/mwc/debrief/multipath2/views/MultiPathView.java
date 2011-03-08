package org.mwc.debrief.multipath2.views;

import java.awt.BorderLayout;
import java.awt.Frame;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DefaultXYItemRenderer;

import MWC.GUI.JFreeChart.DateAxisEditor;
import MWC.GUI.JFreeChart.RelativeDateAxis;

/**

 */

public class MultiPathView extends ViewPart
{

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "org.mwc.debrief.MultiPath2";
	private MultiPathUI _ui;

	/**
	 * The constructor.
	 */
	public MultiPathView()
	{
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	public void createPartControl(Composite parent)
	{
		_ui = new MultiPathUI(parent, SWT.EMBEDDED);
		
		
		createPlot(_ui.getChartHolder());
		
//		Button doMe = new Button(parent, SWT.NONE);
//		doMe.setText("do me");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}
	
private void createPlot(Composite ui)
{ // create a date-formatting axis
	final DateAxis dateAxis = new RelativeDateAxis();
	dateAxis.setStandardTickUnits(DateAxisEditor
			.createStandardDateTickUnitsAsTickUnits());
	
	NumberAxis valAxis = new NumberAxis("Secs");
	DefaultXYItemRenderer theRenderer = new	DefaultXYItemRenderer();

	
	XYPlot _thePlot = new XYPlot(null, dateAxis, valAxis, theRenderer );
	JFreeChart _plotArea = new JFreeChart(_thePlot);
	ChartPanel _chartPanel = new ChartPanel(_plotArea);
	
	// now we need a Swing object to put our chart into
	Frame _plotControl = SWT_AWT.new_Frame(ui);
	
	_plotControl.add(_chartPanel, BorderLayout.CENTER);

}

	private void hookContextMenu()
	{
	}

	private void contributeToActionBars()
	{
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager)
	{

	}

	private void fillContextMenu(IMenuManager manager)
	{
	}

	private void fillLocalToolBar(IToolBarManager manager)
	{

	}

	private void makeActions()
	{
	
	}

	private void hookDoubleClickAction()
	{

	}

	private void showMessage(String message)
	{
		
		MessageDialog.openInformation(this.getSite().getShell(),
				"Multpath Analysis", message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus()
	{
		
	}
}