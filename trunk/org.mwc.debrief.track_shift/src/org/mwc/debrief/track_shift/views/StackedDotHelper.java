/**
 * 
 */
package org.mwc.debrief.track_shift.views;

import java.awt.Color;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.Vector;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Composite;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.SeriesException;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.mwc.cmap.core.DataTypes.TrackData.TrackManager;

import Debrief.Tools.Tote.WatchableList;
import Debrief.Wrappers.FixWrapper;
import Debrief.Wrappers.SensorContactWrapper;
import Debrief.Wrappers.SensorWrapper;
import Debrief.Wrappers.TrackWrapper;
import Debrief.Wrappers.Track.TrackSegment;
import MWC.GUI.Editable;
import MWC.GUI.ErrorLogger;
import MWC.GUI.JFreeChart.ColouredDataItem;
import MWC.GenericData.HiResDate;
import MWC.GenericData.TimePeriod;
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


	/** sort out data of interest 
	 * 
	 */
	private Vector<Doublet> getDoublets(final TrackWrapper sensorHost,
			final TrackWrapper targetTrack, boolean onlyVis)
	{
		final Vector<Doublet> res = new Vector<Doublet>(0, 1);

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
						// right, get the track segment and fix nearest to this DTG
						Enumeration<Editable> trkData = targetTrack.elements();
						while(trkData.hasMoreElements())
						{
							Editable thisI = trkData.nextElement();
							if(thisI instanceof TrackSegment)
							{
								TrackSegment ts = (TrackSegment) thisI;
								TimePeriod validPeriod = new TimePeriod.BaseTimePeriod(ts.startDTG(), ts.endDTG());
								if(validPeriod.contains(scw.getDTG()))
								{
									// sorted. here we go
									targetParent = ts;
									
									// and the child element
									FixWrapper index = new FixWrapper(new Fix(scw.getDTG(), new WorldLocation(0,0,0), 0.0, 0.0));
									SortedSet<Editable> items = ts.tailSet(index);
									targetFix = (FixWrapper) items.first();
								}
							}
						}
						
						if (targetFix != null)
						{
							// ok. found match. store it
							final Doublet thisDub = new Doublet(scw, targetFix, targetParent);
							res.add(thisDub);
						}
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
	 * @param holder
	 * @param logger
	 * 
	 * @param currentOffset
	 *          how far the current track has been dragged
	 */
	public void updateBearingData(XYPlot dotPlot, XYPlot linePlot,
			TrackManager tracks, boolean onlyVis, Composite holder, ErrorLogger logger)
	{
		// ok, find the track wrappers
		if (_secondaryTrack == null)
			initialise(tracks, false, onlyVis, holder, logger, "Bearing");

		// did it work?
		if (_secondaryTrack == null)
			return;

		if (_primaryDoublets == null)
			return;

		// ok - the tracks have moved. better update the doublets
		updateDoublets(onlyVis);

		// create the collection of series
		final TimeSeriesCollection errorSeries = new TimeSeriesCollection();
		final TimeSeriesCollection actualSeries = new TimeSeriesCollection();

		// produce a dataset for each track
		final TimeSeries errorValues = new TimeSeries(_primaryTrack.getName());

		final TimeSeries measuredValues = new TimeSeries("Measured");
		final TimeSeries calculatedValues = new TimeSeries("Calculated");

		// ok, run through the points on the primary track
		Iterator<Doublet> iter = _primaryDoublets.iterator();
		while (iter.hasNext())
		{
			final Doublet thisD = iter.next();

			final Color thisColor = thisD.getColor();
			final double measuredBearing = thisD.getMeasuredBearing();
			final double calculatedBearing = thisD.getCalculatedBearing(null, null);
			final double thisError = thisD.calculateError(measuredBearing,
					calculatedBearing);
			final HiResDate currentTime = thisD.getDTG();

			// create a new, correctly coloured data item
			// HI-RES NOT DONE - should provide FixedMicrosecond structure
			final ColouredDataItem newError = new ColouredDataItem(
					new FixedMillisecond(currentTime.getDate().getTime()), thisError,
					thisColor, false, null);

			final ColouredDataItem mBearing = new ColouredDataItem(
					new FixedMillisecond(currentTime.getDate().getTime()),
					measuredBearing, thisColor, false, null);

			final ColouredDataItem cBearing = new ColouredDataItem(
					new FixedMillisecond(currentTime.getDate().getTime()),
					calculatedBearing, thisColor, true, null);

			try
			{
				// and add them to the series
				errorValues.add(newError);
				measuredValues.add(mBearing);
				calculatedValues.add(cBearing);
			}
			catch (final SeriesException e)
			{
				// hack: we shouldn't be allowing this exception. Look at why we're
				// getting the same
				// time period being entered twice for this track.

				// Stop catching the error, load Dave W's holistic approach plot file,
				// and check the track/fix which is causing the problem.

				// e.printStackTrace(); //To change body of catch statement use File |
				// Settings | File Templates.
			}

		}

		// ok, add these new series
		errorSeries.addSeries(errorValues);

		actualSeries.addSeries(measuredValues);
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
			// showMessage("Sorry, a Debrief plot must be selected", showError);
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
			logger.logError(IStatus.INFO,
					"A secondary track must be present on the tote", null);
			return;
		}

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

		if (_primaryTrack.getSensors() == null)
		{
			logger
					.logError(IStatus.INFO, "There must be sensor data available", null);
			return;
		}

		// must have worked, hooray
		logger.logError(IStatus.INFO, dataType + " error", null);

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
		if (_primaryTrack != null && _secondaryTrack != null)
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
			TrackManager tracks, boolean onlyVis, Composite holder, ErrorLogger logger)
	{
		// ok, find the track wrappers
		if (_secondaryTrack == null)
			initialise(tracks, false, onlyVis, holder, logger, "Frequency");

		// did it work?
		if (_secondaryTrack == null)
			return;

		if (_primaryDoublets == null)
			return;

		// ok - the tracks have moved. better update the doublets
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

			final Color thisColor = thisD.getColor();
			final double measuredFreq = thisD.getMeasuredFrequency();
			final double correctedFreq = thisD.getCorrectedFrequency();
			final double predictedFreq = thisD.getPredictedFrequency();
			final double baseFreq = thisD.getBaseFrequency();
			final double thisError = thisD.calculateError(measuredFreq,
					predictedFreq);
			final HiResDate currentTime = thisD.getDTG();

			// create a new, correctly coloured data item
			// HI-RES NOT DONE - should provide FixedMicrosecond structure
			final ColouredDataItem eFreq = new ColouredDataItem(
					new FixedMillisecond(currentTime.getDate().getTime()), thisError,
					thisColor, false, null);

			final ColouredDataItem mFreq = new ColouredDataItem(
					new FixedMillisecond(currentTime.getDate().getTime()),
					measuredFreq, thisColor, false, null);

			final ColouredDataItem corrFreq = new ColouredDataItem(
					new FixedMillisecond(currentTime.getDate().getTime()),
					correctedFreq, thisColor, false, null);

			final ColouredDataItem bFreq = new ColouredDataItem(
					new FixedMillisecond(currentTime.getDate().getTime()),
					baseFreq, thisColor, true, null);

			final ColouredDataItem pFreq = new ColouredDataItem(
					new FixedMillisecond(currentTime.getDate().getTime()),
					predictedFreq, thisColor, false, null);
			
			try
			{
				// and add them to the series
				errorValues.add(eFreq);
				measuredValues.add(mFreq);
				baseValues.add(bFreq);
				correctedValues.add(corrFreq);
				predictedValues.add(pFreq);				
				
			}
			catch (final SeriesException e)
			{
				// hack: we shouldn't be allowing this exception. Look at why we're
				// getting the same
				// time period being entered twice for this track.

				// Stop catching the error, load Dave W's holistic approach plot file,
				// and check the track/fix which is causing the problem.

				// e.printStackTrace(); //To change body of catch statement use File |
				// Settings | File Templates.
			}

		}

		// ok, add these new series
		errorSeries.addSeries(errorValues);

		actualSeries.addSeries(measuredValues);
		actualSeries.addSeries(correctedValues);
		actualSeries.addSeries(predictedValues);
		actualSeries.addSeries(baseValues);

		dotPlot.setDataset(errorSeries);
		linePlot.setDataset(actualSeries);
	}

}