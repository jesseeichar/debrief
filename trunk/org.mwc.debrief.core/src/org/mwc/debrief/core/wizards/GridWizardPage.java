package org.mwc.debrief.core.wizards;

import java.beans.*;

import org.eclipse.jface.viewers.ISelection;

import MWC.GUI.Plottable;
import MWC.GUI.Chart.Painters.*;
import MWC.GenericData.WorldDistance;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (xml).
 */

public class GridWizardPage extends CorePlottableWizardPage
{

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public GridWizardPage(ISelection selection)
	{
		super(selection, "gridPage", "Add Grid to Plot",
				"This page adds a grid to your plot", "images/grid_wizard.gif");
	}

	protected Plottable createMe()
	{
		if (_plottable == null)
			_plottable = new GridPainter();

		return _plottable;
	}

	/**
	 * @return
	 */
	protected PropertyDescriptor[] getPropertyDescriptors()
	{
		PropertyDescriptor[] descriptors = {
				prop("Color", "the Color to draw the grid", getPlottable()),
				prop("PlotLabels", "whether to plot grid labels", getPlottable()),
				longProp("Delta", "the step size for the grid", getPlottable(),
						DefaultDistancePropertyEditor.class) };
		return descriptors;
	}

	public static class DefaultDistancePropertyEditor extends
			PropertyEditorSupport
	{

		protected WorldDistance _myDistance;

		private static WorldDistance[] _myDistances;

		private static String[] _myTags;

		public String[] getTags()
		{
			if (_myDistances == null)
			{
				_myDistances = new WorldDistance[] {
						new WorldDistance(500, WorldDistance.METRES),
						new WorldDistance(1, WorldDistance.KM),
						new WorldDistance(1, WorldDistance.MINUTES),
						new WorldDistance(5, WorldDistance.MINUTES),
						new WorldDistance(15, WorldDistance.MINUTES),
						new WorldDistance(30, WorldDistance.MINUTES),
						new WorldDistance(1, WorldDistance.DEGS), };

				// and now convert them to strings
				_myTags = new String[_myDistances.length];

				// cycle through converting as we go
				for (int i = 0; i < _myDistances.length; i++)
				{
					WorldDistance thisDist = _myDistances[i];
					_myTags[i] = thisDist.toString();

				}
			}

			return _myTags;
		}

		public void setValue(Object p1)
		{
			if (p1 instanceof WorldDistance)
			{
				_myDistance = (WorldDistance) p1;
			}
			if (p1 instanceof String)
			{
				for (int i = 0; i < _myTags.length; i++)
				{
					String thisTag = _myTags[i];
					if (p1.equals(thisTag))
					{
						_myDistance = _myDistances[i];
						break;
					}
				}
			}
		}

		public void setAsText(String val)
		{
			setValue(val);
		}

		public Object getValue()
		{
			return _myDistance;
		}

		public String getAsText()
		{
			return _myDistance.toString();
		}
	}

}
