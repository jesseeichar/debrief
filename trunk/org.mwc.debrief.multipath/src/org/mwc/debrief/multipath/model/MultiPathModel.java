package org.mwc.debrief.multipath.model;

import java.awt.Color;
import java.io.IOException;
import java.util.Iterator;

import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.mwc.debrief.multipath.model.TimeDeltas.Observation;

import Debrief.Wrappers.FixWrapper;
import Debrief.Wrappers.LabelWrapper;
import Debrief.Wrappers.TrackWrapper;
import MWC.GenericData.HiResDate;
import MWC.GenericData.Watchable;
import MWC.GenericData.WatchableList;
import MWC.GenericData.WorldLocation;
import MWC.GenericData.WorldVector;
import MWC.TacticalData.Fix;

public class MultiPathModel
{
	/**
	 * get the measured profile
	 * 
	 * @param deltas
	 * @return a time series of measured time delays
	 */
	public TimeSeries getMeasuredProfileFor(TimeDeltas deltas)
	{
		TimeSeries res = new TimeSeries("Measured delay");

		// ok, loop through the times
		Iterator<Observation> iter = deltas.iterator();
		while (iter.hasNext())
		{
			Observation thisO = iter.next();

			// what's this time?
			HiResDate tNow = thisO.getDate();
			double delay = thisO.getInterval();

			// and add it
			res.add(new FixedMillisecond(tNow.getDate().getTime()), delay);
		}

		return res;
	}

	/**
	 * get the calculated profile
	 * 
	 * @param primary
	 *          the primary track
	 * @param secondary
	 *          the secondary track
	 * @param svp
	 *          the sound speed profile
	 * @param deltas
	 *          the series of time-delta observations
	 * @param targetDepth
	 *          target depth to experiment with
	 * @return a time series of calculated time delays
	 */
	public TimeSeries getCalculatedProfileFor(WatchableList primary,
			WatchableList secondary, SVP svp, TimeDeltas deltas, double targetDepth)
	{
		Boolean oldPrimaryInterp = null;
		Boolean oldSecondaryInterp = null;

		TimeSeries res = new TimeSeries("Calculated delay");

		if (primary instanceof TrackWrapper)
		{
			TrackWrapper trk = (TrackWrapper) primary;
			oldPrimaryInterp = trk.getInterpolatePoints();
			trk.setInterpolatePoints(true);
		}
		if (secondary instanceof TrackWrapper)
		{
			TrackWrapper trk = (TrackWrapper) secondary;
			oldSecondaryInterp = trk.getInterpolatePoints();
			trk.setInterpolatePoints(true);
		}

		// ok, loop through the times
		Iterator<Observation> iter = deltas.iterator();
		while (iter.hasNext())
		{
			Observation thisO = iter.next();

			// what's this time?
			HiResDate tNow = thisO.getDate();

			// find the locations
			Watchable[] priLocs = primary.getNearestTo(tNow);
			Watchable[] secLocs = secondary.getNearestTo(tNow);

			// do we have data
			if (priLocs.length == 0)
				throw new RuntimeException("Insufficient primary data");
			if (secLocs.length == 0)
				throw new RuntimeException("Insufficient secondary data");

			WorldLocation priLoc = priLocs[0].getLocation();
			WorldLocation secLoc = secLocs[0].getLocation();
			WorldVector sep = priLoc.subtract(secLoc);
			double sepM = MWC.Algorithms.Conversions.Degs2m(sep.getRange());

			double zR = priLoc.getDepth();
			double zS = targetDepth;

			// now sort out the sound speeds
			double cD = svp.getMeanSpeedBetween(zS, zR);
			double cS = svp.getMeanSpeedBetween(0, zS);
			double cR = svp.getMeanSpeedBetween(0, zR);

			// do the actual calculation
			double time_delay = calculateDelay(sepM, zR, zS, cD, cS, cR);

			res.add(new FixedMillisecond(tNow.getDate().getTime()), time_delay);
		}

		// restore the interpolation settings
		if (oldPrimaryInterp != null)
		{
			TrackWrapper trk = (TrackWrapper) primary;
			trk.setInterpolatePoints(oldPrimaryInterp.booleanValue());
		}
		if (oldSecondaryInterp != null)
		{
			TrackWrapper trk = (TrackWrapper) secondary;
			trk.setInterpolatePoints(oldSecondaryInterp.booleanValue());
		}

		return res;
	}

	private double calculateDelay(double sepM, double zR, double zS, double cD,
			double cS, double cR)
	{
		double hD = sepM;
		double lDirect = Math.sqrt(hD * hD + Math.pow(zS - zR, 2));

		double lsHoriz = (hD * zS) / (zS + zR);
		double lS = Math.sqrt(lsHoriz * lsHoriz + zS * zS);
		double lR = Math.sqrt(Math.pow(hD - lsHoriz, 2) + zR * zR);

		double time_delay = (lS / cS + lR / cR) - (lDirect / cD);
		return time_delay;
	}

	// /////////////////////////////////////////////////
	// and the testing goes here
	// /////////////////////////////////////////////////
	public static class IntervalTest extends junit.framework.TestCase
	{

		public void testMe()
		{
			WorldLocation loc = new WorldLocation(2, 2, 30);
			LabelWrapper primary = new LabelWrapper("Sensor", loc, Color.red);

			TrackWrapper secondary = new TrackWrapper();

			TimeDeltas deltas = getDeltas();

			HiResDate start = deltas.getStartTime();
			HiResDate end = deltas.getEndTime();
			long interval = 10000;

			WorldLocation startLoc = loc.add(new WorldVector(Math.PI, 0.02, 0));

			for (long thisT = start.getDate().getTime(); thisT <= end.getDate()
					.getTime() + interval; thisT += interval)
			{
				HiResDate thisD = new HiResDate(thisT);
				WorldLocation newLoc = new WorldLocation(startLoc.add(new WorldVector(
						Math.PI * 1.5, 0.005, 0)));
				Fix newF = new Fix(thisD, newLoc, 0, 0);
				FixWrapper newFw = new FixWrapper(newF);
				secondary.addFix(newFw);

				startLoc = newLoc;
			}
			
			SVP svp = getSVP();

			// ok, go for the calc
			MultiPathModel model = new MultiPathModel();
			TimeSeries calc = model.getCalculatedProfileFor(primary, secondary, svp, deltas, 44);
			
			assertNotNull("got some results",calc);
		}

		private SVP getSVP()
		{
			SVP svp = new SVP();
			try
			{
				svp.load(SVP.SVP_Test.SVP_FILE);
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
				fail("number format");
			}
			catch (IOException e)
			{
				e.printStackTrace();
				fail("file read problem");
			}
			return svp;
		}

		private static TimeDeltas getDeltas()
		{
			TimeDeltas deltas = new TimeDeltas();
			try
			{
				deltas.load(TimeDeltas.IntervalTest.TEST_TIMES_FILE);
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
				fail("number format problem");
			}
			catch (IOException e)
			{
				e.printStackTrace();
				fail("file read problem");
			}
			return deltas;
		}
	}
}
