/*
 * Desciption:
 * User: administrator
 * Date: Nov 11, 2001
 * Time: 12:29:16 PM
 */
package ASSET.GUI.SuperSearch.Observers;

import java.awt.Color;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Vector;

import ASSET.NetworkParticipant;
import ASSET.ScenarioType;
import ASSET.Models.Decision.TargetType;
import ASSET.Models.Detection.DetectionEvent;
import ASSET.Participants.Category;
import ASSET.Participants.Status;
import ASSET.Util.SupportTesting;
import Debrief.Wrappers.LabelWrapper;
import MWC.GUI.CanvasType;
import MWC.GUI.Editable;
import MWC.GenericData.WorldLocation;

public class RemoveDetectedObserver extends
		ASSET.Scenario.Observers.DetectionObserver
{
	/***************************************************************
	 * member variables
	 ***************************************************************/

	protected int _numDitched = 0;
	private Vector<LabelWrapper> _myDeadParts;

	private boolean _plotTheDead = true;

	/**
	 * ************************************************************ constructor
	 * *************************************************************
	 */
	public RemoveDetectedObserver(final TargetType watchVessel,
			final TargetType targetVessel, final String myName,
			final Integer detectionLevel, final boolean isActive)
	{
		super(watchVessel, targetVessel, myName, detectionLevel, isActive);

	}

	/***************************************************************
	 * member methods
	 ***************************************************************/

	/**
	 * valid detection happened, process it
	 */
	protected void validDetection(final DetectionEvent detection)
	{
		// let the parent do it's stuff
		super.validDetection(detection);

		// remove this target
		final int tgt = detection.getTarget();

		ditchHim(tgt);

		_numDitched++;

		// tell the attribute helper
		getAttributeHelper().newData(this.getScenario(), detection.getTime(),
				_numDitched);
	}

	private void ditchHim(final int tgt)
	{
		NetworkParticipant thisPart = getScenario().getThisParticipant(tgt);
		if (thisPart == null)
			return;
		Status hisStat = thisPart.getStatus();
		if (hisStat == null)
			return;
		WorldLocation loc = hisStat.getLocation();
		Color hisColor = Category.getColorFor(thisPart.getCategory());
		LabelWrapper lw = new LabelWrapper(thisPart.getName(), loc, hisColor);
		lw.setSymbolType("Reference Position");

		if (_myDeadParts == null)
			_myDeadParts = new Vector<LabelWrapper>(0, 1);

		_myDeadParts.add(lw);

		getScenario().removeParticipant(tgt);

	}

	public boolean isPlotTheDead()
	{
		return _plotTheDead;
	}

	public void setPlotTheDead(boolean plotTheDead)
	{
		_plotTheDead = plotTheDead;
	}

	@Override
	public void paint(CanvasType dest)
	{
		if (_plotTheDead)
		{
			if (_myDeadParts != null)
			{
				Object[] labels = _myDeadParts.toArray();
				for (int i = 0; i < labels.length; i++)
				{
					LabelWrapper labelWrapper = (LabelWrapper) labels[i];
					labelWrapper.paint(dest);
				}
			}
		}
	}

	
	
	@Override
	protected void performSetupProcessing(ScenarioType scenario)
	{
		super.performSetupProcessing(scenario);
		
		// chuck in the reset operation, so we're ready for this run
		resetMe();
	}

	public void performCloseProcessing(ScenarioType scenario)
	{
		super.performCloseProcessing(scenario);
	}

	private void resetMe()
	{
		_numDitched = 0;
		if (_myDeadParts != null)
			_myDeadParts.removeAllElements();
	}

	@Override
	public void restart(ScenarioType scenario)
	{
		super.restart(scenario);

		resetMe();
	}

	/***************************************************************
	 * plottable properties
	 ***************************************************************/
	/**
	 * whether there is any edit information for this item this is a convenience
	 * function to save creating the EditorType data first
	 * 
	 * @return yes/no
	 */
	public boolean hasEditor()
	{
		return true;
	}

	/**
	 * get the editor for this item
	 * 
	 * @return the BeanInfo data for this editable object
	 */
	public Editable.EditorType getInfo()
	{
		if (_myEditor == null)
			_myEditor = new RemoverInfo(this, getName());

		return _myEditor;
	}

	// ////////////////////////////////////////////////////
	// bean info for this class
	// ///////////////////////////////////////////////////
	public class RemoverInfo extends Editable.EditorType
	{

		public RemoverInfo(final RemoveDetectedObserver data, final String name)
		{
			super(data, name, "");
		}

		public String getName()
		{
			return RemoveDetectedObserver.this.getName();
		}

		public PropertyDescriptor[] getPropertyDescriptors()
		{
			try
			{
				final PropertyDescriptor[] res =
				{ prop("Name", "the name of this observer"),
						prop("PlotTheDead", "whether to plot locations of dead contacts"),
						prop("Active", "whether this listener is active"), };
				return res;
			}
			catch (IntrospectionException e)
			{
				System.out.println("::" + e.getMessage());
				return super.getPropertyDescriptors();
			}
		}
	}

	/**
	 * ************************************************************ a gui class to
	 * show progress of this monitor
	 * *************************************************************
	 */

	// ////////////////////////////////////////////////////////////////////////////////////////////////
	// testing for this class
	// ////////////////////////////////////////////////////////////////////////////////////////////////
	public static class RemDetectedTest extends SupportTesting.EditableTesting
	{
		static public final String TEST_ALL_TEST_TYPE = "UNIT";

		public RemDetectedTest(final String val)
		{
			super(val);
		}

		/**
		 * get an object which we can test
		 * 
		 * @return Editable object which we can check the properties for
		 */
		public Editable getEditable()
		{
			MWC.GUI.Editable ed = new RemoveDetectedObserver(null, null, "how many",
					new Integer(2), true);
			return ed;
		}
	}
}
