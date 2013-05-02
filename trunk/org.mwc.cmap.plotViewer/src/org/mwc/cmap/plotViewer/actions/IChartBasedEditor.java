package org.mwc.cmap.plotViewer.actions;

import org.mwc.cmap.plotViewer.editors.udig.InteractiveChart;

import MWC.GUI.Layer;
import MWC.GUI.Plottable;

/** interface for editors that contain a chart (so the Debrief buttons can apply to them)
 * 
 * @author Administrator
 *
 */
public interface IChartBasedEditor
{
	public InteractiveChart getChart();

	public void selectPlottable(Plottable shape, Layer layer);
}
