package org.mwc.debrief.multipath.model;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import flanagan.interpolation.LinearInterpolation;

/** class that loads & store an SVP, providing a mean average calculator
 * 
 * @author ian
 *
 */
public class SVP
{

	public static final String DEEP_FAIL = "SVP doesn't go deep enough";
	public static final String SHALLOW_FAIL = "SVP doesn't go shallow enough";
	double _depths[];
	double _speeds[];

	/** load an SVP from teh specific path
	 * 
	 * @param path where to get it from
	 * @throws NumberFormatException if the numbers aren't legible
	 * @throws IOException if the file can't be found
	 */
	public void load(String path) throws NumberFormatException, IOException
	{
		Vector<Double> values = new Vector<Double>();

		BufferedReader bufRdr = new BufferedReader(new FileReader(path));
		String line = null;

		// read each line of text file
		while ((line = bufRdr.readLine()) != null)
		{
			StringTokenizer st = new StringTokenizer(line, ",");
			while (st.hasMoreTokens())
			{
				String thisE = st.nextToken();
				if (thisE.length() > 0)
					values.add(Double.valueOf(thisE));
			}
		}

		final int numEntries = values.size();
		_depths = new double[numEntries / 2];
		_speeds = new double[numEntries / 2];
		for (int i = 0; i < numEntries; i += 2)
		{
			_depths[i / 2] = values.elementAt(i);
			_speeds[i / 2] = values.elementAt(i + 1);
		}
	}

	/** calculate the weighted average speed between the two depths
	 * 
	 * @param depthOne first depth
	 * @param depthTwo second depth
	 * @return
	 */
	public double getMeanSpeedBetween(double depthOne, double depthTwo)
	{
		double shallowDepth = Math.min(depthOne, depthTwo);
		double deepDepth = Math.max(depthOne, depthTwo);

		LinearInterpolation interp = new LinearInterpolation(_depths, _speeds);

		// ok, first find the point before the shallow depth
		int before = pointBefore(shallowDepth);
		
		// did we find one?
		if(before == -1)
			throw new RuntimeException(SHALLOW_FAIL);

		// now find the point after the deep depth
		int after = pointAfter(deepDepth);
		
		if(after == -1)
			throw new RuntimeException(DEEP_FAIL);

		double runningMean = -1;
		double lastDepth = -1;
		double lastSpeed = -1;

		// sort out the gaps
		for (int i = before; i <= after; i++)
		{
			double thisDepth = _depths[i];
			double thisSpeed = _speeds[i];

			// have we passed our first loop?
			if (lastDepth != -1)
			{
				if (runningMean == -1)
				{
					double travelInThisSeg = thisDepth - shallowDepth;
					double lowerSpeed = interp.interpolate(shallowDepth);
					double upperSpeed = thisSpeed;
					double meanSpeed = (lowerSpeed + upperSpeed) / 2;
					runningMean = meanSpeed * travelInThisSeg;
				}
				else
				{
					// ok, are we in a mid-section?
					if (thisDepth < deepDepth)
					{
						// yes, consume the whole of this section
						double travelInThisSeg = thisDepth - lastDepth;
						double meanSpeed = (thisSpeed + lastSpeed) / 2;
						runningMean += meanSpeed * travelInThisSeg;
					}
					else
					{
						// we must be in the last leg, just consume the portion we need
						double travelInThisSeg = deepDepth - lastDepth;
						double lowerSpeed = lastSpeed;
						double upperSpeed = interp.interpolate(deepDepth);
						double meanSpeed = (lowerSpeed + upperSpeed) / 2;
						runningMean += meanSpeed * travelInThisSeg;
					}
				}
			}
			lastDepth = thisDepth;
			lastSpeed = thisSpeed;
		}

		// ok, now just divide by the total depth travelled
		runningMean = runningMean / (deepDepth - shallowDepth);

		return runningMean;
	}

	/** determine the index of the observation before the specified depth
	 * 
	 * @param depth
	 * @return
	 */
	private int pointBefore(double depth)
	{
		int res = -1;
		int len = _depths.length;
		for (int i = 0; i < len; i++)
		{
			double thisD = _depths[i];
			if (thisD > depth)
			{
				// ok, passed our data value
				break;
			}
			res = i;
		}

		return res;
	}

	/** determine the index of the data point after the supplied depth
	 * 
	 * @param depth
	 * @return
	 */
	private int pointAfter(double depth)
	{
		int res = -1;
		int len = _depths.length;
		for (int i = 0; i < len; i++)
		{
			double thisD = _depths[i];
			if (thisD >= depth)
			{
				res = i;
				break;
			}
		}

		return res;
	}

	// /////////////////////////////////////////////////
	// and the testing goes here
	// /////////////////////////////////////////////////
	public static class SVP_Test extends junit.framework.TestCase
	{
		public void testMean()
		{
			SVP svp = new SVP();

			assertEquals("not got data", null, svp._depths);

			try
			{
				svp.load("src/org/mwc/debrief/multipath/model/test_data.csv");
			}
			catch (NumberFormatException e)
			{
				fail("wrong number format");
			}
			catch (IOException e)
			{
				fail("unable to read lines");
			}
			

			double mean = svp.getMeanSpeedBetween(30, 55);
			assertEquals("correct mean", 1510.125, mean);

			// now for a calc that spans a SVP step
			mean = svp.getMeanSpeedBetween(20, 55);
			assertEquals("correct mean", 1508.42, mean, 0.01);

			// now for a calc that goes from surface
			mean = svp.getMeanSpeedBetween(0, 35);
			assertEquals("correct mean", 1503.03, mean, 0.01);

			// now for a calc with depths reversed
			mean = svp.getMeanSpeedBetween(55, 20);
			assertEquals("correct mean", 1508.42, mean, 0.01);
		}
		
		public void testMissingData()
		{
			SVP svp = new SVP();

			assertEquals("not got data", null, svp._depths);

			try
			{
				svp.load("src/org/mwc/debrief/multipath/model/test_data.csv");
			}
			catch (NumberFormatException e)
			{
				fail("wrong number format");
			}
			catch (IOException e)
			{
				fail("unable to read lines");
			}
			
			// change the first point so we don't have data at zero
			svp._depths[0] = 4;
			
			try{
				svp.getMeanSpeedBetween(0, 22);
				fail("should not have found index");
			}catch(RuntimeException e)
			{
				assertEquals("wrong message provided", SHALLOW_FAIL, e.getMessage());
			}

			try{
				svp.getMeanSpeedBetween(33, 222);
				fail("should not have found index");
			}catch(RuntimeException e)
			{
				assertEquals("wrong message provided", DEEP_FAIL, e.getMessage());
			}

		}


		public void testIndex()
		{
			SVP svp = new SVP();

			assertEquals("not got data", null, svp._depths);

			try
			{
				svp.load("src/org/mwc/debrief/multipath/model/test_data.csv");
			}
			catch (NumberFormatException e)
			{
				fail("wrong number format");
			}
			catch (IOException e)
			{
				fail("unable to read lines");
			}

			assertEquals("got data", 4, svp._depths.length);

			int t1 = svp.pointBefore(0);
			assertEquals("correct depth", 0d, svp._depths[t1]);

			t1 = svp.pointBefore(5);
			assertEquals("correct depth", 0d, svp._depths[t1]);

			t1 = svp.pointBefore(15);
			assertEquals("correct depth", 00d, svp._depths[t1]);

			t1 = svp.pointBefore(35);
			assertEquals("correct depth", 30d, svp._depths[t1]);

			t1 = svp.pointBefore(50);
			assertEquals("correct depth", 40d, svp._depths[t1]);

			t1 = svp.pointAfter(0);
			assertEquals("correct depth", 0d, svp._depths[t1]);

			t1 = svp.pointAfter(5);
			assertEquals("correct depth", 30d, svp._depths[t1]);

			t1 = svp.pointAfter(15);
			assertEquals("correct depth", 30d, svp._depths[t1]);

			t1 = svp.pointAfter(35);
			assertEquals("correct depth", 40d, svp._depths[t1]);

			t1 = svp.pointAfter(50);
			assertEquals("correct depth", 60d, svp._depths[t1]);

			t1 = svp.pointAfter(150);
			assertEquals("correct depth", -1, t1);


		}
	}
}
