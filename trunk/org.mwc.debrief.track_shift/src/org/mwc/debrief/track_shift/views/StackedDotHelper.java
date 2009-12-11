/**
 * 
 */
package org.mwc.debrief.track_shift.views;

import java.awt.Color;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.Vector;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.core.DataTypes.TrackData.TrackManager;

import Debrief.Wrappers.FixWrapper;
import Debrief.Wrappers.SensorContactWrapper;
import Debrief.Wrappers.SensorWrapper;
import Debrief.Wrappers.TrackWrapper;
import Debrief.Wrappers.Track.TrackSegment;
import Debrief.Wrappers.Track.TrackWrapper_Support.SegmentList;
import MWC.GUI.Editable;
import MWC.GUI.ErrorLogger;
import MWC.GUI.JFreeChart.ColouredDataItem;
import MWC.GenericData.HiResDate;
import MWC.GenericData.TimePeriod;
import MWC.GenericData.WatchableList;
import MWC.GenericData.WorldLocation;
import MWC.TacticalData.Fix;

public final class StackedDotHelper
{
	/**
	 * the track being dragged
	 */
	private TrackWrapper _primaryTrack;

	/**
	 * the secondary track we're monitoring
	 */
	private TrackWrapper _secondaryTrack;

	/**
	 * the set of points to watch on the primary track
	 */
	private Vector<Doublet> _primaryDoublets;

	// ////////////////////////////////////////////////
	// CONSTRUCTOR
	// ////////////////////////////////////////////////

	// ////////////////////////////////////////////////
	// MEMBER METHODS
	// ////////////////////////////////////////////////

	/**
	 * sort out data of interest
	 * 
	 */
	private Vector<Doublet> getDoublets(final TrackWrapper sensorHost,
			final TrackWrapper targetTrack, boolean onlyVis)
	{
		final Vector<Doublet> res = new Vector<Doublet>(0, 1);

		// friendly fix-wrapper to save us repeatedly creating it
		FixWrapper index = new FixWrapper(new Fix(null, new WorldLocation(0, 0, 0),
				0.0, 0.0));

		// loop through our sensor data
		Enumeration<SensorWrapper> sensors = sensorHost.getSensors();
		while (sensors.hasMoreElements())
		{
			SensorWrapper wrapper = sensors.nextElement();
			if (!onlyVis || (onlyVis && wrapper.getVisible()))
			{
				Enumeration<Editable> cuts = wrapper.elements();
				while (cuts.hasMoreElements())
				{
					SensorContactWrapper scw = (SensorContactWrapper) cuts.nextElement();
					if (!onlyVis || (onlyVis && scw.getVisible()))
					{
						FixWrapper targetFix = null;
						TrackSegment targetParent = null;

						if (targetTrack != null)
						{
							// right, get the track segment and fix nearest to
							// this
							// DTG
							Enumeration<Editable> trkData = targetTrack.elements();
							Vector<TrackSegment> _theSegments = new Vector<TrackSegment>();

							while (trkData.hasMoreElements())
							{

								Editable thisI = trkData.nextElement();
								if (thisI instanceof SegmentList)
								{
									SegmentList thisList = (SegmentList) thisI;
									Enumeration<Editable> theElements = thisList.elements();
									while (theElements.hasMoreElements())
									{
										TrackSegment ts = (TrackSegment) theElements.nextElement();
										_theSegments.add(ts);
									}

								}
								if (thisI instanceof TrackSegment)
								{
									TrackSegment ts = (TrackSegment) thisI;
									_theSegments.add(ts);
								}
							}

							if (_theSegments.size() > 0)
							{
								Iterator<TrackSegment> iter = _theSegments.iterator();
								while (iter.hasNext())
								{
									TrackSegment ts = iter.next();

									TimePeriod validPeriod = new TimePeriod.BaseTimePeriod(ts
											.startDTG(), ts.endDTG());
									if (validPeriod.contains(scw.getDTG()))
									{
										// sorted. here we go
										targetParent = ts;

										// create an object with the right time
										index.getFix().setTime(scw.getDTG());

										// and find any matching items
										SortedSet<Editable> items = ts.tailSet(index);
										targetFix = (FixWrapper) items.first();
									}

								}
							}
						}

						FixWrapper hostFix = (FixWrapper) sensorHost.getNearestTo(scw
								.getDTG())[0];

						final Doublet thisDub = new Doublet(scw, targetFix, targetParent,
								hostFix);

						// if we've no target track add all the points
						if (targetTrack == null)
						{
							// store our data
							res.add(thisDub);
						}
						else
						{
							// if we've got a target track we only add points
							// for which we
							// have
							// a target location
							if (targetFix != null)
							{
								// store our data
								res.add(thisDub);
							}
						}
						// if we find a match
					} // if cut is visible
				} // loop through cuts
			} // if sensor is visible
		} // loop through sensors

		return res;
	}

	/**
	 * ok, our track has been dragged, calculate the new series of offsets
	 * 
	 * @param linePlot
	 * @param dotPlot
	 * @param onlyVis
	 * @param showCourse
	 * @param holder
	 * @param logger
	 * 
	 * @param currentOffset
	 *          how far the current track has been dragged
	 */
	public void updateBearingData(XYPlot dotPlot, XYPlot linePlot,
			TrackManager tracks, boolean onlyVis, boolean showCourse,
			Composite holder, ErrorLogger logger, boolean updateDoublets)
	{
		// do we even have a primary track
		if(_primaryTrack == null)
			return;
		
		// ok, find the track wrappers
		if (_secondaryTrack == null)
			initialise(tracks, false, onlyVis, holder, logger, "Bearing");

		// did it work?
		// if (_secondaryTrack == null)
		// return;

		if (_primaryDoublets == null)
			return;
		
		// and check there's actually something in the re
		if(_primaryDoublets.size() == 0)
			return;

		// ok - the tracks have moved. better update the doublets
		if (updateDoublets)
			updateDoublets(onlyVis);

		// create the collection of series
		final TimeSeriesCollection errorSeries = new TimeSeriesCollection();
		final TimeSeriesCollection actualSeries = new TimeSeriesCollection();

		// produce a dataset for each track
		final TimeSeries errorValues = new TimeSeries(_primaryTrack.getName());

		final TimeSeries measuredValues = new TimeSeries("Measured");
		final TimeSeries ambigValues = new TimeSeries("Ambiguous Bearing");
		final TimeSeries calculatedValues = new TimeSeries("Calculated");

		final TimeSeries osCourseValues = new TimeSeries("Course");
		
		// ok, run through the points on the primary track
		Iterator<Doublet> iter = _primaryDoublets.iterator();
		while (iter.hasNext())
		{
			final Doublet thisD = iter.next();

			try
			{

				// obvious stuff first (stuff that doesn't need the tgt data)
				final Color thisColor = thisD.getColor();
				final double measuredBearing = thisD.getMeasuredBearing();
				final double ambigBearing = thisD.getAmbiguousMeasuredBearing();
				final HiResDate currentTime = thisD.getDTG();
				final FixedMillisecond thisMilli = new FixedMillisecond(currentTime.getDate().getTime());
				
				final ColouredDataItem mBearing = new ColouredDataItem(
						thisMilli,
						measuredBearing, thisColor, false, null);


				// and add them to the series
				measuredValues.add(mBearing);

				if (ambigBearing != Doublet.INVALID_BASE_FREQUENCY)
				{
					final ColouredDataItem amBearing = new ColouredDataItem(
							thisMilli,
							ambigBearing, thisColor, false, null);
					ambigValues.add(amBearing);
				}

				// do we have target data?
				if (thisD.getTarget() != null)
				{
					final double calculatedBearing = thisD.getCalculatedBearing(null,
							null);
					final Color calcColor = thisD.getTarget().getColor();
					final double thisError = thisD.calculateBearingError(measuredBearing,
							calculatedBearing);
					final ColouredDataItem newError = new ColouredDataItem(
							thisMilli, thisError,
							thisColor, false, null);

					final ColouredDataItem cBearing = new ColouredDataItem(
							thisMilli,
							calculatedBearing, calcColor, true, null);

					errorValues.add(newError);
					calculatedValues.add(cBearing);
				}

			}
			catch (final SeriesException e)
			{
				CorePlugin.logError(Status.INFO,
						"some kind of trip whilst updating bearing plot", e);
			}

		}

		
		// right, we do course in a special way, since it isn't dependent on the target track. Do course here.
		HiResDate startDTG, endDTG;
		startDTG = _primaryDoublets.firstElement().getDTG();
		endDTG = _primaryDoublets.lastElement().getDTG();
		Collection<Editable> hostFixes = _primaryTrack.getItemsBetween(startDTG, endDTG);
		
		// loop through th items
		for (Iterator<Editable> iterator = hostFixes.iterator(); iterator.hasNext();) {
			Editable editable = (Editable) iterator.next();
			FixWrapper fw = (FixWrapper) editable;
			final FixedMillisecond thisMilli = new FixedMillisecond(fw.getDateTimeGroup().getDate().getTime());
			final double ownshipCourse = MWC.Algorithms.Conversions.Rads2Degs(fw.getCourse());
			final ColouredDataItem crseBearing = new ColouredDataItem(
					thisMilli,
					ownshipCourse, fw.getColor(), true, null);
			osCourseValues.add(crseBearing);
		}
		
		// ok, add these new series

		if (showCourse)
		{
			actualSeries.addSeries(osCourseValues);
		}

		if (errorValues.getItemCount() > 0)
			errorSeries.addSeries(errorValues);

		actualSeries.addSeries(measuredValues);

		if (ambigValues.getItemCount() > 0)
			actualSeries.addSeries(ambigValues);

		if (calculatedValues.getItemCount() > 0)
			actualSeries.addSeries(calculatedValues);

		dotPlot.setDataset(errorSeries);
		linePlot.setDataset(actualSeries);
	}

	/**
	 * initialise the data, check we've got sensor data & the correct number of
	 * visible tracks
	 * 
	 * @param showError
	 * @param onlyVis
	 * @param holder
	 */
	void initialise(TrackManager tracks, boolean showError, boolean onlyVis,
			Composite holder, ErrorLogger logger, String dataType)
	{

		// have we been created?
		if (holder == null)
			return;

		// are we visible?
		if (holder.isDisposed())
			return;

		_secondaryTrack = null;
		_primaryTrack = null;

		// do we have some data?
		if (tracks == null)
		{
			// output error message
			logger.logError(IStatus.INFO, "Please open a Debrief plot", null);
			return;
		}

		// check we have a primary track
		final WatchableList priTrk = tracks.getPrimaryTrack();
		if (priTrk == null)
		{
			logger.logError(IStatus.INFO,
					"A primary track must be placed on the Tote", null);
			return;
		}
		else
		{
			if (!(priTrk instanceof TrackWrapper))
			{
				logger.logError(IStatus.INFO,
						"The primary track must be a vehicle track", null);
				return;
			}
			else
				_primaryTrack = (TrackWrapper) priTrk;
		}

		// now the sec track
		final WatchableList[] secs = tracks.getSecondaryTracks();

		// any?
		if ((secs == null) || (secs.length == 0))
		{
		}
		else
		{
			// too many?
			if (secs.length > 1)
			{
				logger.logError(IStatus.INFO,
						"Only 1 secondary track may be on the tote", null);
				return;
			}

			// correct sort?
			final WatchableList secTrk = secs[0];
			if (!(secTrk instanceof TrackWrapper))
			{
				logger.logError(IStatus.INFO,
						"The secondary track must be a vehicle track", null);
				return;
			}
			else
			{
				_secondaryTrack = (TrackWrapper) secTrk;
			}

		}

		if (_primaryTrack.getSensors() == null)
		{
			logger
					.logError(IStatus.INFO, "There must be sensor data available", null);
			return;
		}

		// must have worked, hooray
		logger.logError(IStatus.OK, dataType + " error", null);

		// ok, get the positions
		updateDoublets(onlyVis);

	}

	/**
	 * clear our data, all is finished
	 */
	public void reset()
	{
		if (_primaryDoublets != null)
			_primaryDoublets.removeAllElements();
		_primaryDoublets = null;
		_primaryTrack = null;
		_secondaryTrack = null;
	}

	/**
	 * go through the tracks, finding the relevant position on the other track.
	 * 
	 */
	private void updateDoublets(boolean onlyVis)
	{
		// ok - we're now there
		// so, do we have primary and secondary tracks?
		if (_primaryTrack != null)
		{
			// cool sort out the list of sensor locations for these tracks
			_primaryDoublets = getDoublets(_primaryTrack, _secondaryTrack, onlyVis);
		}
	}

	/**
	 * ok, our track has been dragged, calculate the new series of offsets
	 * 
	 * @param linePlot
	 * @param dotPlot
	 * @param onlyVis
	 * @param holder
	 * @param logger
	 * 
	 * @param currentOffset
	 *          how far the current track has been dragged
	 */
	public void updateFrequencyData(XYPlot dotPlot, XYPlot linePlot,
			TrackManager tracks, boolean onlyVis, Composite holder,
			ErrorLogger logger, boolean updateDoublets)
	{
		// ok, find the track wrappers
		if (_secondaryTrack == null)
			initialise(tracks, false, onlyVis, holder, logger, "Frequency");

		// did it work?

		if (_primaryDoublets == null)
			return;

		// ok - the tracks have moved. better update the doublets
		if (updateDoublets)
			updateDoublets(onlyVis);

		// create the collection of series
		final TimeSeriesCollection errorSeries = new TimeSeriesCollection();
		final TimeSeriesCollection actualSeries = new TimeSeriesCollection();

		// produce a dataset for each track
		final TimeSeries errorValues = new TimeSeries(_primaryTrack.getName());

		final TimeSeries measuredValues = new TimeSeries("Measured");
		final TimeSeries correctedValues = new TimeSeries("Corrected");
		final TimeSeries predictedValues = new TimeSeries("Predicted");
		final TimeSeries baseValues = new TimeSeries("Base");

		// ok, run through the points on the primary track
		Iterator<Doublet> iter = _primaryDoublets.iterator();
		while (iter.hasNext())
		{
			final Doublet thisD = iter.next();
			try
			{

				final Color thisColor = thisD.getColor();
				final double measuredFreq = thisD.getMeasuredFrequency();
				final HiResDate currentTime = thisD.getDTG();
				final FixedMillisecond thisMilli = new FixedMillisecond(currentTime.getDate().getTime());
				
				final ColouredDataItem mFreq = new ColouredDataItem(
						thisMilli,
						measuredFreq, thisColor, false, null);

				// final ColouredDataItem corrFreq = new ColouredDataItem(
				// new FixedMillisecond(currentTime.getDate().getTime()),
				// correctedFreq, thisColor, false, null);
				measuredValues.add(mFreq);

				// do we have target data?
				if (thisD.getTarget() != null)
				{
					final double correctedFreq = thisD.getCorrectedFrequency();
					final double baseFreq = thisD.getBaseFrequency();
					final Color calcColor = thisD.getTarget().getColor();

					final ColouredDataItem corrFreq = new ColouredDataItem(
							thisMilli,
							correctedFreq, thisColor, true, null);

					// did we get a base frequency? We may have a track
					// with a section of data that doesn't have frequency, you see.
					if (baseFreq != Doublet.INVALID_BASE_FREQUENCY)
					{
						final double predictedFreq = thisD.getPredictedFrequency();
						final double thisError = thisD.calculateFreqError(measuredFreq,
								predictedFreq);
						final ColouredDataItem bFreq = new ColouredDataItem(
								thisMilli,
								baseFreq, thisColor, true, null);
						final ColouredDataItem pFreq = new ColouredDataItem(
								thisMilli,
								predictedFreq, calcColor, false, null);
						final ColouredDataItem eFreq = new ColouredDataItem(
								thisMilli,
								thisError, thisColor, false, null);
						baseValues.add(bFreq);
						predictedValues.add(pFreq);
						errorValues.add(eFreq);
					}

					correctedValues.add(corrFreq);
				}

			}
			catch (final SeriesException e)
			{
				CorePlugin.logError(Status.INFO,
						"some kind of trip whilst updating frequency plot", e);
			}

		}

		// ok, add these new series
		if (errorValues.getItemCount() > 0)
			errorSeries.addSeries(errorValues);

		actualSeries.addSeries(measuredValues);
		actualSeries.addSeries(correctedValues);

		if (predictedValues.getItemCount() > 0)
			actualSeries.addSeries(predictedValues);
		if (baseValues.getItemCount() > 0)
			actualSeries.addSeries(baseValues);

		dotPlot.setDataset(errorSeries);
		linePlot.setDataset(actualSeries);
	}

}
