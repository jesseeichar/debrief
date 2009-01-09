// Copyright MWC 1999, Debrief 3 Project
// $RCSfile: ImportPMRFFix.java,v $
// @author $Author: Ian.Mayo $
// @version $Revision: 1.3 $
// $Log: ImportPMRFFix.java,v $
// Revision 1.3  2005/12/13 09:04:33  Ian.Mayo
// Tidying - as recommended by Eclipse
//
// Revision 1.2  2004/11/25 10:24:13  Ian.Mayo
// Switch to Hi Res dates
//
// Revision 1.1.1.2  2003/07/21 14:47:40  Ian.Mayo
// Re-import Java files to keep correct line spacing
//
// Revision 1.3  2003-03-19 15:37:30+00  ian_mayo
// improvements according to IntelliJ inspector
//
// Revision 1.2  2002-05-28 12:28:09+01  ian_mayo
// after update
//
// Revision 1.1  2002-05-28 09:12:13+01  ian_mayo
// Initial revision
//
// Revision 1.1  2002-04-23 12:29:45+01  ian_mayo
// Initial revision
//
// Revision 1.1  2002-03-28 11:48:53+00  administrator
// More tidily handle switch to next day
//
// Revision 1.0  2001-07-17 08:41:35+01  administrator
// Initial revision
//
// Revision 1.1  2001-01-03 13:40:48+00  novatech
// Initial revision
//
// Revision 1.1.1.1  2000/12/12 20:46:50  ianmayo
// initial import of files
//
// Revision 1.2  2000-06-06 12:42:47+01  ian_mayo
// handling passing through midnight
//
// Revision 1.1  2000-06-05 14:20:33+01  ian_mayo
// Initial revision
//
package Debrief.ReaderWriter.PCArgos;

import java.util.*;

import Debrief.ReaderWriter.Replay.ReplayFix;
import MWC.GenericData.*;
import MWC.TacticalData.Fix;
import MWC.Utilities.ReaderWriter.PlainLineImporter;

/** import a fix from a line of text (in PCArgos format)
 */
final class ImportPMRFFix implements PlainLineImporter
{

	/** header information necessary for range data offsets
	 */
	private WorldLocation _origin = null;
	private long _dtg = 0;
	private Hashtable<String, Fix> _lastPoints = null;
	private long _freq = 0;

	/** keep track of DTG header lines (since some lines omit this)
	 */
	static long _lastDTG;

  /** the (-ve) time interval we see to decide that a successive data point is in fact
   * from the previous day
   */
  private final static long NEXT_DAY_INTERVAL = 1 * 60 * 60 * 1000; // 1 hours in millis

	/** initialise our parameters
	 */
	public final void setParameters(WorldLocation origin,
														long DTG,
														Hashtable<String, Fix> lastPoints,
														long freq)
	{
		_origin = origin;
		_dtg = DTG;
		_lastPoints = lastPoints;
		_freq = freq;
	}
	
	
  /**
   */
	public final Object readThisLine(String theLine)
	{

    // declare local variables
    WorldLocation theLoc;
		double x;
		double y;
		double z;

		StringTokenizer st = new StringTokenizer(theLine);
		
		// find out which type of line this is.
		int fields = st.countTokens();
		
		if(fields == 13)
		{
			
			// this is a header line, which includes the DTG
			// 2 0 72000 36000 2001001 -331.67 40823.67 -180.67 -182.39 -3.15 -3.43 0.04 8.28			
			
			// extract the DTG info
			st.nextToken(); // the 2
			st.nextToken();  // the 0
			String seconds = st.nextToken(); // the elapsed seconds
			
			// extract secs from this
			long elapsed = Long.valueOf(seconds).longValue();
			
			// convert this value to millis
			elapsed *= 1000;
			
			// add these secs to the DTG (we will use this for this fix, and for
			// subsequent "short" lines
			long this_time = _dtg + elapsed;
			
			
			// see if we have passed through midnight, i.e. if
			// the elapsed time has decreased
			if((_lastDTG - this_time) > NEXT_DAY_INTERVAL)
			{
				// ooh, we must have stepped back in time.  No, let's assume
				// that this new DTG is for the next day, by adding
				// 24 hours (as millis) to the dtg

        // move the date offset to the next day
        _dtg += 1 * 24 * 60 * 60 * 1000;

        // and recalculate this time
        this_time = _dtg + elapsed;
			}
			
			// ok, our time value is now valid
			_lastDTG = this_time;
			
			st.nextToken(); // the other elapsed time

		}				
		
		// get the track name
		String trk = st.nextToken();

		///////////////////////////////////////////////////
		// find out if we are interested in this data point
		///////////////////////////////////////////////////
		// what is the last dtg for this track?
		Fix fx = (Fix)_lastPoints.get(trk);
		if(fx != null)
		{
			// get the time
			long lastDTG = fx.getTime().getMicros() / 1000;
				
			// are we too soon?
			if(_lastDTG < lastDTG + _freq)
			{
				// yes, return
				return null;
			}
			// no, continue
		}
		else
		{
			// this is the first item we have found for this track, use it
		}		

		//////////////////////////////////////////////
		// back on the rest of the track
		//////////////////////////////////////////////
		
		String theX = st.nextToken();
		String theY = st.nextToken();
		st.nextToken();
		String theZ = st.nextToken();

		x = Double.valueOf(theX).doubleValue();
		y = Double.valueOf(theY).doubleValue();
		z = Double.valueOf(theZ).doubleValue();
		
		// convert the z from feet to yards
		z = z / 3;

		
		// calc the bearing
		double brg = Math.atan2(x,y); //@@ IM experimenting!!
		double rng = Math.sqrt(x*x + y*y);
		WorldVector offset = new WorldVector(brg, MWC.Algorithms.Conversions.Yds2Degs(rng), z);
		theLoc = _origin.add(offset);
	
    // create the fix ready to store it
    HiResDate dtg = new HiResDate(_lastDTG, 0);
    Fix res = new Fix(dtg, theLoc, 0.0, 0.0);
		
    ReplayFix rf = new ReplayFix();
    rf.theFix = res;
    rf.theTrackName = trk;
    rf.theSymbology = "@@";
    
    
    return rf;
  }
  public final String getYourType(){
    return null;
  }
	
	/** export the specified shape as a string
	 * @return the shape in String form
	 * @param theWrapper the Shape we are exporting
	 */	
	public final String exportThis(MWC.GUI.Plottable theWrapper)
	{
		return null;
	}

	/** indicate if you can export this type of object
	 * @param val the object to test
	 * @return boolean saying whether you can do it
	 */
	public final boolean canExportThis(Object val)
	{
		return false;
	}
	
}








