package Debrief.Wrappers.Track;

import java.awt.Point;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import Debrief.Tools.Tote.Watchable;
import Debrief.Tools.Tote.WatchableList;
import Debrief.Wrappers.FixWrapper;
import Debrief.Wrappers.SensorContactWrapper;
import Debrief.Wrappers.SensorWrapper;
import MWC.GUI.CanvasType;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GenericData.Duration;
import MWC.GenericData.HiResDate;
import MWC.GenericData.TimePeriod;
import MWC.GenericData.WorldDistance;
import MWC.GenericData.WorldLocation;
import MWC.GenericData.WorldSpeed;
import MWC.GenericData.WorldVector;
import MWC.TacticalData.Fix;

/**
 * extension of track segment that represents a single TMA solution as a series
 * of fixes
 * 
 * @author Ian Mayo
 * 
 */
public class TMASegment extends TrackSegment
{

	/**
	 * class containing editable details of a track
	 */
	public final class TMASegmentInfo extends TrackSegmentInfo
	{

		/**
		 * constructor for this editor, takes the actual track as a parameter
		 * 
		 * @param data
		 *          track being edited
		 */
		public TMASegmentInfo(final TrackSegment data)
		{
			super(data);
		}

		private final static String SOLUTION = "Solution";
		private final static String OFFSET = "Offset";
		
		@Override
		public final PropertyDescriptor[] getPropertyDescriptors()
		{
			// start off with the parent
			PropertyDescriptor[] parent = super.getPropertyDescriptors();
			PropertyDescriptor[] mine = null;

			try
			{
				PropertyDescriptor[] res =
				{
						expertProp("Visible", "whether this layer is visible", FORMAT),
						expertProp("Course", "Course of this TMA Solution", SOLUTION),
						expertProp("Speed", "Speed of this TMA Solution", SOLUTION),
						expertProp("HostName",
								"Name of the track from which range/bearing measured", OFFSET),
						expertProp("OffsetRange", "Distance to start point on host track",
								OFFSET),
						expertProp("OffsetBearing",
								"Beraing from host track to start of this solution", OFFSET) };
				mine = res;
			}
			catch (final IntrospectionException e)
			{
				return super.getPropertyDescriptors();
			}

			// now combine them.
			PropertyDescriptor[] bigRes = new PropertyDescriptor[parent.length
					+ mine.length];
			System.arraycopy(parent, 0, bigRes, 0, parent.length);
			System.arraycopy(mine, 0, bigRes, parent.length, mine.length);

			return bigRes;
		}
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * steady course (Degs)
	 * 
	 */
	private double _courseDegs;

	/**
	 * steady speed
	 * 
	 */
	private WorldSpeed _speed;

	/**
	 * the feature we're based on
	 * 
	 */
	private WatchableList _referenceTrack;

	/**
	 * the offset we apply to the origin
	 * 
	 */
	private WorldVector _offset;

	/**
	 * name of the watchable list we're going to use as our origin
	 * 
	 * @return
	 */
	private String _hostName;

	private final Layers _theLayers;

	/**
	 * base constructor - sorts out the obvious
	 * 
	 * @param courseDegs
	 * @param speed
	 * @param offset
	 * @param theLayers
	 */
	public TMASegment(final double courseDegs, final WorldSpeed speed,
			final WorldVector offset, Layers theLayers)
	{
		_theLayers = theLayers;
		_courseDegs = courseDegs;
		_speed = speed;
		_offset = offset;
	}

	/**
	 * build up a solution from the supplied sensor data
	 * 
	 * @param observations
	 *          create a single position for the DTG of each solution
	 * @param offset
	 *          the range/brg from the host's position at the DTG of the first
	 *          observation
	 * @param speed
	 *          the initial target speed estimate
	 * @param courseDegs
	 *          the initial target course estimate
	 */
	public TMASegment(SensorContactWrapper[] observations, WorldVector offset,
			WorldSpeed speed, double courseDegs, Layers theLayers)
	{
		this(courseDegs, speed, offset, theLayers);

		// sort out the origin
		SensorContactWrapper scw = observations[0];
		_referenceTrack = scw.getSensor().getHost();

		// create the points
		createPointsFrom(observations);
	}

	/**
	 * build up a solution from the supplied sensor data
	 * 
	 * @param observations
	 *          create a single position for the DTG of each solution
	 * @param offset
	 *          the range/brg from the host's position at the DTG of the first
	 *          observation
	 * @param speed
	 *          the initial target speed estimate
	 * @param courseDegs
	 *          the initial target course estimate
	 */
	public TMASegment(SensorWrapper sw, WorldVector offset, WorldSpeed speed,
			double courseDegs, Layers theLayers)
	{
		this(courseDegs, speed, offset, theLayers);

		// sort out the origin
		_referenceTrack = sw.getHost();

		// create the points
		createPointsFrom(sw);
	}

	/**
	 * build up a solution from the supplied sensor data
	 * 
	 * @param observations
	 *          create a single position for the DTG of each solution
	 * @param offset
	 *          the range/brg from the host's position at the DTG of the first
	 *          observation
	 * @param speed
	 *          the initial target speed estimate
	 * @param courseDegs
	 *          the initial target course estimate
	 */
	public TMASegment(WatchableList origin, TimePeriod period, Duration interval,
			WorldVector offset, WorldSpeed speed, double courseDegs, Layers theLayers)
	{
		this(courseDegs, speed, offset, theLayers);

		// sort out the origin
		_referenceTrack = origin;

		// create the points
		createPointsFor(period, interval);
	}

	private Fix createFix(long thisT)
	{
		Fix fix = new Fix(new HiResDate(thisT), new WorldLocation(0, 0, 0),
				MWC.Algorithms.Conversions.Degs2Rads(_courseDegs), _speed
						.getValueIn(WorldSpeed.ft_sec) / 3);
		return fix;
	}

	/**
	 * create a fix at the specified dtg
	 * 
	 * @param thisS
	 * @return
	 */
	private FixWrapper createPointFor(SensorContactWrapper thisS)
	{
		FixWrapper newFix = new FixWrapper(createFix(thisS.getDTG().getDate()
				.getTime()));
		return newFix;
	}

	/**
	 * produce a series of regularly spaced fixes, during the specified period
	 * 
	 * @param period
	 * @param interval
	 */
	private void createPointsFor(TimePeriod period, Duration interval)
	{
		long intervalMillis = (long) interval.getValueIn(Duration.MILLISECONDS);
		for (long thisT = period.getStartDTG().getDate().getTime(); thisT <= period
				.getEndDTG().getDate().getTime(); thisT += intervalMillis)
		{
			FixWrapper nextFix = new FixWrapper(createFix(thisT));
			addFix(nextFix);
		}
	}

	private void createPointsFrom(SensorContactWrapper[] observations)
	{
		System.err.println("about to create:" + observations.length + " points");

		// better start looping
		for (int i = 0; i < observations.length; i++)
		{
			SensorContactWrapper thisS = observations[i];
			FixWrapper newFix = createPointFor(thisS);
			addFix(newFix);
		}
	}

	private void createPointsFrom(SensorWrapper sw)
	{
		Enumeration<Editable> obs = sw.elements();
		while (obs.hasMoreElements())
		{
			SensorContactWrapper thisS = (SensorContactWrapper) obs.nextElement();
			FixWrapper newFix = createPointFor(thisS);
			addFix(newFix);
		}
	}

	/**
	 * get the current course of this leg
	 * 
	 * @return course (degs)
	 */
	public double getCourse()
	{
		return _courseDegs;
	}

	public double getDetectionBearing()
	{
		return MWC.Algorithms.Conversions.Rads2Degs(_offset.getBearing());
	}

	public WorldDistance getDetectionRange()
	{
		return new WorldDistance(_offset.getRange(), WorldDistance.DEGS);
	}

	public String getHostName()
	{
		// just check we have some data
		if(_hostName == null)
			_hostName = _referenceTrack.getName();
		
		return _hostName;
	}

	@Override
	public EditorType getInfo()
	{
		return new TMASegmentInfo(this);
	}

	public WorldVector getOffset()
	{
		return _offset;
	}

	public double getOffsetBearing()
	{
		return MWC.Algorithms.Conversions.Rads2Degs(_offset.getBearing());
	}

	public WorldDistance getOffsetRange()
	{
		return new WorldDistance(_offset.getRange(), WorldDistance.DEGS);
	}

	/**
	 * get the start of this tma segment
	 * 
	 * @return
	 */
	@Override
	public WorldLocation getOrigin()
	{
		WorldLocation res = null;

		// have we sorted out our reference track yet?
		if (_referenceTrack == null)
		{
			setReferenceTrack();
		}

		if (_referenceTrack != null)
		{
			Watchable[] pts = _referenceTrack.getNearestTo(startDTG());
			if (pts.length > 0)
			{
				WorldLocation startPos = pts[0].getLocation();
				res = startPos.add(_offset);
			}
		}
		return res;
	}

	@Override
	public boolean getPlotRelative()
	{
		// always return true for TMA Segments
		return true;
	}

	public WatchableList getReferenceTrack()
	{
		return _referenceTrack;
	}

	/**
	 * the constant speed of this segment
	 * 
	 * @return the current speed
	 */
	public WorldSpeed getSpeed()
	{
		return _speed;
	}

	@Override
	public void paint(CanvasType dest)
	{
		Collection<Editable> items = getData();

		// ok - draw that line!
		Point lastPoint = null;
		Point lastButOne = null;
		WorldLocation tmaLastLoc = null;
		long tmaLastDTG = 0;

		for (Iterator<Editable> iterator = items.iterator(); iterator.hasNext();)
		{
			FixWrapper thisF = (FixWrapper) iterator.next();

			long thisTime = thisF.getDateTimeGroup().getDate().getTime();

			// ok, is this our first location?
			if (tmaLastLoc == null)
			{
				tmaLastLoc = new WorldLocation(getOrigin());
			}
			else
			{
				// calculate a new vector
				long timeDelta = thisTime - tmaLastDTG;
				WorldVector thisVec = vectorFor(timeDelta, thisF.getSpeed(), thisF
						.getCourse());
				tmaLastLoc.addToMe(thisVec);
			}

			// dump the location into the fix
			thisF.setFixLocationSilent(new WorldLocation(tmaLastLoc));

			// cool, remember the time.
			tmaLastDTG = thisTime;

			Point thisPoint = dest.toScreen(thisF.getFixLocation());

			// do we have enough for a line?
			if (lastPoint != null)
			{
				// draw that line
				dest.drawLine(lastPoint.x, lastPoint.y, thisPoint.x, thisPoint.y);

				// are we at the start of the line?
				if (lastButOne == null)
				{
					drawMyStalk(dest, lastPoint, thisPoint, true);
				}
			}

			lastButOne = lastPoint;
			lastPoint = new Point(thisPoint);

			// also draw in a marker for this point
			dest.drawRect(lastPoint.x - 1, lastPoint.y - 1, 3, 3);
		}

		// lastly 'plot on' from the last points
		drawMyStalk(dest, lastPoint, lastButOne, false);

	}

	/**
	 * the current course
	 * 
	 * @param course
	 *          (degs)
	 */
	public void setCourse(double course)
	{
		_courseDegs = course;

		double crseRads = MWC.Algorithms.Conversions.Degs2Rads(course);
		Collection<Editable> data = getData();
		for (Iterator<Editable> iterator = data.iterator(); iterator.hasNext();)
		{
			FixWrapper fix = (FixWrapper) iterator.next();
			fix.getFix().setCourse(crseRads);
		}

		// ditch our temp vector, we've got to recalc it
		_vecTempLastDTG = -2;
	}

	public void setDetectionBearing(double detectionBearing)
	{
		_offset = new WorldVector(MWC.Algorithms.Conversions
				.Degs2Rads(detectionBearing), new WorldDistance(_offset.getRange(),
				WorldDistance.DEGS), null);
	}

	public void setDetectionRange(WorldDistance detectionRange)
	{
		_offset = new WorldVector(_offset.getBearing(), detectionRange, null);
	}

	/**
	 * temporarily store the hostname, until we've finished loading and we can
	 * sort it out for real.
	 * 
	 * @param hostName
	 */
	public void setHostName(final String hostName)
	{
		// better trim what we've recived
		String name = hostName.trim();
		
		// have we got meaningful data?
		if(name.length() > 0)
		{
			// right, see if we can find it
			if(_theLayers != null)
			{
				Layer tgt = _theLayers.findLayer(name);
				if(tgt != null)
				{
					// clear the reference item we're currently looking at
					_referenceTrack = null;

					// now remember the new name
					_hostName = hostName;
				}
			}
			
		}

	}

	public void setOffsetBearing(double offsetBearing)
	{
		_offset.setValues(MWC.Algorithms.Conversions.Degs2Rads(offsetBearing),
				_offset.getRange(), _offset.getDepth());
	}

	public void setOffsetRange(WorldDistance offsetRange)
	{
		_offset.setValues(_offset.getBearing(), offsetRange
				.getValueIn(WorldDistance.DEGS), _offset.getDepth());
	}

	/**
	 * find the reference track for this relative solution
	 * 
	 */
	private void setReferenceTrack()
	{
		_referenceTrack = (WatchableList) _theLayers.findLayer(_hostName);
	}

	/**
	 * set the constant speed of this segment
	 * 
	 * @param speed
	 *          the new speed
	 */
	public void setSpeed(WorldSpeed speed)
	{
		_speed = speed;

		double spdYps = speed.getValueIn(WorldSpeed.ft_sec) / 3;
		Collection<Editable> data = getData();
		for (Iterator<Editable> iterator = data.iterator(); iterator.hasNext();)
		{
			FixWrapper fix = (FixWrapper) iterator.next();
			fix.getFix().setSpeed(spdYps);
		}

		// ditch our temp vector, we've got to recalc it
		_vecTempLastDTG = -2;

	}

	@Override
	public void shift(WorldVector vector)
	{
		// really, we just need to add this vector to our orign
		WorldLocation tmpOrigin = new WorldLocation(getOrigin());
		tmpOrigin.addToMe(_offset);
		tmpOrigin.addToMe(vector);

		_offset = tmpOrigin.subtract(getOrigin());
	}

}