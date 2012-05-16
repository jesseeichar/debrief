package Debrief.Wrappers;

import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.util.Enumeration;

import Debrief.Wrappers.Track.PlanningSegment;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Properties.PlanningLegCalcModelPropertyEditor;
import MWC.GenericData.HiResDate;
import MWC.GenericData.WorldDistance;
import MWC.GenericData.WorldLocation;
import MWC.GenericData.WorldSpeed;
import MWC.GenericData.WorldVector;
import MWC.TacticalData.Fix;

/**
 * class that represents a series of track legs, but a single start
 * time/location
 * 
 * @author ian
 * 
 */
public class CompositeTrackWrapper extends TrackWrapper
{

	/**
	 * class containing editable details of a track
	 */
	public final class CompositeTrackInfo extends Editable.EditorType implements
			Editable.DynamicDescriptors
	{

		/**
		 * constructor for this editor, takes the actual track as a parameter
		 * 
		 * @param data
		 *          track being edited
		 */
		public CompositeTrackInfo(final CompositeTrackWrapper data)
		{
			super(data, data.getName(), "");
		}

		@Override
		public final MethodDescriptor[] getMethodDescriptors()
		{
			// just add the reset color field first
			final Class<TrackWrapper> c = TrackWrapper.class;

			final MethodDescriptor[] mds =
			{ method(c, "exportThis", null, "Export Shape") };

			return mds;
		}

		@Override
		public final String getName()
		{
			return super.getName();
		}

		@Override
		public final PropertyDescriptor[] getPropertyDescriptors()
		{
			try
			{
				PropertyDescriptor[] res =
				{ expertProp("Origin", "where this track starts", FORMAT),
						expertProp("StartDate", "the time this track starts", FORMAT) };
				return res;
			}
			catch (final IntrospectionException e)
			{
				return super.getPropertyDescriptors();
			}
		}

	}

	private HiResDate _startDate;
	private WorldLocation _origin;

	public CompositeTrackWrapper(HiResDate startDate, WorldLocation centre)
	{
		_startDate = startDate;
		_origin = centre;
	}

	public HiResDate getStartDate()
	{
		return _startDate;
	}

	public void setStartDate(HiResDate startDate)
	{
		this._startDate = startDate;
	}

	public WorldLocation getOrigin()
	{
		return _origin;
	}

	public void setOrigin(WorldLocation origin)
	{
		this._origin = origin;
	}

	/**
	 * the editable details for this track
	 * 
	 * @return the details
	 */
	@Override
	public Editable.EditorType getInfo()
	{
		if (_myEditor == null)
			_myEditor = new CompositeTrackInfo(this);

		return _myEditor;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void add(Editable point)
	{
		if (point instanceof PlanningSegment)
		{
			// take a copy of the name, to stop it getting manmgled
			String name = point.getName();

			super.add(point);

			if (point.getName() != name)
				((PlanningSegment) point).setName(name);

			// better do a recalc, aswell
			recalculate();
		}
		else
		{
			throw new RuntimeException(
					"can't add this type to a composite track wrapper");
		}
	}

	@Override
	public void addFix(FixWrapper theFix)
	{
		throw new RuntimeException("can't add a fix to this composite track");
	}

	@Override
	public void append(Layer other)
	{
		throw new RuntimeException(
				"can't add another layer to this composite track");
	}

	public void recalculate()
	{
		Enumeration<Editable> numer = getSegments().elements();
		WorldLocation thisOrigin = getOrigin();
		HiResDate thisDate = getStartDate();
		while (numer.hasMoreElements())
		{
			Editable editable = (Editable) numer.nextElement();
			PlanningSegment seg = (PlanningSegment) editable;

			PlanningCalc theCalc = null;
			int model = seg.getCalculation();
			switch (model)
			{
			case PlanningLegCalcModelPropertyEditor.RANGE_SPEED:
				theCalc = new FromRangeSpeed();
				break;
			case PlanningLegCalcModelPropertyEditor.RANGE_TIME:
				theCalc = new FromRangeSpeed();
				break;
			case PlanningLegCalcModelPropertyEditor.SPEED_TIME:
				theCalc = new FromRangeSpeed();
				break;
			}

			theCalc.construct(seg, thisOrigin, thisDate);

			// ok, now update the date/location
			thisOrigin = seg.last().getBounds().getCentre();
			thisDate = seg.endDTG();

			System.out.println("doing recalc for:" + seg);
		}
	}

	private abstract static class PlanningCalc
	{
		void construct(PlanningSegment seg, WorldLocation origin, HiResDate date)
		{
			double distPerMinute = getMinuteDelta(seg);

			// ditch the existing items
			seg.removeAllElements();

			// ok build for this segment
			double secs = seg.getLength().getValueIn(WorldDistance.METRES)
					/ seg.getSpeed().getValueIn(WorldSpeed.M_sec);
			double courseRads = MWC.Algorithms.Conversions.Degs2Rads(seg.getCourse());

			WorldVector vec = new WorldVector(courseRads, new WorldDistance(
					distPerMinute, WorldDistance.METRES).getValueIn(WorldDistance.DEGS),
					0);

			long timeMillis = date.getDate().getTime();
			for (long tNow = timeMillis; tNow < timeMillis + secs * 1000; tNow += 60 * 1000)
			{
				System.err.println("new point at:" + tNow);
				HiResDate thisDtg = new HiResDate(tNow);

				// produce a new position
				origin = origin.add(vec);

				Fix thisF = new Fix(thisDtg, origin, courseRads, seg.getSpeed()
						.getValueIn(WorldSpeed.ft_sec / 3));
				FixWrapper fw = new FixWrapper(thisF);
				seg.add(fw);
			}
		}

		abstract double getMinuteDelta(PlanningSegment seg);
	}

	private static class FromRangeSpeed extends PlanningCalc
	{

		@Override
		double getMinuteDelta(PlanningSegment seg)
		{
			// find out how far it travels
			double distPerMinute = seg.getSpeed().getValueIn(WorldSpeed.M_sec) / 60d;
			return distPerMinute;
		}

	}
}
