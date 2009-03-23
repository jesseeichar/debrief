// Copyright MWC 1999, Debrief 3 Project
// Revision 1.1  1999-01-31 13:33:08+00  sm11td
// Initial revision
//

package Debrief.Wrappers;

import java.awt.Color;
import java.awt.Point;
import java.beans.IntrospectionException;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import Debrief.ReaderWriter.Replay.FormatTracks;
import Debrief.Tools.Tote.Watchable;
import Debrief.Tools.Tote.WatchableList;
import MWC.Algorithms.Conversions;
import MWC.GUI.CanvasType;
import MWC.GUI.DynamicPlottable;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.PlainWrapper;
import MWC.GUI.Plottable;
import MWC.GUI.Plottables;
import MWC.GUI.Canvas.MockCanvasType;
import MWC.GUI.Layer.ProvidesContiguousElements;
import MWC.GUI.Properties.TimeFrequencyPropertyEditor;
import MWC.GUI.Shapes.DraggableItem;
import MWC.GUI.Shapes.HasDraggableComponents;
import MWC.GenericData.HiResDate;
import MWC.GenericData.TimePeriod;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldDistance;
import MWC.GenericData.WorldLocation;
import MWC.GenericData.WorldVector;
import MWC.TacticalData.Fix;

// our old collections package used prior to JDK 1.2
// import com.sun.java.util.collections.*;

/**
 * the TrackWrapper maintains the GUI and data attributes of the whole track
 * iteself, but the responsibility for the fixes within the track are demoted to
 * the FixWrapper
 */
public final class TrackWrapper extends MWC.GUI.PlainWrapper implements
		Serializable, WatchableList, DynamicPlottable, MWC.GUI.Layer,
		DraggableItem, HasDraggableComponents, ProvidesContiguousElements
{

	// //////////////////////////////////////
	// member variables
	// //////////////////////////////////////

	/**
	 * keep track of versions - version id
	 */
	static final long serialVersionUID = 1;

	public static void main(final String[] args)
	{
		final testMe tm = new testMe("scrap");
		tm.testGettingTimes();
		tm.testGetItemsBetween_Second();
		tm.testMyParams();

	}

	/**
	 * whether to interpolate points in this track
	 */
	private boolean _interpolatePoints = false;

	/**
	 * the end of the track to plot the label
	 */
	private boolean _LabelAtStart = true;

	private HiResDate _lastLabelFrequency = new HiResDate(0,
			TimeFrequencyPropertyEditor.SHOW_ALL_FREQUENCY);

	private HiResDate _lastSymbolFrequency = new HiResDate(0,
			TimeFrequencyPropertyEditor.SHOW_ALL_FREQUENCY);

	/**
	 * the width of this track
	 */
	private int _lineWidth = 2;

	/**
	 * whether or not to link the Positions
	 */
	private boolean _linkPositions;

	/**
	 * our editable details
	 */
	transient private Editable.EditorType _myEditor = null;

	/**
	 * keep a list of points waiting to be plotted
	 * 
	 */
	transient int[] _myPts;

	/**
	 * the sensor tracks for this vessel
	 */
	private Vector<SensorWrapper> _mySensors = null;

	/**
	 * the TMA solutions for this vessel
	 */
	private Vector<TMAWrapper> _mySolutions = null;

	/**
	 * keep track of how far we are through our array of points
	 * 
	 */
	transient int _ptCtr = 0;

	/**
	 * whether or not to show the Positions
	 */
	private boolean _showPositions;

	/**
	 * the label describing this track
	 */
	private final MWC.GUI.Shapes.TextLabel _theLabel;

	/**
	 * the list of wrappers we hold
	 */
	private PlottableLayer _thePositions;

	/**
	 * the symbol to pass on to a snail plotter
	 */
	private MWC.GUI.Shapes.Symbols.PlainSymbol _theSnailShape;

	/**
	 * working ZERO location value, to reduce number of working values
	 */
	final private WorldLocation _zeroLocation = new WorldLocation(0, 0, 0);

	transient private FixWrapper finisher;

	transient private HiResDate lastDTG;

	transient private FixWrapper lastFix;

	// for getNearestTo
	transient private FixWrapper nearestFix;

	// //////////////////////////////////////
	// member functions
	// //////////////////////////////////////

	/**
	 * working parameters
	 */
	// for getFixesBetween
	transient private FixWrapper starter;

	transient private TimePeriod _myTimePeriod;

	transient private WorldArea _myWorldArea;

	// //////////////////////////////////////
	// constructors
	// //////////////////////////////////////
	/**
	 * Wrapper for a Track (a series of position fixes). It combines the data with
	 * the formatting details
	 */
	public TrackWrapper()
	{
		// declare our arrays
		_thePositions = new PlottableLayer();
		_thePositions.setName("Positions");

		_linkPositions = true;

		// start off with positions showing (although the default setting for a
		// fix
		// is to not show a symbol anyway). We need to make this "true" so that
		// when a fix position is set to visible it is not over-ridden by this
		// setting
		_showPositions = true;

		_theLabel = new MWC.GUI.Shapes.TextLabel(new WorldLocation(0, 0, 0), null);
		// set an initial location for the label
		_theLabel.setRelativeLocation(new Integer(
				MWC.GUI.Properties.LocationPropertyEditor.RIGHT));

		// initialise the symbol to use for plotting this track in snail mode
		_theSnailShape = MWC.GUI.Shapes.Symbols.SymbolFactory
				.createSymbol("Submarine");
	}

	/**
	 * add the indicated point to the track
	 * 
	 * @param point
	 *          the point to add
	 */
	public final void add(final MWC.GUI.Editable point)
	{
		boolean done = false;
		// see what type of object this is
		if (point instanceof FixWrapper)
		{
			final FixWrapper fw = (FixWrapper) point;
			fw.setTrackWrapper(this);
			addFix(fw);
			done = true;
		}
		// is this a sensor?
		else if (point instanceof SensorWrapper)
		{
			final SensorWrapper swr = (SensorWrapper) point;
			if (_mySensors == null)
			{
				_mySensors = new Vector<SensorWrapper>(0, 1);
			}
			// add to our list
			_mySensors.add(swr);

			// tell the sensor about us
			swr.setHost(this);

			// and the track name (if we're loading from REP it will already
			// know
			// the name, but if this data is being pasted in, it may start with
			// a different
			// parent track name - so override it here)
			swr.setTrackName(this.getName());

			// indicate success
			done = true;

		}
		// is this a TMA solution track?
		else if (point instanceof TMAWrapper)
		{
			final TMAWrapper twr = (TMAWrapper) point;
			if (_mySolutions == null)
			{
				_mySolutions = new Vector<TMAWrapper>(0, 1);
			}
			// add to our list
			_mySolutions.add(twr);

			// tell the sensor about us
			twr.setHost(this);

			// and the track name (if we're loading from REP it will already
			// know
			// the name, but if this data is being pasted in, it may start with
			// a different
			// parent track name - so override it here)
			twr.setTrackName(this.getName());

			// indicate success
			done = true;

		}

		if (!done)
		{
			MWC.GUI.Dialogs.DialogFactory.showMessage("Add point",
					"Sorry it is not possible to add:" + point.getName() + " to "
							+ this.getName());
		}
	}

	/**
	 * add the fix wrapper to the track
	 * 
	 * @param theFix
	 *          the Fix to be added
	 */
	public final void addFix(final FixWrapper theFix)
	{
		// add fix to the track
		_thePositions.add(theFix);

		// and add the fix wrapper to our data list
//		_theTrack.addFix(theFix.getFix());


		// and extend the start/end DTGs
		if(_myTimePeriod == null)
			_myTimePeriod = new TimePeriod.BaseTimePeriod(theFix.getDateTimeGroup(), theFix.getDateTimeGroup());
		else
			_myTimePeriod.extend(theFix.getDateTimeGroup());
		
		if(_myWorldArea == null)
			_myWorldArea = new WorldArea(theFix.getLocation(), theFix.getLocation());
		else
			_myWorldArea.extend(theFix.getLocation());
	}

	/**
	 * append this other layer to ourselves (although we don't really bother with
	 * it)
	 * 
	 * @param other
	 *          the layer to add to ourselves
	 */
	public final void append(final Layer other)
	{
		final java.util.Enumeration<Editable> iter = other.elements();
		while (iter.hasMoreElements())
		{
			add(iter.nextElement());
		}
	}

	/**
	 * instruct this object to clear itself out, ready for ditching
	 */
	public final void closeMe()
	{
		// do the parent
		super.closeMe();

		// and my objects
		// first ask them to close themselves
		final Enumeration<Editable> it = _thePositions.elements();
		while (it.hasMoreElements())
		{
			final Object val = it.nextElement();
			if (val instanceof PlainWrapper)
			{
				final PlainWrapper pw = (PlainWrapper) val;
				pw.closeMe();
			}
		}

		// now ditch them
		_thePositions.removeAllElements();
		_thePositions = null;

		// and my objects
		// first ask the sensors to close themselves
		if (_mySensors != null)
		{
			final Iterator<SensorWrapper> it2 = _mySensors.iterator();
			while (it2.hasNext())
			{
				final Object val = it2.next();
				if (val instanceof PlainWrapper)
				{
					final PlainWrapper pw = (PlainWrapper) val;
					pw.closeMe();
				}
			}
			// now ditch them
			_mySensors.clear();
		}

		// now ask the solutions to close themselves
		if (_mySolutions != null)
		{
			final Iterator<TMAWrapper> it2 = _mySolutions.iterator();
			while (it2.hasNext())
			{
				final Object val = it2.next();
				if (val instanceof PlainWrapper)
				{
					final PlainWrapper pw = (PlainWrapper) val;
					pw.closeMe();
				}
			}
			// now ditch them
			_mySolutions.clear();
		}

		// and our utility objects
		finisher = null;
		lastFix = null;
		nearestFix = null;
		starter = null;

		// and our editor
		_myEditor = null;
	}

	/**
	 * return our tiered data as a single series of elements
	 * 
	 * @return
	 */
	public final Enumeration<Editable> contiguousElements()
	{
		final Vector<Editable> res = new Vector<Editable>(0, 1);

		if (_mySensors != null)
		{
			final Enumeration<SensorWrapper> iter = _mySensors.elements();
			while (iter.hasMoreElements())
			{
				res.add(iter.nextElement());
			}
		}

		if (_mySolutions != null)
		{
			final Enumeration<TMAWrapper> iter = _mySolutions.elements();
			while (iter.hasMoreElements())
			{
				res.add(iter.nextElement());
			}
		}

		if (_thePositions != null)
		{
			final Enumeration<Editable> iter = _thePositions.elements();
			while (iter.hasMoreElements())
			{
				res.add(iter.nextElement());
			}
		}

		return res.elements();
	}

	/**
	 * get an enumeration of the points in this track
	 * 
	 * @return the points in this track
	 */
	public final Enumeration<Editable> elements()
	{
		final TreeSet<Editable> res = new TreeSet<Editable>();

		if (_mySensors != null)
		{
			final Enumeration<SensorWrapper> iter = _mySensors.elements();
			while (iter.hasMoreElements())
			{
				res.add(iter.nextElement());
			}
		}

		if (_mySolutions != null)
		{

			final Enumeration<TMAWrapper> iter = _mySolutions.elements();
			while (iter.hasMoreElements())
			{
				res.add(iter.nextElement());
			}
		}

		if (res == null)
		{
			final Enumeration<Editable> iter = _thePositions.elements();
			while (iter.hasMoreElements())
			{
				res.add(iter.nextElement());
			}
		} else
		{
			// ok, we want to wrap our fast-data as a set of plottables
			res.add(_thePositions);
		}

		return new IteratorWrapper(res.iterator());
	}

	/**
	 * export this track to REPLAY file
	 */
	public final void exportShape()
	{
		// call the method in PlainWrapper
		this.exportThis();
	}

	/**
	 * filter the list to the specified time period, then inform any listeners
	 * (such as the time stepper)
	 * 
	 * @param start
	 *          the start dtg of the period
	 * @param end
	 *          the end dtg of the period
	 */
	public final void filterListTo(final HiResDate start, final HiResDate end)
	{
		final Enumeration<Editable> fixWrappers = _thePositions.elements();
		while (fixWrappers.hasMoreElements())
		{
			final FixWrapper fw = (FixWrapper) fixWrappers.nextElement();
			final HiResDate dtg = fw.getTime();
			if ((dtg.greaterThanOrEqualTo(start)) && (dtg.lessThanOrEqualTo(end)))
			{
				fw.setVisible(true);
			} else
			{
				fw.setVisible(false);
			}
		}

		// now do the same for our sensor data
		if (_mySensors != null)
		{
			final Enumeration<SensorWrapper> iter = _mySensors.elements();
			while (iter.hasMoreElements())
			{
				final WatchableList sw = iter.nextElement();
				sw.filterListTo(start, end);
			} // through the sensors
		} // whether we have any sensors

		// and our solution data
		if (_mySolutions != null)
		{
			final Enumeration<TMAWrapper> iter = _mySolutions.elements();
			while (iter.hasMoreElements())
			{
				final WatchableList sw = iter.nextElement();
				sw.filterListTo(start, end);
			} // through the sensors
		} // whether we have any sensors

		// do we have any property listeners?
		if (getSupport() != null)
		{
			final Debrief.GUI.Tote.StepControl.somePeriod newPeriod = new Debrief.GUI.Tote.StepControl.somePeriod(
					start, end);
			getSupport().firePropertyChange(WatchableList.FILTERED_PROPERTY, null,
					newPeriod);
		}
	}

	public void findNearestHotSpotIn(Point cursorPos, WorldLocation cursorLoc,
			ComponentConstruct currentNearest, Layer parentLayer)
	{
		// initialise thisDist, since we're going to be over-writing it
		WorldDistance thisDist = new WorldDistance(0, WorldDistance.DEGS);

		// cycle through the fixes
		final Enumeration<Editable> fixes = _thePositions.elements();
		while (fixes.hasMoreElements())
		{
			final FixWrapper thisF = (FixWrapper) fixes.nextElement();

			// only check it if it's visible
			if (thisF.getVisible())
			{

				// how far away is it?
				thisDist = thisF.getLocation().rangeFrom(cursorLoc, thisDist);

				final WorldLocation fixLocation = new WorldLocation(thisF.getLocation())
				{
					private static final long serialVersionUID = 1L;

					public void addToMe(WorldVector delta)
					{
						super.addToMe(delta);
						thisF.setFixLocation(this);
					}
				};

				// try range
				currentNearest.checkMe(this, thisDist, null, parentLayer, fixLocation);
			}
		}

	}

	public void findNearestHotSpotIn(Point cursorPos, WorldLocation cursorLoc,
			LocationConstruct currentNearest, Layer parentLayer)
	{
		// initialise thisDist, since we're going to be over-writing it
		WorldDistance thisDist = new WorldDistance(0, WorldDistance.DEGS);

		// cycle through the fixes
		final Enumeration<Editable> fixes = _thePositions.elements();
		while (fixes.hasMoreElements())
		{
			final FixWrapper thisF = (FixWrapper) fixes.nextElement();

			if (thisF.getVisible())
			{
				// how far away is it?
				thisDist = thisF.getLocation().rangeFrom(cursorLoc, thisDist);

				// is it closer?
				currentNearest.checkMe(this, thisDist, null, parentLayer);
			}
		}
	}

	/**
	 * what geographic area is covered by this track?
	 * 
	 * @return get the outer bounds of the area
	 */
	public final WorldArea getBounds()
	{
		// we no longer just return the bounds of the track, because a portion
		// of the track may have been made invisible.

		// instead, we will pass through the full dataset and find the outer
		// bounds
		// of the visible area
		WorldArea res = null;

		if (!getVisible())
		{
			// hey, we're invisible, return null
		} else
		{
			final Enumeration<Editable> it = this._thePositions.elements();
			while (it.hasMoreElements())
			{
				final FixWrapper fw = (FixWrapper) it.nextElement();

				// is this point visible?
				if (fw.getVisible())
				{

					// has our data been initialised?
					if (res == null)
					{
						// no, initialise it
						res = new WorldArea(fw.getLocation(), fw.getLocation());
					} else
					{
						// yes, extend to include the new area
						res.extend(fw.getLocation());
					}
				}
			}

			// also extend to include our sensor data
			if (_mySensors != null)
			{
				final Enumeration<SensorWrapper> iter = _mySensors.elements();
				while (iter.hasMoreElements())
				{
					final PlainWrapper sw = iter.nextElement();
					final WorldArea theseBounds = sw.getBounds();
					if (theseBounds != null)
					{
						if (res == null)
							res = new WorldArea(theseBounds);
						else
							res.extend(sw.getBounds());
					}
				} // step through the sensors
			} // whether we have any sensors

			// and our solution data
			if (_mySolutions != null)
			{
				final Enumeration<TMAWrapper> iter = _mySolutions.elements();
				while (iter.hasMoreElements())
				{
					final PlainWrapper sw = iter.nextElement();
					final WorldArea theseBounds = sw.getBounds();
					if (theseBounds != null)
					{
						if (res == null)
							res = new WorldArea(theseBounds);
						else
							res.extend(sw.getBounds());
					}
				} // step through the sensors
			} // whether we have any sensors

		} // whether we're visible

		return res;
	}

	public final TrackWrapper getDragTrack()
	{
		return this;
	}

	/**
	 * the time of the last fix
	 * 
	 * @return the DTG
	 */
	public final HiResDate getEndDTG()
	{
		HiResDate res = null;
		if(_myTimePeriod != null)
			res = _myTimePeriod.getEndDTG();
		return res;
	}

	/**
	 * the editable details for this track
	 * 
	 * @return the details
	 */
	public final Editable.EditorType getInfo()
	{
		if (_myEditor == null)
			_myEditor = new trackInfo(this);

		return _myEditor;
	}

	/**
	 * create a new, interpolated point between the two supplied
	 * 
	 * @param previous
	 *          the previous point
	 * @param next
	 *          the next point
	 * @return and interpolated point
	 */
	private final FixWrapper getInterpolatedFix(final FixWrapper previous,
			final FixWrapper next, HiResDate requestedDTG)
	{
		FixWrapper res = null;

		// do we have a start point
		if (previous == null)
			res = next;

		// hmm, or do we have an end point?
		if (next == null)
			res = previous;

		// did we find it?
		if (res == null)
		{
			res = FixWrapper.interpolateFix(previous, next, requestedDTG);
		}

		return res;
	}

	public final boolean getInterpolatePoints()
	{
		return _interpolatePoints;
	}

	/**
	 * get the set of fixes contained within this time period (inclusive of both
	 * end values)
	 * 
	 * @param start
	 *          start DTG
	 * @param end
	 *          end DTG
	 * @return series of fixes
	 */
	public final Collection<Editable> getItemsBetween(final HiResDate start,
			final HiResDate end)
	{
		//
		SortedSet<Editable> set = null;

		// does our track contain any data at all
		if (_thePositions.size() > 0)
		{

			// see if we have _any_ points in range
			if ((getStartDTG().greaterThan(end)) || (getEndDTG().lessThan(start)))
			{
				// don't bother with it.
			} else
			{

				// SPECIAL CASE! If we've been asked to show interpolated data
				// points,
				// then
				// we should produce a series of items between the indicated
				// times. How
				// about 1 minute resolution?
				if (getInterpolatePoints())
				{
					final long ourInterval = 1000 * 60; // one minute
					set = new TreeSet<Editable>();
					for (long newTime = start.getDate().getTime(); newTime < end
							.getDate().getTime(); newTime += ourInterval)
					{
						final HiResDate newD = new HiResDate(newTime);
						final Watchable[] nearestOnes = getNearestTo(newD);
						if (nearestOnes.length > 0)
						{
							final FixWrapper nearest = (FixWrapper) nearestOnes[0];
							set.add(nearest);
						}
					}
				} else
				{
					// bugger that - get the real data

					// have a go..
					if (starter == null)
					{
						starter = new FixWrapper(new Fix((start), _zeroLocation, 0.0, 0.0));
					} else
					{
						starter.getFix().setTime(new HiResDate(0, start.getMicros() - 1));
					}

					if (finisher == null)
					{
						finisher = new FixWrapper(new Fix(new HiResDate(0,
								end.getMicros() + 1), _zeroLocation, 0.0, 0.0));
					} else
					{
						finisher.getFix().setTime(new HiResDate(0, end.getMicros() + 1));
					}

					// ok, ready, go for it.
					set = _thePositions.subSet(starter, finisher);
				}

			}
		}

		return set;
	}

	/**
	 * method to allow the setting of label frequencies for the track
	 * 
	 * @return frequency to use
	 */
	public final HiResDate getLabelFrequency()
	{
		return this._lastLabelFrequency;
	}

	// //////////////////////////////////////
	// editing parameters
	// //////////////////////////////////////

	/**
	 * the line thickness (convenience wrapper around width)
	 * 
	 * @return
	 */
	public int getLineThickness()
	{
		return _lineWidth;
	}

	/**
	 * name of this Track (normally the vessel name)
	 * 
	 * @return the name
	 */
	public final String getName()
	{
		return _theLabel.getString();
	}

	/**
	 * whether to show the track label at the start or end of the track
	 * 
	 * @return yes/no to indicate <I>At Start</I>
	 */
	public final boolean getNameAtStart()
	{
		return _LabelAtStart;
	}

	/**
	 * the relative location of the label
	 * 
	 * @return the relative location
	 */
	public final Integer getNameLocation()
	{
		return _theLabel.getRelativeLocation();
	}

	/**
	 * whether the track label is visible or not
	 * 
	 * @return yes/no
	 */
	public final boolean getNameVisible()
	{
		return _theLabel.getVisible();
	}

	/**
	 * find the fix nearest to this time (or the first fix for an invalid time)
	 * 
	 * @param DTG
	 *          the time of interest
	 * @return the nearest fix
	 */
	public final Watchable[] getNearestTo(final HiResDate srchDTG)
	{
		/**
		 * we need to end up with a watchable, not a fix, so we need to work our way
		 * through the fixes
		 */
		FixWrapper res = null;

		// check that we do actually contain some data
		if (_thePositions.size() == 0)
			return new Debrief.Tools.Tote.Watchable[]
			{};

		// special case - if we've been asked for an invalid time value
		if (srchDTG == TimePeriod.INVALID_DATE)
			// just return our first location
			return new Debrief.Tools.Tote.Watchable[]
			{ (Watchable) _thePositions.first() };

		// see if this is the DTG we have just requestsed
		if ((srchDTG.equals(lastDTG)) && (lastFix != null))
		{
			res = lastFix;
		} else
		{
			// see if this DTG is inside our data range
			// in which case we will just return null
			final FixWrapper theFirst = (FixWrapper) _thePositions.first();
			final FixWrapper theLast = (FixWrapper) _thePositions.last();

			if ((srchDTG.greaterThanOrEqualTo(theFirst.getTime()))
					&& (srchDTG.lessThanOrEqualTo(theLast.getTime())))
			{
				// yes it's inside our data range, find the first fix
				// after the indicated point

				// right, increment the time, since we want to allow matching
				// points
				// HiResDate DTG = new HiResDate(0, srchDTG.getMicros() + 1);

				// see if we have to create our local temporary fix
				if (nearestFix == null)
				{
					nearestFix = new FixWrapper(new Fix(srchDTG, _zeroLocation, 0.0, 0.0));
				} else
					nearestFix.getFix().setTime(srchDTG);

				// right, we really should be filtering the list according to if
				// the
				// points are visible.
				// how do we do filters?

				// get the data. use tailSet, since it's inclusive...
				SortedSet<Editable> set = _thePositions.tailSet(nearestFix);

				// see if the requested DTG was inside the range of the data
				if (!set.isEmpty())
				{
					res = (FixWrapper) set.first();

					// is this one visible?
					if (!res.getVisible())
					{
						// right, the one we found isn't visible. duplicate the
						// set, so that
						// we can remove items
						// without affecting the parent
						set = new TreeSet<Editable>(set);

						// ok, start looping back until we find one
						while ((!res.getVisible()) && (set.size() > 0))
						{

							// the first one wasn't, remove it
							set.remove(res);
							if (set.size() > 0)
								res = (FixWrapper) set.first();
						}
					}

				}

				// right, that's the first points on or before the indicated
				// DTG. Are we
				// meant
				// to be interpolating?
				if (res != null)
					if (getInterpolatePoints())
					{
						// right - just check that we aren't actually on the
						// correct time
						// point.
						// HEY, USE THE ORIGINAL SEARCH TIME, NOT THE
						// INCREMENTED ONE,
						// SINCE WE DON'T WANT TO COMPARE AGAINST A MODIFIED
						// TIME

						if (!res.getTime().equals(srchDTG))
						{

							// right, we haven't found an actual data point.
							// Better calculate
							// one

							// hmm, better also find the point before our one.
							// the
							// headSet operation is exclusive - so we need to
							// find the one
							// after the first
							final SortedSet<Editable> otherSet = _thePositions
									.headSet(nearestFix);

							FixWrapper previous = null;

							if (!set.isEmpty())
							{
								previous = (FixWrapper) otherSet.last();
							}

							// did it work?
							if (previous != null)
							{
								// cool, sort out the interpolated point USING
								// THE ORIGINAL
								// SEARCH TIME
								res = getInterpolatedFix(previous, res, srchDTG);
							}
						}
					}

			}

			// and remember this fix
			lastFix = res;
			lastDTG = srchDTG;
		}

		if (res != null)
			return new Debrief.Tools.Tote.Watchable[]
			{ res };
		else
			return new Debrief.Tools.Tote.Watchable[]
			{};

	}

	/**
	 * get the position data, not all the sensor/contact/position data mixed
	 * together
	 * 
	 * @return
	 */
	public final Enumeration<Editable> getPositions()
	{
		Enumeration<Editable> res = null;
		if (_thePositions != null)
			res = _thePositions.elements();

		return res;
	}

	/**
	 * determine whether we are linking the points on the track
	 * 
	 * @return yes/no
	 */
	public final boolean getPositionsLinked()
	{
		return _linkPositions;
	}

	/**
	 * whether the individual fixes themselves are shown either by a symbol or
	 * label
	 * 
	 * @return yes/no
	 */
	public final boolean getPositionsVisible()
	{
		return _showPositions;
	}

	/**
	 * get the list of sensors for this track
	 */
	public final Enumeration<SensorWrapper> getSensors()
	{
		Enumeration<SensorWrapper> res = null;

		if (_mySensors != null)
			res = _mySensors.elements();

		return res;
	}

	/**
	 * return the symbol to be used for plotting this track in snail mode
	 */
	public final MWC.GUI.Shapes.Symbols.PlainSymbol getSnailShape()
	{
		return _theSnailShape;
	}

	/**
	 * get the list of sensors for this track
	 */
	public final Enumeration<TMAWrapper> getSolutions()
	{
		Enumeration<TMAWrapper> res = null;

		if (_mySolutions != null)
			res = _mySolutions.elements();

		return res;
	}

	// //////////////////////////////////////
	// watchable (tote related) parameters
	// //////////////////////////////////////
	/**
	 * the earliest fix in the track
	 * 
	 * @return the DTG
	 */
	public final HiResDate getStartDTG()
	{
		HiResDate res = null;
		if(_myTimePeriod != null)
			res = _myTimePeriod.getStartDTG();
		return res;
	}

	/**
	 * return the symbol frequencies for the track
	 * 
	 * @return frequency in seconds
	 */
	public final HiResDate getSymbolFrequency()
	{
		return _lastSymbolFrequency;
	}

	/**
	 * get the type of this symbol
	 */
	public final String getSymbolType()
	{
		return _theSnailShape.getType();
	}

	/**
	 * the colour of the points on the track
	 * 
	 * @return the colour
	 */
	public final Color getTrackColor()
	{
		return getColor();
	}

	/**
	 * font handler
	 * 
	 * @return the font to use for the label
	 */
	public final java.awt.Font getTrackFont()
	{
		return _theLabel.getFont();
	}

	/**
	 * get the set of fixes contained within this time period which haven't been
	 * filtered, and which have valid depths. The isVisible flag indicates whether
	 * a track has been filtered or not. We also have the getVisibleFixesBetween
	 * method (below) which decides if a fix is visible if it is set to Visible,
	 * and it's label or symbol are visible. <p/> We don't have to worry about a
	 * valid depth, since 3d doesn't show points with invalid depth values
	 * 
	 * @param start
	 *          start DTG
	 * @param end
	 *          end DTG
	 * @return series of fixes
	 */
	public final Collection<Editable> getUnfilteredItems(final HiResDate start,
			final HiResDate end)
	{

		// if we have an invalid end point, just return the full track
		if (end == TimePeriod.INVALID_DATE)
			return _thePositions.getData();

		// see if we have _any_ points in range
		if ((getStartDTG().greaterThan(end)) || (getEndDTG().lessThan(start)))
			return null;

		if (this.getVisible() == false)
			return null;

		// get ready for the output
		final Vector<Editable> res = new Vector<Editable>(0, 1);

		// put the data into a period
		final TimePeriod thePeriod = new TimePeriod.BaseTimePeriod(start, end);

		// step through our fixes
		final Enumeration<Editable> iter = _thePositions.elements();
		while (iter.hasMoreElements())
		{
			final FixWrapper fw = (FixWrapper) iter.nextElement();
			if (fw.getVisible())
			{
				// is it visible?
				if (thePeriod.contains(fw.getTime()))
				{
					res.add(fw);
				}
			}
		}

		return res;
	}

	/**
	 * get the set of fixes contained within this time period
	 * 
	 * @param start
	 *          start DTG
	 * @param end
	 *          end DTG
	 * @return series of fixes
	 */
	public final Collection<Editable> getVisibleItemsBetween(
			final HiResDate start, final HiResDate end)
	{

		// see if we have _any_ points in range
		if ((getStartDTG().greaterThan(end)) || (getEndDTG().lessThan(start)))
			return null;

		if (this.getVisible() == false)
			return null;

		// get ready for the output
		final Vector<Editable> res = new Vector<Editable>(0, 1);

		// put the data into a period
		final TimePeriod thePeriod = new TimePeriod.BaseTimePeriod(start, end);

		// step through our fixes
		final Enumeration<Editable> iter = _thePositions.elements();
		while (iter.hasMoreElements())
		{
			final FixWrapper fw = (FixWrapper) iter.nextElement();
			if (fw.getVisible() && (fw.getSymbolShowing() || fw.getLabelShowing()))
			{
				// is it visible?
				if (thePeriod.contains(fw.getTime()))
				{
					// hey, it's valid - continue
					res.add(fw);
				}
			}
		}
		return res;
	}

	/**
	 * whether this object has editor details
	 * 
	 * @return yes/no
	 */
	public final boolean hasEditor()
	{
		return true;
	}

	public boolean hasOrderedChildren()
	{
		return false;
	}

	/**
	 * quick accessor for how many fixes we have
	 * 
	 * @return
	 */
	public int numFixes()
	{
		return _thePositions.size();
	}

	/**
	 * draw this track (we can leave the Positions to draw themselves)
	 * 
	 * @param dest
	 *          the destination
	 */
	public final void paint(final CanvasType dest)
	{
		if (getVisible())
		{
			// set the thickness for this track
			dest.setLineWidth(_lineWidth);

			// and set the initial colour for this track
			dest.setColor(getColor());

			// /////////////////////////////////////////////
			// firstly plot the solutions
			// /////////////////////////////////////////////
			if (_mySolutions != null)
			{
				final Enumeration<TMAWrapper> iter = _mySolutions.elements();
				while (iter.hasMoreElements())
				{
					final TMAWrapper sw = iter.nextElement();
					sw.paint(dest);

				} // through the solutions
			} // whether we have any solutions

			// /////////////////////////////////////////////
			// now plot the sensors
			// /////////////////////////////////////////////
			if (_mySensors != null)
			{
				final Enumeration<SensorWrapper> iter = _mySensors.elements();
				while (iter.hasMoreElements())
				{
					final SensorWrapper sw = iter.nextElement();
					sw.paint(dest);

				} // through the sensors
			} // whether we have any sensors

			// /////////////////////////////////////////////
			// and now the track itself
			// /////////////////////////////////////////////

			// is our points store long enough?
			if ((_myPts == null) || (_myPts.length < numFixes() * 2))
			{
				_myPts = new int[numFixes() * 2];
			}

			// reset the points counter
			_ptCtr = 0;

			// java.awt.Point lastP = null;
			Color lastCol = null;

			boolean locatedTrack = false;
			WorldLocation lastLocation = null;

			// just check if we are drawing anything at all
			if ((!_linkPositions) && (!_showPositions))
				return;

			// keep track of if we have plotted any points (since
			// we won't be plotting the name if none of the points are visible).
			// this typically occurs when filtering is applied and a short
			// track is completely outside the time period
			boolean plotted_anything = false;

			// ///////////////////////////////////////////
			// let the fixes draw themselves in
			// ///////////////////////////////////////////
			final Enumeration<Editable> fixWrappers = _thePositions.elements();
			while (fixWrappers.hasMoreElements())
			{
				final FixWrapper fw = (FixWrapper) fixWrappers.nextElement();

				// is this fix visible
				if (!fw.getVisible())
				{
					// nope. Don't join it to the last position.
					// ok, if we've built up a polygon, we need to write it now
					paintTrack(dest, lastCol);
				} else
				{
					// yup, it's visible. carry on.

					// ok, so we have plotted something
					plotted_anything = true;

					// this is a valid one, remember the details
					lastLocation = fw.getLocation();
					final java.awt.Point thisP = dest.toScreen(lastLocation);

					// just check that there's enough GUI to create the plot
					// (i.e. has a point been returned)
					if (thisP == null)
						return;

					// so, we're looking at the first data point. Do
					// we want to use this to locate the track name?
					if (_LabelAtStart)
					{
						// or have we already sorted out the location
						if (!locatedTrack)
						{
							locatedTrack = true;
							_theLabel.setLocation(new WorldLocation(lastLocation));
						}
					}

					// are we
					if (_linkPositions)
					{
						// right, just check if we're a different colour to the
						// previous one
						final Color thisCol = fw.getColor();

						// do we know the previous colour
						if (lastCol == null)
						{
							lastCol = thisCol;
						}

						// is this to be joined to the previous one?
						if (fw.getLineShowing())
						{
							// so, grow the the polyline, unless we've got a colour
							// change...
							if (thisCol != lastCol)
							{
								// add our position to the list - so it finishes on us
								_myPts[_ptCtr++] = thisP.x;
								_myPts[_ptCtr++] = thisP.y;

								// yup, better get rid of the previous polygon
								paintTrack(dest, lastCol);
							}

							// add our position to the list - we'll output the
							// polyline at the end
							_myPts[_ptCtr++] = thisP.x;
							_myPts[_ptCtr++] = thisP.y;
						} else
						{

							// nope, output however much line we've got so far -
							// since this
							// line won't be joined to future points
							paintTrack(dest, thisCol);

							// start off the next line
							_myPts[_ptCtr++] = thisP.x;
							_myPts[_ptCtr++] = thisP.y;

						}

						// set the colour of the track from now on to this
						// colour, so that
						// the "link" to the next fix is set to this colour if
						// left
						// unchanged
						dest.setColor(fw.getColor());

						// and remember the last colour
						lastCol = thisCol;

					}

					if (_showPositions)
					{
						fw.paintMe(dest);
					}
				}

			}

			// ok, just see if we have any pending polylines to paint
			paintTrack(dest, lastCol);

			// are we trying to put the label at the end of the track?
			if (!_LabelAtStart)
			{
				// check that we have found at least one location to plot.
				if (lastLocation != null)
					_theLabel.setLocation(new WorldLocation(lastLocation));
			}

			// and draw the track label
			if (_theLabel.getVisible())
			{

				// still, we only plot the track label if we have plotted any
				// points
				if (plotted_anything)
				{

					// check that we have found a location for the lable
					if (_theLabel.getLocation() != null)
					{

						// check that we have set the name for the label
						if (_theLabel.getString() == null)
						{
							_theLabel.setString(getName());
						}

						if (_theLabel.getColor() == null)
						{
							_theLabel.setColor(getColor());
						}

						// and paint it
						_theLabel.paint(dest);

					} // if the label has a location
				}
			} // if the label is visible

		} // if visible
	}

	/**
	 * paint any polyline that we've built up
	 * 
	 * @param dest -
	 *          where we're painting to
	 * @param thisCol
	 */
	private void paintTrack(final CanvasType dest, final Color thisCol)
	{
		if (_ptCtr > 0)
		{
			dest.setColor(thisCol);
			final int[] poly = new int[_ptCtr];
			System.arraycopy(_myPts, 0, poly, 0, _ptCtr);
			dest.drawPolyline(poly);

			// and reset the counter
			_ptCtr = 0;
		}
	}

	/**
	 * return the range from the nearest corner of the track
	 * 
	 * @param other
	 *          the other location
	 * @return the range
	 */
	public final double rangeFrom(final WorldLocation other)
	{
		double nearest = -1;

		// do we have a track?
		if (_myWorldArea != null)
		{
			// find the nearest point on the track
			nearest = _myWorldArea.rangeFrom(other);
		}

		return nearest;
	}

	/**
	 * remove the requested item from the track
	 * 
	 * @param point
	 *          the point to remove
	 */
	public final void removeElement(final Editable point)
	{
		// just see if it's a sensor which is trying to be removed
		if (point instanceof SensorWrapper)
		{
			_mySensors.remove(point);
		} else if (point instanceof TMAWrapper)
		{
			_mySolutions.remove(point);
		} else if (point instanceof SensorContactWrapper)
		{
			// ok, cycle through our sensors, try to remove this contact...
			final Iterator<SensorWrapper> iter = _mySensors.iterator();
			while (iter.hasNext())
			{
				final SensorWrapper sw = iter.next();
				// try to remove it from this one...
				sw.removeElement(point);
			}
		} else
		{
			_thePositions.removeElement(point);
		}

	}

	/**
	 * pass through the track, resetting the labels back to their original DTG
	 */
	public void resetLabels()
	{
		FormatTracks.formatTrack(this);
	}

	/**
	 * set the colour of this track label
	 * 
	 * @param theCol
	 *          the colour
	 */
	public final void setColor(final Color theCol)
	{
		// do the parent
		super.setColor(theCol);

		// now do our processing
		_theLabel.setColor(theCol);
		_theSnailShape.setColor(theCol);
	}

	public final void setDragTrack(TrackWrapper track)
	{
		//
	}

	/**
	 * the setter function which passes through the track
	 */
	private void setFixes(final FixSetter setter, final HiResDate theVal)
	{
		final long freq = theVal.getMicros();

		// briefly check if we are revealing/hiding all times (ie if freq is 1
		// or 0)
		if (freq == TimeFrequencyPropertyEditor.SHOW_ALL_FREQUENCY)
		{
			// show all of the labels
			final Enumeration<Editable> iter = _thePositions.elements();
			while (iter.hasMoreElements())
			{
				final FixWrapper fw = (FixWrapper) iter.nextElement();
				setter.execute(fw, true);
			}
		} else
		{
			// no, we're not just blindly doing all of them. do them at the
			// correct
			// frequency

			// hide all of the labels/symbols first
			final Enumeration<Editable> enumA = _thePositions.elements();
			while (enumA.hasMoreElements())
			{
				final FixWrapper fw = (FixWrapper) enumA.nextElement();
				setter.execute(fw, false);
			}

			if (freq == 0)
			{
				// we can ignore this, since we have just hidden all of the
				// points
			} else
			{

				// pass through the track setting the values

				// sort out the start and finish times
				long start_time = getStartDTG().getMicros();
				final long end_time = getEndDTG().getMicros();

				// first check that there is a valid time period between start
				// time
				// and end time
				if (start_time + freq < end_time)
				{
					long num = start_time / freq;

					// we need to add one to the quotient if it has rounded down
					if (start_time % freq == 0)
					{
						// start is at our freq, so we don't need to increment
						// it
					} else
					{
						num++;
					}

					// calculate new start time
					start_time = num * freq;
				} else
				{
					// there is not one of our 'intervals' between the start and
					// the end,
					// so use the start time
				}

				while (start_time <= end_time)
				{
					// right, increment the start time by one, because we were
					// getting the
					// fix immediately before the requested time
					final HiResDate thisDTG = new HiResDate(0, start_time);
					final Debrief.Tools.Tote.Watchable[] list = this
							.getNearestTo(thisDTG);
					// check we found some
					if (list.length > 0)
					{
						final FixWrapper fw = (FixWrapper) list[0];
						setter.execute(fw, true);
					}
					// produce the next time step
					start_time += freq;
				}
			}

		}
	}

	public final void setInterpolatePoints(boolean val)
	{
		_interpolatePoints = val;
	}

	/**
	 * set the label frequency (in seconds)
	 * 
	 * @param theVal
	 *          frequency to use
	 */
	public final void setLabelFrequency(final HiResDate theVal)
	{
		this._lastLabelFrequency = theVal;

		final FixSetter setLabel = new FixSetter()
		{
			public void execute(final FixWrapper fix, final boolean val)
			{
				fix.setLabelShowing(val);
			}
		};
		setFixes(setLabel, theVal);
	}

	/**
	 * the line thickness (convenience wrapper around width)
	 */
	public void setLineThickness(final int val)
	{
		_lineWidth = val;
	}

	/**
	 * set the name of this track (normally the name of the vessel
	 * 
	 * @param theName
	 *          the name as a String
	 */
	public final void setName(final String theName)
	{
		_theLabel.setString(theName);
	}

	// ////////////////////////////////////////////////////
	// LAYER support methods
	// /////////////////////////////////////////////////////

	/**
	 * whether to show the track name at the start or end of the track
	 * 
	 * @param val
	 *          yes no for <I>show label at start</I>
	 */
	public final void setNameAtStart(final boolean val)
	{
		_LabelAtStart = val;
	}

	/**
	 * the relative location of the label
	 * 
	 * @param val
	 *          the relative location
	 */
	public final void setNameLocation(final Integer val)
	{
		_theLabel.setRelativeLocation(val);
	}

	/**
	 * whether to show the track name
	 * 
	 * @param val
	 *          yes/no
	 */
	public final void setNameVisible(final boolean val)
	{
		_theLabel.setVisible(val);
	}

	/**
	 * indicate whether to join the points on the track
	 * 
	 * @param val
	 *          yes/no
	 */
	public final void setPositionsLinked(final boolean val)
	{
		_linkPositions = val;
	}

	/**
	 * whether to show the position fixes
	 * 
	 * @param val
	 *          yes/no
	 */
	public final void setPositionsVisible(final boolean val)
	{
		_showPositions = val;
	}

	/**
	 * how frequently symbols are placed on the track
	 * 
	 * @param theVal
	 *          frequency in seconds
	 */
	public final void setSymbolFrequency(final HiResDate theVal)
	{
		this._lastSymbolFrequency = theVal;

		// set the "showPositions" parameter, as long as we are
		// not setting the symbols off
		if (theVal.getMicros() != 0.0)
		{
			this.setPositionsVisible(true);
		}

		final FixSetter setSymbols = new FixSetter()
		{
			public void execute(final FixWrapper fix, final boolean val)
			{
				fix.setSymbolShowing(val);
			}
		};

		setFixes(setSymbols, theVal);
	}

	// ////////////////////////////////////////////////////
	// track-shifting operation
	// /////////////////////////////////////////////////////

	// /////////////////////////////////////////////////
	// support for dragging the track around
	// ////////////////////////////////////////////////

	public final void setSymbolType(final String val)
	{
		// is this the type of our symbol?
		if (val.equals(_theSnailShape.getType()))
		{
			// don't bother we're using it already
		} else
		{
			// remember the size of the symbol
			final double scale = _theSnailShape.getScaleVal();
			// replace our symbol with this new one
			_theSnailShape = null;
			_theSnailShape = MWC.GUI.Shapes.Symbols.SymbolFactory.createSymbol(val);
			_theSnailShape.setColor(this.getColor());

			_theSnailShape.setScaleVal(scale);
		}
	}

	// note we are putting a track-labelled wrapper around the colour
	// parameter, to make the properties window less confusing
	/**
	 * the colour of the points on the track
	 * 
	 * @param theCol
	 *          the colour to use
	 */
	public final void setTrackColor(final Color theCol)
	{
		setColor(theCol);
	}

	/**
	 * font handler
	 * 
	 * @param font
	 *          the font to use for the label
	 */
	public final void setTrackFont(final java.awt.Font font)
	{
		_theLabel.setFont(font);
	}

	public void shift(WorldLocation feature, WorldVector vector)
	{
		feature.addToMe(vector);
	}

	// ///////////////////////////////////////////////////////////////
	// read/write operations
	// //////////////////////////////////////////////////////////////
	// private void readObject(java.io.ObjectInputStream in)
	// throws IOException, ClassNotFoundException
	// {
	// in.defaultReadObject();
	//
	// // TOD: eventually, remove support for the old "_theData" type
	// // of storing tracks, leave to store in native (treeSet) storage
	//
	// // see if we are processing an old version of the file
	// /* if(this._fastData == null)
	// {
	// _fastData = new com.sun.java.util.collections.TreeSet(new
	// compareFixes());
	//
	// // move all of the contents of the vector to the fast wrapper
	// java.util.Enumeration enum = _theData.elements();
	// while(enum.hasMoreElements())
	// {
	// FixWrapper fw = (FixWrapper)enum.nextElement();
	// _fastData.add(fw);
	// }
	//
	//
	// // now remove all of the elements from the old structure
	// _theData.removeAllElements();
	// _theData = null;
	// }
	// */
	// // check that we have our track lable
	// if(_theLabel == null)
	// {
	// _theLabel = new MWC.GUI.Shapes.TextLabel(new WorldLocation(0,0,0), null);
	// _theLabel.setName(getName());
	// _theLabel.setColor(getColor());
	// }
	// }
	//
	//
	// private void writeObject(java.io.ObjectOutputStream out)
	// throws IOException
	// {
	// // put the fast data into the old vector
	//
	// // create the old vector
	// /* _theData = new java.util.Vector(_fastData.size(), 1);
	//
	// // get an iterator from the fast data
	// Iterator it = _fastData.iterator();
	//
	// // put the fast data into the old array
	// while(it.hasNext())
	// {
	// _theData.addElement(it.next());
	// }
	//
	// */
	// // allow the default write to copy the array to storage
	// out.defaultWriteObject();
	//
	//
	// }

	// //////////////////////////////////////
	// beaninfo
	// //////////////////////////////////////

	public void shift(WorldVector vector)
	{
		this.shiftTrack(elements(), vector);
	}

	/**
	 * move the whole of the track be the provided offset
	 */
	public final void shiftTrack(Enumeration<Editable> theEnum,
			final WorldVector offset)
	{
		// keep track of if the track contains something that doesn't get
		// dragged
		boolean handledData = false;

		if (theEnum == null)
			theEnum = elements();

		while (theEnum.hasMoreElements())
		{
			final Object thisO = theEnum.nextElement();
			if (thisO instanceof FixWrapper)
			{
				final FixWrapper fw = (FixWrapper) thisO;

				final WorldLocation copiedLoc = new WorldLocation(fw.getFix()
						.getLocation());
				copiedLoc.addToMe(offset);

				// and replace the location (this method updates all 3 location
				// contained
				// in the fix wrapper
				fw.setFixLocation(copiedLoc);

				// ok - job well done
				handledData = true;

			} // whether this was a fix wrapper
			else if (thisO instanceof SensorWrapper)
			{
				final SensorWrapper sw = (SensorWrapper) thisO;
				final Enumeration<Editable> enumS = sw.elements();
				while (enumS.hasMoreElements())
				{
					final SensorContactWrapper scw = (SensorContactWrapper) enumS
							.nextElement();
					// does this fix have it's own origin?
					final WorldLocation sensorOrigin = scw.getOrigin(null);

					if (sensorOrigin != null)
					{
						// create new object to contain the updated location
						final WorldLocation newSensorLocation = new WorldLocation(
								sensorOrigin);
						newSensorLocation.addToMe(offset);

						// so the contact did have an origin, change it
						scw.setOrigin(newSensorLocation);
					}
				} // looping through the contacts

				// ok - job well done
				handledData = true;

			} // whether this is a sensor wrapper
			else if (thisO instanceof TrackWrapper.PlottableLayer)
			{
				final PlottableLayer tw = (PlottableLayer) thisO;
				final Enumeration<Editable> enumS = tw.elements();

				// fire recursively, smart-arse.
				shiftTrack(enumS, offset);

				// ok - job well done
				handledData = true;

			} // whether this is a sensor wrapper
		} // looping through this track

		// ok, did we handle the data?
		if (!handledData)
		{
			System.err.println("TrackWrapper problem; not able to shift:" + theEnum);
		}
	}

	/**
	 * extra parameter, so that jvm can produce a sensible name for this
	 * 
	 * @return the track name, as a string
	 */
	public final String toString()
	{
		return "Track:" + getName();
	}

	/**
	 * is this track visible between these time periods?
	 * 
	 * @param start
	 *          start DTG
	 * @param end
	 *          end DTG
	 * @return yes/no
	 */
	public final boolean visibleBetween(final HiResDate start, final HiResDate end)
	{
		boolean visible = false;
		if (getStartDTG().lessThan(end) && (getEndDTG().greaterThan(start)))
		{
			visible = true;
		}

		return visible;
	}

	// /////////////////////////////////////////////////
	// nested interface which contains a single method, taking a
	// boolean parameter
	// ////////////////////////////////////////////////
	/**
	 * interface defining a boolean operation which is applied to all fixes in a
	 * track
	 */
	protected interface FixSetter
	{
		/**
		 * operation to apply to a fix
		 * 
		 * @param fix
		 *          subject of operation
		 * @param val
		 *          yes/no value to apply
		 */
		public void execute(FixWrapper fix, boolean val);
	}

	// //////////////////////////////////////////////////////////////////
	// embedded class to allow us to pass the local iterator (Iterator) used
	// internally
	// outside as an Enumeration
	// /////////////////////////////////////////////////////////////////
	private static final class IteratorWrapper implements
			java.util.Enumeration<Editable>
	{
		private final Iterator<Editable> _val;

		public IteratorWrapper(final Iterator<Editable> iterator)
		{
			_val = iterator;
		}

		public final boolean hasMoreElements()
		{
			return _val.hasNext();

		}

		public final Editable nextElement()
		{
			return _val.next();
		}
	}

	/**
	 * convenience class that makes our plottables look like a layer
	 * 
	 * @author ian.mayo
	 */
	public class PlottableLayer extends Plottables implements Layer
	{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * only allow fixes to be added...
		 * 
		 * @param thePlottable
		 */
		public void add(Editable thePlottable)
		{
			if (thePlottable instanceof FixWrapper)
			{
				super.add(thePlottable);
			} else
			{
				System.err.println("Trying to add wront");
			}
		}

		public void append(Layer other)
		{
			// ok, pass through and add the items
			final Enumeration<Editable> enumer = other.elements();
			while (enumer.hasMoreElements())
			{
				final Plottable pl = (Plottable) enumer.nextElement();
				add(pl);
			}
		}

		public void exportShape()
		{
			// ignore..
		}

		/**
		 * get the editing information for this type
		 */
		public Editable.EditorType getInfo()
		{
			return new plottableLayerInfo(this);
		}

		public int getLineThickness()
		{
			// ignore..
			return 1;
		}

		/**
		 * @return
		 */
		@Override
		public boolean getVisible()
		{
			return getPositionsLinked();
		}

		public boolean hasOrderedChildren()
		{
			return true;
		}

		/**
		 * @param visible
		 */
		@Override
		public void setVisible(boolean visible)
		{
			setPositionsLinked(visible);
		}

		/**
		 * class containing editable details of a track
		 */
		public final class plottableLayerInfo extends Editable.EditorType
		{

			/**
			 * constructor for this editor, takes the actual track as a parameter
			 * 
			 * @param data
			 *          track being edited
			 */
			public plottableLayerInfo(final PlottableLayer data)
			{
				super(data, data.getName(), "");
			}

			public final String getName()
			{
				return super.getName();
			}

			public final PropertyDescriptor[] getPropertyDescriptors()
			{
				try
				{
					final PropertyDescriptor[] res =
					{ expertProp("Visible", "whether this layer is visible", FORMAT), };
					return res;
				} catch (final IntrospectionException e)
				{
					e.printStackTrace();
					return super.getPropertyDescriptors();
				}
			}
		}

	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////
	// testing for this class
	// ////////////////////////////////////////////////////////////////////////////////////////////////
	static public final class testMe extends junit.framework.TestCase
	{
		/**
		 * utility to track number of calls
		 * 
		 */
		static int callCount = 0;

		/**
		 * utility to track number of points passed to paint polyline method
		 * 
		 */
		static int pointCount = 0;;

		static public final String TEST_ALL_TEST_TYPE = "UNIT";

		public testMe(final String val)
		{
			super(val);
		}

		@SuppressWarnings("synthetic-access")
		public final void testFilterToTimePeriod()
		{
			TrackWrapper tw = getDummyTrack();		 			
			HiResDate startH = new HiResDate(150, 0);
			HiResDate endH = new HiResDate(450, 0);
			tw.filterListTo(startH , endH );
			int ctr = countVisibleFixes(tw);
			int sCtr = countVisibleSensorWrappers(tw);
			int tCtr = countVisibleSolutionWrappers(tw);
			assertEquals("contains correct number of entries", 3, ctr);
			assertEquals("contains correct number of sensor entries", 6, sCtr);
			assertEquals("contains correct number of sensor entries", 5, tCtr);

			tw = getDummyTrack();			 			
			startH = new HiResDate(350, 0);
			endH = new HiResDate(550, 0);
			tw.filterListTo(startH , endH );
			ctr = countVisibleFixes(tw);
			sCtr = countVisibleSensorWrappers(tw);
			tCtr = countVisibleSolutionWrappers(tw);
			assertEquals("contains correct number of entries", 2, ctr);
			assertEquals("contains correct number of sensor entries", 1, sCtr);
			assertEquals("contains correct number of sensor entries", 1, tCtr);
			
			tw = getDummyTrack();			 			
			startH = new HiResDate(0, 0);
			endH = new HiResDate(450, 0);
			tw.filterListTo(startH , endH );
			ctr = countVisibleFixes(tw);
			sCtr = countVisibleSensorWrappers(tw);
			tCtr = countVisibleSolutionWrappers(tw);
			assertEquals("contains correct number of entries", 4, ctr);
			assertEquals("contains correct number of sensor entries", 6, sCtr);
			assertEquals("contains correct number of sensor entries", 6, tCtr);
		}

		@SuppressWarnings("synthetic-access")
		private int countVisibleSensorWrappers(TrackWrapper tw)
		{
			Iterator<SensorWrapper> iter2 = tw._mySensors.iterator();			
			int sCtr = 0;
			while(iter2.hasNext())
			{
				SensorWrapper sw = iter2.next();
				Enumeration<Editable> enumS = sw.elements();
				while(enumS.hasMoreElements())
				{
					Plottable pl = (Plottable) enumS.nextElement();
					if(pl.getVisible())
						sCtr++;
				}
			}
			return sCtr;
		}

		@SuppressWarnings("synthetic-access")
		private int countVisibleSolutionWrappers(TrackWrapper tw)
		{
			Iterator<TMAWrapper> iter2 = tw._mySolutions.iterator();			
			int sCtr = 0;
			while(iter2.hasNext())
			{
				TMAWrapper sw = iter2.next();
				Enumeration<Editable> enumS = sw.elements();
				while(enumS.hasMoreElements())
				{
					Plottable pl = (Plottable) enumS.nextElement();
					if(pl.getVisible())
						sCtr++;
				}
			}
			return sCtr;
		}
		
		@SuppressWarnings("synthetic-access")
		private int countVisibleFixes(TrackWrapper tw)
		{
			int ctr = 0;
			Enumeration<Editable> iter;
			iter = tw._thePositions.elements();
			while(iter.hasMoreElements())
			{
				Plottable thisE = (Plottable) iter.nextElement();
				if(thisE.getVisible())
					ctr++;
			}
			return ctr;
		}

		private TrackWrapper getDummyTrack()
		{
			final TrackWrapper tw = new TrackWrapper();

			final WorldLocation loc_1 = new WorldLocation(0, 0, 0);
			final FixWrapper fw1 = new FixWrapper(new Fix(new HiResDate(100, 10000),
					loc_1.add(new WorldVector(33, new WorldDistance(100,
							WorldDistance.METRES), null)), 10, 110));
			fw1.setLabel("fw1");
			final FixWrapper fw2 = new FixWrapper(new Fix(new HiResDate(200, 20000),
					loc_1.add(new WorldVector(33, new WorldDistance(200,
							WorldDistance.METRES), null)), 20, 120));
			fw2.setLabel("fw2");
			final FixWrapper fw3 = new FixWrapper(new Fix(new HiResDate(300, 30000),
					loc_1.add(new WorldVector(33, new WorldDistance(300,
							WorldDistance.METRES), null)), 30, 130));
			fw3.setLabel("fw3");
			final FixWrapper fw4 = new FixWrapper(new Fix(new HiResDate(400, 40000),
					loc_1.add(new WorldVector(33, new WorldDistance(400,
							WorldDistance.METRES), null)), 40, 140));
			fw4.setLabel("fw4");
			final FixWrapper fw5 = new FixWrapper(new Fix(new HiResDate(500, 50000),
					loc_1.add(new WorldVector(33, new WorldDistance(500,
							WorldDistance.METRES), null)), 50, 150));
			fw5.setLabel("fw5");
			tw.addFix(fw1);
			tw.addFix(fw2);
		  tw.addFix(fw3);
			tw.addFix(fw4);
			tw.addFix(fw5);
			// also give it some sensor data
			SensorWrapper swa = new SensorWrapper("title one");
			SensorContactWrapper  scwa1 = new SensorContactWrapper("aaa", new HiResDate(150,0),null,0,null,null,null,0,null);
			SensorContactWrapper  scwa2 = new SensorContactWrapper("bbb", new HiResDate(180,0),null,0,null,null,null,0,null);
			SensorContactWrapper  scwa3 = new SensorContactWrapper("ccc", new HiResDate(250,0),null,0,null,null,null,0,null);
			swa.add(scwa1);
			swa.add(scwa2);
			swa.add(scwa3);
			tw.add(swa);
			SensorWrapper sw = new SensorWrapper("title two");
			SensorContactWrapper  scw1 = new SensorContactWrapper("ddd", new HiResDate(260,0),null,0,null,null,null,0,null);
			SensorContactWrapper  scw2 = new SensorContactWrapper("eee", new HiResDate(280,0),null,0,null,null,null,0,null);
			SensorContactWrapper  scw3 = new SensorContactWrapper("fff", new HiResDate(350,0),null,0,null,null,null,0,null);
			sw.add(scw1);
			sw.add(scw2);
			sw.add(scw3);
			tw.add(sw);

			TMAWrapper mwa = new TMAWrapper("bb");
			TMAContactWrapper tcwa1 = new TMAContactWrapper("aaa", "bbb", new HiResDate(130),null, 0,0,0, null, null, null, null);
			TMAContactWrapper tcwa2 = new TMAContactWrapper("bbb", "bbb", new HiResDate(190),null, 0,0,0, null, null, null, null);
			TMAContactWrapper tcwa3 = new TMAContactWrapper("ccc", "bbb", new HiResDate(230),null, 0,0,0, null, null, null, null);
			mwa.add(tcwa1);
			mwa.add(tcwa2);
			mwa.add(tcwa3);
			tw.add(mwa);
			TMAWrapper mw = new TMAWrapper("cc");
			TMAContactWrapper tcw1 = new TMAContactWrapper("ddd", "bbb", new HiResDate(230),null, 0,0,0, null, null, null, null);
			TMAContactWrapper tcw2 = new TMAContactWrapper("eee", "bbb", new HiResDate(330),null, 0,0,0, null, null, null, null);
			TMAContactWrapper tcw3 = new TMAContactWrapper("fff", "bbb", new HiResDate(390),null, 0,0,0, null, null, null, null);
			mw.add(tcw1);
			mw.add(tcw2);
			mw.add(tcw3);
			tw.add(mw);
			
			return tw;
		}

		public void testGetItemsBetween_Second()
		{
			final TrackWrapper tw = new TrackWrapper();

			final WorldLocation loc_1 = new WorldLocation(0, 0, 0);
			final FixWrapper fw1 = new FixWrapper(new Fix(new HiResDate(0, 1), loc_1,
					0, 0));
			final FixWrapper fw2 = new FixWrapper(new Fix(new HiResDate(0, 2), loc_1,
					0, 0));
			final FixWrapper fw3 = new FixWrapper(new Fix(new HiResDate(0, 3), loc_1,
					0, 0));
			final FixWrapper fw4 = new FixWrapper(new Fix(new HiResDate(0, 4), loc_1,
					0, 0));
			final FixWrapper fw5 = new FixWrapper(new Fix(new HiResDate(0, 5), loc_1,
					0, 0));
			final FixWrapper fw6 = new FixWrapper(new Fix(new HiResDate(0, 6), loc_1,
					0, 0));
			final FixWrapper fw7 = new FixWrapper(new Fix(new HiResDate(0, 7), loc_1,
					0, 0));
			tw.addFix(fw1);
			tw.addFix(fw2);
			tw.addFix(fw3);
			tw.addFix(fw4);
			tw.addFix(fw5);
			tw.addFix(fw6);
			tw.addFix(fw7);
			fw1.setLabelShowing(true);
			fw2.setLabelShowing(true);
			fw3.setLabelShowing(true);
			fw4.setLabelShowing(true);
			fw5.setLabelShowing(true);
			fw6.setLabelShowing(true);
			fw7.setLabelShowing(true);

			Collection<Editable> col = tw.getItemsBetween(new HiResDate(0, 3),
					new HiResDate(0, 5));
			assertEquals("found correct number of items", 3, col.size());

			// make the fourth item not visible
			fw4.setVisible(false);

			col = tw.getVisibleItemsBetween(new HiResDate(0, 3), new HiResDate(0, 5));
			assertEquals("found correct number of items", 2, col.size());

			final Watchable[] pts2 = tw.getNearestTo(new HiResDate(0, 3));
			assertEquals("found something", 1, pts2.length);
			assertEquals("found the third item", fw3, pts2[0]);

			final Watchable[] pts = tw.getNearestTo(new HiResDate(0, 1));
			assertEquals("found something", 1, pts.length);
			assertEquals("found the first item", fw1, pts[0]);

		}

		public final void testGettingTimes()
		{
			// Enumeration<SensorContactWrapper>
			final TrackWrapper tw = new TrackWrapper();

			final WorldLocation loc_1 = new WorldLocation(0, 0, 0);
			final WorldLocation loc_2 = new WorldLocation(1, 1, 0);
			final FixWrapper fw1 = new FixWrapper(new Fix(new HiResDate(0, 100),
					loc_1, 0, 0));
			final FixWrapper fw2 = new FixWrapper(new Fix(new HiResDate(0, 300),
					loc_2, 0, 0));
			final FixWrapper fw3 = new FixWrapper(new Fix(new HiResDate(0, 500),
					loc_2, 0, 0));
			final FixWrapper fw4 = new FixWrapper(new Fix(new HiResDate(0, 700),
					loc_2, 0, 0));

			// check returning empty data
			Collection<Editable> coll = tw.getItemsBetween(new HiResDate(0, 0),
					new HiResDate(0, 40));
			assertEquals("Return empty when empty", coll, null);

			tw.addFix(fw1);

			// check returning single field
			coll = tw.getItemsBetween(new HiResDate(0, 0), new HiResDate(0, 40));
			assertEquals("Return empty when out of range", coll, null);

			coll = tw.getItemsBetween(new HiResDate(0, 520), new HiResDate(0, 540));
			assertEquals("Return empty when out of range", coll, null);

			coll = tw.getItemsBetween(new HiResDate(0, 0), new HiResDate(0, 140));
			assertEquals("Return valid point", coll.size(), 1);

			coll = tw.getItemsBetween(new HiResDate(0, 100), new HiResDate(0, 100));
			assertEquals("Return valid point", coll.size(), 1);

			tw.addFix(fw2);

			// check returning with fields
			coll = tw.getItemsBetween(new HiResDate(0, 0), new HiResDate(0, 40));
			assertEquals("Return empty when out of range", coll, null);

			coll = tw.getItemsBetween(new HiResDate(0, 520), new HiResDate(0, 540));
			assertEquals("Return empty when out of range", coll, null);

			coll = tw.getItemsBetween(new HiResDate(0, 0), new HiResDate(0, 140));
			assertEquals("Return valid point", coll.size(), 1);

			coll = tw.getItemsBetween(new HiResDate(0, 0), new HiResDate(0, 440));
			assertEquals("Return valid point", coll.size(), 2);

			coll = tw.getItemsBetween(new HiResDate(0, 150), new HiResDate(0, 440));
			assertEquals("Return valid point", coll.size(), 1);

			coll = tw.getItemsBetween(new HiResDate(0, 300), new HiResDate(0, 440));
			assertEquals("Return valid point", coll.size(), 1);

			tw.addFix(fw3);

			// check returning with fields
			coll = tw.getItemsBetween(new HiResDate(0, 0), new HiResDate(0, 40));
			assertEquals("Return empty when out of range", coll, null);

			coll = tw.getItemsBetween(new HiResDate(0, 520), new HiResDate(0, 540));
			assertEquals("Return empty when out of range", coll, null);

			coll = tw.getItemsBetween(new HiResDate(0, 0), new HiResDate(0, 140));
			assertEquals("Return valid point", coll.size(), 1);

			coll = tw.getItemsBetween(new HiResDate(0, 0), new HiResDate(0, 440));
			assertEquals("Return valid point", coll.size(), 2);

			coll = tw.getItemsBetween(new HiResDate(0, 150), new HiResDate(0, 440));
			assertEquals("Return valid point", coll.size(), 1);

			coll = tw.getItemsBetween(new HiResDate(0, 300), new HiResDate(0, 440));
			assertEquals("Return valid point", coll.size(), 1);

			coll = tw.getItemsBetween(new HiResDate(0, 100), new HiResDate(0, 300));
			assertEquals("Return valid point", coll.size(), 2);

			coll = tw.getItemsBetween(new HiResDate(0, 300), new HiResDate(0, 500));
			assertEquals("Return valid point", coll.size(), 2);

			tw.addFix(fw4);

		}

		public final void testInterpolation()
		{
			final TrackWrapper tw = new TrackWrapper();

			final WorldLocation loc_1 = new WorldLocation(0, 0, 0);
			final FixWrapper fw1 = new FixWrapper(new Fix(new HiResDate(100, 10000),
					loc_1.add(new WorldVector(33, new WorldDistance(100,
							WorldDistance.METRES), null)), 10, 110));
			fw1.setLabel("fw1");
			final FixWrapper fw2 = new FixWrapper(new Fix(new HiResDate(200, 20000),
					loc_1.add(new WorldVector(33, new WorldDistance(200,
							WorldDistance.METRES), null)), 20, 120));
			fw2.setLabel("fw2");
			final FixWrapper fw3 = new FixWrapper(new Fix(new HiResDate(300, 30000),
					loc_1.add(new WorldVector(33, new WorldDistance(300,
							WorldDistance.METRES), null)), 30, 130));
			fw3.setLabel("fw3");
			final FixWrapper fw4 = new FixWrapper(new Fix(new HiResDate(400, 40000),
					loc_1.add(new WorldVector(33, new WorldDistance(400,
							WorldDistance.METRES), null)), 40, 140));
			fw4.setLabel("fw4");
			final FixWrapper fw5 = new FixWrapper(new Fix(new HiResDate(500, 50000),
					loc_1.add(new WorldVector(33, new WorldDistance(500,
							WorldDistance.METRES), null)), 50, 150));
			fw5.setLabel("fw5");
			tw.addFix(fw1);
			tw.addFix(fw2);
			// tw.addFix(fw3);
			tw.addFix(fw4);
			tw.addFix(fw5);

			// check that we're not interpolating
			assertFalse("interpolating switched off by default", tw
					.getInterpolatePoints());

			// ok, get on with it.
			Watchable[] list = tw.getNearestTo(new HiResDate(200, 20000));
			assertNotNull("found list", list);
			assertEquals("contains something", list.length, 1);
			assertEquals("right answer", list[0], fw2);

			// and the end
			list = tw.getNearestTo(new HiResDate(500, 50000));
			assertNotNull("found list", list);
			assertEquals("contains something", list.length, 1);
			assertEquals("right answer", list[0], fw5);

			// and now an in-between point
			// ok, get on with it.
			list = tw.getNearestTo(new HiResDate(230, 23000));
			assertNotNull("found list", list);
			assertEquals("contains something", list.length, 1);
			assertEquals("right answer", list[0], fw4);

			// ok, with interpolation on
			tw.setInterpolatePoints(true);

			assertTrue("interpolating now switched on", tw.getInterpolatePoints());

			// ok, get on with it.
			list = tw.getNearestTo(new HiResDate(200, 20000));
			assertNotNull("found list", list);
			assertEquals("contains something", list.length, 1);
			assertEquals("right answer", list[0], fw2);

			// and the end
			list = tw.getNearestTo(new HiResDate(500, 50000));
			assertNotNull("found list", list);
			assertEquals("contains something", list.length, 1);
			assertEquals("right answer", list[0], fw5);

			// hey

			// and now an in-between point
			// ok, get on with it.
			list = tw.getNearestTo(new HiResDate(300, 30000));
			assertNotNull("found list", list);
			assertEquals("contains something", list.length, 1);

			// have a look at them
			final FixWrapper res = (FixWrapper) list[0];
			final WorldVector rangeError = res.getFixLocation().subtract(
					fw3.getFixLocation());
			assertEquals("right answer", 0,
					Conversions.Degs2m(rangeError.getRange()), 0.0001);
			// assertEquals("right speed", res.getSpeed(), fw3.getSpeed(), 0);
			// assertEquals("right course", res.getCourse(), fw3.getCourse(),
			// 0);

		}

		public final void testMyParams()
		{
			TrackWrapper ed = new TrackWrapper();
			ed.setName("blank");

			editableTesterSupport.testParams(ed, this);
			ed = null;
		}

		public void testPaintingColChange()
		{
			final TrackWrapper tw = new TrackWrapper();
			tw.setColor(Color.RED);
			tw.setName("test track");

			/**
			 * intention of this test: line is broken into three segments (red,
			 * yellow, green). - first of 2 points, next of 2 points, last of 3 points
			 * (14 values)
			 */

			final WorldLocation loc_1 = new WorldLocation(0, 0, 0);
			final FixWrapper fw1 = new FixWrapper(new Fix(new HiResDate(100, 10000),
					loc_1.add(new WorldVector(33, new WorldDistance(100,
							WorldDistance.METRES), null)), 10, 110));
			fw1.setLabel("fw1");
			fw1.setColor(Color.red);
			final FixWrapper fw2 = new FixWrapper(new Fix(new HiResDate(200, 20000),
					loc_1.add(new WorldVector(33, new WorldDistance(200,
							WorldDistance.METRES), null)), 20, 120));
			fw2.setLabel("fw2");
			fw2.setColor(Color.yellow);
			final FixWrapper fw3 = new FixWrapper(new Fix(new HiResDate(300, 30000),
					loc_1.add(new WorldVector(33, new WorldDistance(300,
							WorldDistance.METRES), null)), 30, 130));
			fw3.setLabel("fw3");
			fw3.setColor(Color.green);
			final FixWrapper fw4 = new FixWrapper(new Fix(new HiResDate(400, 40000),
					loc_1.add(new WorldVector(33, new WorldDistance(400,
							WorldDistance.METRES), null)), 40, 140));
			fw4.setLabel("fw4");
			fw4.setColor(Color.green);
			final FixWrapper fw5 = new FixWrapper(new Fix(new HiResDate(500, 50000),
					loc_1.add(new WorldVector(33, new WorldDistance(500,
							WorldDistance.METRES), null)), 50, 150));
			fw5.setLabel("fw5");
			fw5.setColor(Color.green);
			tw.addFix(fw1);
			tw.addFix(fw2);
			tw.addFix(fw3);
			tw.addFix(fw4);
			tw.addFix(fw5);

			callCount = 0;
			pointCount = 0;

			assertNull("our array of points starts empty", tw._myPts);
			assertEquals("our point array counter is zero", tw._ptCtr, 0);

			final CanvasType dummyDest = new TestMockCanvas();

			tw.paint(dummyDest);

			assertEquals("our array has correct number of points", 10,
					tw._myPts.length);
			assertEquals("the pointer counter has been reset", 0, tw._ptCtr);

			// check it got called the correct number of times
			assertEquals("We didnt paint enough polygons", 3, callCount);
			assertEquals("We didnt paint enough polygons points", 14, pointCount);
		}

		public void testPaintingLineJoinedChange()
		{
			final TrackWrapper tw = new TrackWrapper();
			tw.setColor(Color.RED);
			tw.setName("test track");

			/**
			 * intention of this test: line is broken into two segments - one of two
			 * points, the next of three, thus two polygons should be drawn - 10
			 * points total (4 then 6).
			 */

			final WorldLocation loc_1 = new WorldLocation(0, 0, 0);
			final FixWrapper fw1 = new FixWrapper(new Fix(new HiResDate(100, 10000),
					loc_1.add(new WorldVector(33, new WorldDistance(100,
							WorldDistance.METRES), null)), 10, 110));
			fw1.setLabel("fw1");
			fw1.setColor(Color.red);
			final FixWrapper fw2 = new FixWrapper(new Fix(new HiResDate(200, 20000),
					loc_1.add(new WorldVector(33, new WorldDistance(200,
							WorldDistance.METRES), null)), 20, 120));
			fw2.setLabel("fw2");
			fw2.setColor(Color.red);
			final FixWrapper fw3 = new FixWrapper(new Fix(new HiResDate(300, 30000),
					loc_1.add(new WorldVector(33, new WorldDistance(300,
							WorldDistance.METRES), null)), 30, 130));
			fw3.setLabel("fw3");
			fw3.setColor(Color.red);
			fw3.setLineShowing(false);
			final FixWrapper fw4 = new FixWrapper(new Fix(new HiResDate(400, 40000),
					loc_1.add(new WorldVector(33, new WorldDistance(400,
							WorldDistance.METRES), null)), 40, 140));
			fw4.setLabel("fw4");
			fw4.setColor(Color.red);
			final FixWrapper fw5 = new FixWrapper(new Fix(new HiResDate(500, 50000),
					loc_1.add(new WorldVector(33, new WorldDistance(500,
							WorldDistance.METRES), null)), 50, 150));
			fw5.setLabel("fw5");
			fw5.setColor(Color.red);
			tw.addFix(fw1);
			tw.addFix(fw2);
			tw.addFix(fw3);
			tw.addFix(fw4);
			tw.addFix(fw5);

			callCount = 0;
			pointCount = 0;

			assertNull("our array of points starts empty", tw._myPts);
			assertEquals("our point array counter is zero", tw._ptCtr, 0);

			final CanvasType dummyDest = new TestMockCanvas();

			tw.paint(dummyDest);

			assertEquals("our array has correct number of points", 10,
					tw._myPts.length);
			assertEquals("the pointer counter has been reset", 0, tw._ptCtr);

			// check it got called the correct number of times
			assertEquals("We didnt paint enough polygons", 2, callCount);
			assertEquals("We didnt paint enough polygons points", 10, pointCount);

		}

		public void testPaintingVisChange()
		{
			final TrackWrapper tw = new TrackWrapper();
			tw.setColor(Color.RED);
			tw.setName("test track");

			/**
			 * intention of this test: line is broken into two segments of two points,
			 * thus two polygons should be drawn, each with 4 points - 8 points total.
			 */

			final WorldLocation loc_1 = new WorldLocation(0, 0, 0);
			final FixWrapper fw1 = new FixWrapper(new Fix(new HiResDate(100, 10000),
					loc_1.add(new WorldVector(33, new WorldDistance(100,
							WorldDistance.METRES), null)), 10, 110));
			fw1.setLabel("fw1");
			fw1.setColor(Color.red);
			final FixWrapper fw2 = new FixWrapper(new Fix(new HiResDate(200, 20000),
					loc_1.add(new WorldVector(33, new WorldDistance(200,
							WorldDistance.METRES), null)), 20, 120));
			fw2.setLabel("fw2");
			fw2.setColor(Color.red);
			final FixWrapper fw3 = new FixWrapper(new Fix(new HiResDate(300, 30000),
					loc_1.add(new WorldVector(33, new WorldDistance(300,
							WorldDistance.METRES), null)), 30, 130));
			fw3.setLabel("fw3");
			fw3.setColor(Color.red);
			fw3.setVisible(false);
			final FixWrapper fw4 = new FixWrapper(new Fix(new HiResDate(400, 40000),
					loc_1.add(new WorldVector(33, new WorldDistance(400,
							WorldDistance.METRES), null)), 40, 140));
			fw4.setLabel("fw4");
			fw4.setColor(Color.red);
			final FixWrapper fw5 = new FixWrapper(new Fix(new HiResDate(500, 50000),
					loc_1.add(new WorldVector(33, new WorldDistance(500,
							WorldDistance.METRES), null)), 50, 150));
			fw5.setLabel("fw5");
			fw5.setColor(Color.red);
			tw.addFix(fw1);
			tw.addFix(fw2);
			tw.addFix(fw3);
			tw.addFix(fw4);
			tw.addFix(fw5);

			callCount = 0;
			pointCount = 0;

			assertNull("our array of points starts empty", tw._myPts);
			assertEquals("our point array counter is zero", tw._ptCtr, 0);

			final CanvasType dummyDest = new TestMockCanvas();

			tw.paint(dummyDest);

			assertEquals("our array has correct number of points", 10,
					tw._myPts.length);
			assertEquals("the pointer counter has been reset", 0, tw._ptCtr);

			// check it got called the correct number of times
			assertEquals("We didnt paint enough polygons", 2, callCount);
			assertEquals("We didnt paint enough polygons points", 8, pointCount);

		}

		protected static class TestMockCanvas extends MockCanvasType
		{
			public void drawPolyline(int[] points)
			{
				callCount++;
				pointCount += points.length;
			}
		}
	}

	/**
	 * class containing editable details of a track
	 */
	public final class trackInfo extends Editable.EditorType
	{

		/**
		 * constructor for this editor, takes the actual track as a parameter
		 * 
		 * @param data
		 *          track being edited
		 */
		public trackInfo(final TrackWrapper data)
		{
			super(data, data.getName(), "");
		}

		public final MethodDescriptor[] getMethodDescriptors()
		{
			// just add the reset color field first
			final Class<TrackWrapper> c = TrackWrapper.class;

			final MethodDescriptor[] mds =
			{ method(c, "exportThis", null, "Export Shape"),
					method(c, "resetLabels", null, "Reset DTG Labels") };

			return mds;
		}

		public final String getName()
		{
			return super.getName();
		}

		public final PropertyDescriptor[] getPropertyDescriptors()
		{
			try
			{
				final PropertyDescriptor[] res =
				{
						expertProp("SymbolType",
								"the type of symbol plotted for this label", FORMAT),
						legacyProp("DragTrack", "drag the track location"),
						expertProp("LineThickness", "the width to draw this track", FORMAT),
						expertProp("Name", "the track name"),
						expertProp("InterpolatePoints",
								"whether to interpolate points between known data points",
								SPATIAL),
						expertProp("Color", "the track color", FORMAT),
						expertProp("TrackFont", "the track label font", FORMAT),
						expertProp("PositionsLinked", "link the track Positions"),
						expertProp("NameVisible", "show the track label", VISIBILITY),
						expertProp("PositionsVisible", "show individual Positions",
								VISIBILITY),
						expertProp("NameAtStart",
								"whether to show the track name at the start (or end)",
								VISIBILITY),
						expertProp("Visible", "whether the track is visible", VISIBILITY),
						expertLongProp("NameLocation", "relative location of track label",
								MWC.GUI.Properties.LocationPropertyEditor.class),
						expertLongProp("LabelFrequency", "the label frequency",
								MWC.GUI.Properties.TimeFrequencyPropertyEditor.class),
						expertLongProp("SymbolFrequency", "the symbol frequency",
								MWC.GUI.Properties.TimeFrequencyPropertyEditor.class)

				};
				res[0]
						.setPropertyEditorClass(MWC.GUI.Shapes.Symbols.SymbolFactoryPropertyEditor.class);
				res[1]
						.setPropertyEditorClass(Debrief.Tools.Reconstruction.DragTrackEditor.class);
				res[2]
						.setPropertyEditorClass(MWC.GUI.Properties.LineWidthPropertyEditor.class);
				return res;
			} catch (final IntrospectionException e)
			{
				e.printStackTrace();
				return super.getPropertyDescriptors();
			}
		}

	}

}