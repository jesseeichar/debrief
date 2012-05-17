package Debrief.Wrappers;

import java.awt.Color;
import java.awt.Point;
import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.util.Enumeration;

import Debrief.Wrappers.Track.PlanningSegment;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Properties.PlanningLegCalcModelPropertyEditor;
import MWC.GUI.Properties.TimeFrequencyPropertyEditor;
import MWC.GenericData.Duration;
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
				{
						expertProp("Origin", "where this track starts", FORMAT),
						expertProp("StartDate", "the time this track starts", FORMAT),

						expertLongProp("LabelFrequency", "the label frequency",
								MWC.GUI.Properties.TimeFrequencyPropertyEditor.class),
						expertLongProp("SymbolFrequency", "the symbol frequency",
								MWC.GUI.Properties.TimeFrequencyPropertyEditor.class),
						expertLongProp("ResampleDataAt", "the data sample rate",
								MWC.GUI.Properties.TimeFrequencyPropertyEditor.class),
						expertLongProp("ArrowFrequency", "the direction marker frequency",
								MWC.GUI.Properties.TimeFrequencyPropertyEditor.class),

						expertProp("Color", "the track color", FORMAT),
						expertProp("SymbolColor", "the color of the symbol (when used)",
								FORMAT),
						expertProp("TrackFont", "the track label font", FORMAT),
						expertProp("NameVisible", "show the track label", VISIBILITY),
						expertProp("NameAtStart",
								"whether to show the track name at the start (or end)",
								VISIBILITY),

				};
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
		this.setColor(Color.red);
		
		// give us a neater set of intervals
		this.setSymbolFrequency(new HiResDate(0,TimeFrequencyPropertyEditor._5_MINS));
		this.setLabelFrequency(new HiResDate(0,TimeFrequencyPropertyEditor._15_MINS));
	}

	@Override
	public void findNearestHotSpotIn(Point cursorPos, WorldLocation cursorLoc,
			ComponentConstruct currentNearest, Layer parentLayer)
	{
		// initialise thisDist, since we're going to be over-writing it
		WorldDistance thisDist = new WorldDistance(0, WorldDistance.DEGS);

		Enumeration<Editable> numer = getSegments().elements();
		while (numer.hasMoreElements())
		{
			final PlanningSegment thisSeg = (PlanningSegment) numer.nextElement();
			if (thisSeg.getVisible())
			{
				// produce a location for the end
				FixWrapper endFix = (FixWrapper) thisSeg.last();
				if (endFix != null)
				{

					// how far away is it?
					thisDist = endFix.getLocation().rangeFrom(cursorLoc, thisDist);

					final WorldLocation fixLocation = new WorldLocation(
							endFix.getLocation())
					{
						private static final long serialVersionUID = 1L;

						@Override
						public void addToMe(WorldVector delta)
						{
							super.addToMe(delta);

							// so, what's the bearing back to the leg start?
							double newBearing = super.bearingFrom(thisSeg.first().getBounds()
									.getCentre());

							newBearing = MWC.Algorithms.Conversions.Rads2Degs(newBearing);

							// limit the bearing to the nearest 5 deg marker
							int m = ((int) newBearing / 10);
							newBearing = m * 10d;

							thisSeg.setCourse(newBearing);
						}
					};

					// try range
					currentNearest
							.checkMe(this, thisDist, null, parentLayer, fixLocation);
				}
			}
		}
	}

	@Override
	public void shift(WorldVector vector)
	{
		this.getOrigin().addToMe(vector);
		recalculate();
	}

	public HiResDate getStartDate()
	{
		return _startDate;
	}

	public void setStartDate(HiResDate startDate)
	{
		this._startDate = startDate;
		recalculate();
	}

	public WorldLocation getOrigin()
	{
		return _origin;
	}

	public void setOrigin(WorldLocation origin)
	{
		this._origin = origin;
		recalculate();
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
	public Enumeration<Editable> contiguousElements()
	{
		return this.getSegments().elements();
	}

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
				theCalc = new FromRangeTime();
				break;
			case PlanningLegCalcModelPropertyEditor.SPEED_TIME:
				theCalc = new FromSpeedTime();
				break;
			}

			theCalc.construct(seg, thisOrigin, thisDate);

			// did we generate anything?
			if (seg.size() > 0)
			{
				// ok, now update the date/location
				thisOrigin = seg.last().getBounds().getCentre();
				thisDate = seg.endDTG();
			}
		}

		// ok, sort out the symbol & label freq
		HiResDate symFreq = this.getSymbolFrequency();
		HiResDate labelFreq = this.getLabelFrequency();

		this.setSymbolFrequency(new HiResDate(0));
		this.setLabelFrequency(new HiResDate(0));

		// and restore them
		setSymbolFrequency(symFreq);
		setLabelFrequency(labelFreq);

	}

	private abstract static class PlanningCalc
	{
		void construct(PlanningSegment seg, WorldLocation origin, HiResDate date)
		{
			// check we have some data
			if (date == null || origin == null)
				return;

			double distPerMinute = getMinuteDelta(seg);
			double timeTravelled = getSecsTravelled(seg);

			// ditch the existing items
			seg.removeAllElements();

			// ok build for this segment
			double courseDegs = seg.getCourse();
			double courseRads = MWC.Algorithms.Conversions.Degs2Rads(courseDegs);

			WorldVector vec = new WorldVector(courseRads, new WorldDistance(
					distPerMinute, WorldDistance.METRES), null);

			long timeMillis = date.getDate().getTime();
			for (long tNow = timeMillis; tNow <= timeMillis + timeTravelled * 1000; tNow += 60 * 1000)
			{
				HiResDate thisDtg = new HiResDate(tNow);

				// ok, do this fix
				Fix thisF = new Fix(thisDtg, origin, courseRads, seg.getSpeed()
						.getValueIn(WorldSpeed.ft_sec / 3));

				// override the depth
				thisF.getLocation().setDepth(
						seg.getDepth().getValueIn(WorldDistance.METRES));

				FixWrapper fw = new FixWrapper(thisF);
				seg.add(fw);
				
				// produce a new position
				origin = origin.add(vec);
			}
		}

		protected abstract double getSecsTravelled(PlanningSegment seg);

		abstract double getMinuteDelta(PlanningSegment seg);
	}

	private static class FromRangeSpeed extends PlanningCalc
	{

		@Override
		double getMinuteDelta(PlanningSegment seg)
		{
			// find out how far it travels
			double distPerMinute = seg.getSpeed().getValueIn(WorldSpeed.M_sec) * 60d;
			return distPerMinute;
		}

		@Override
		protected double getSecsTravelled(PlanningSegment seg)
		{
			// how long does it take to travel this distance?
			double secsTaken = seg.getDistance().getValueIn(WorldDistance.METRES)
					/ seg.getSpeed().getValueIn(WorldSpeed.M_sec);
			return secsTaken;
		}
	}

	private static class FromRangeTime extends PlanningCalc
	{

		@Override
		double getMinuteDelta(PlanningSegment seg)
		{
			// home long to travel along it (secs)
			double travelSecs = seg.getDuration().getValueIn(Duration.SECONDS);
			double metresPerSec = seg.getDistance().getValueIn(WorldDistance.METRES)
					/ travelSecs;

			double metresPerMin = metresPerSec * 60d;

			// update the speed, so it makes sense in the fix
			WorldSpeed speedMtrs = new WorldSpeed(metresPerSec, WorldSpeed.M_sec);
			WorldSpeed speedKTs = new WorldSpeed(
					speedMtrs.getValueIn(WorldSpeed.Kts), WorldSpeed.Kts);
			seg.setSpeedSilent(speedKTs);

			return metresPerMin;
		}

		@Override
		protected double getSecsTravelled(PlanningSegment seg)
		{
			return seg.getDuration().getValueIn(Duration.SECONDS);
		}
	}

	private static class FromSpeedTime extends PlanningCalc
	{

		@Override
		protected double getSecsTravelled(PlanningSegment seg)
		{
			return seg.getDuration().getValueIn(Duration.SECONDS);
		}

		@Override
		double getMinuteDelta(PlanningSegment seg)
		{
			// how far will we travel in time?
			double metresPerSec = seg.getSpeed().getValueIn(WorldSpeed.M_sec);
			double metresPerMin = metresPerSec * 60d;
			return metresPerMin;
		}
	}

}
