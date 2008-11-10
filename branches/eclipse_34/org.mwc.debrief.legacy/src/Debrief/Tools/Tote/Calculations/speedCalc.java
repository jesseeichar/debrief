package Debrief.Tools.Tote.Calculations;

// Copyright MWC 1999, Debrief 3 Project
// $RCSfile: speedCalc.java,v $
// @author $Author: Ian.Mayo $
// @version $Revision: 1.4 $
// $Log: speedCalc.java,v $
// Revision 1.4  2006/03/16 16:01:08  Ian.Mayo
// Override not-applicable statement
//
// Revision 1.3  2005/12/13 09:04:55  Ian.Mayo
// Tidying - as recommended by Eclipse
//
// Revision 1.2  2004/11/25 10:24:40  Ian.Mayo
// Switch to Hi Res dates
//
// Revision 1.1.1.2  2003/07/21 14:49:17  Ian.Mayo
// Re-import Java files to keep correct line spacing
//
// Revision 1.5  2003-03-19 15:36:59+00  ian_mayo
// improvements according to IntelliJ inspector
//
// Revision 1.4  2003-02-10 16:27:47+00  ian_mayo
// Reflect name change of get wrappable data
//
// Revision 1.3  2003-02-07 15:36:09+00  ian_mayo
// Add accessor flag to indicate is this calculation needs special processing (where data crosses through zero)
//
// Revision 1.2  2002-05-28 09:25:12+01  ian_mayo
// after switch to new system
//
// Revision 1.1  2002-05-28 09:11:42+01  ian_mayo
// Initial revision
//
// Revision 1.1  2002-04-23 12:28:36+01  ian_mayo
// Initial revision
//
// Revision 1.0  2001-07-17 08:41:12+01  administrator
// Initial revision
//
// Revision 1.1  2001-01-03 13:40:26+00  novatech
// Initial revision
//
// Revision 1.1.1.1  2000/12/12 20:49:05  ianmayo
// initial import of files
//
// Revision 1.1  2000-09-14 10:25:04+01  ian_mayo
// Initial revision
//
// Revision 1.3  2000-08-14 11:00:55+01  ian_mayo
// reflect correct use of units
//
// Revision 1.2  2000-05-19 11:23:43+01  ian_mayo
// provided n/a result string when secondary watchable not present
//
// Revision 1.1  1999-10-12 15:34:21+01  ian_mayo
// Initial revision
//
// Revision 1.1  1999-08-26 10:03:04+01  administrator
// Initial revision
//

import java.text.DecimalFormat;

import Debrief.Tools.Tote.Watchable;
import MWC.GenericData.HiResDate;

public final class speedCalc extends plainCalc
{

  /////////////////////////////////////////////////////////////
  // constructor
  ////////////////////////////////////////////////////////////

  public speedCalc()
  {  
    super(new DecimalFormat("00.00"), "Speed", "Knots");
  }
  /////////////////////////////////////////////////////////////
  // member functions
  ////////////////////////////////////////////////////////////

  public final double calculate(Watchable primary, Watchable secondary, HiResDate thisTime)
  {
    double res = 0.0;
    if(secondary != null)
    {
      res = secondary.getSpeed();
    }
    return res;
  }
  
  public final String update(Watchable primary, Watchable secondary, HiResDate time)
  {
		// check we have data
		if(secondary == null)
			return NOT_APPLICABLE;
		
    return _myPattern.format(calculate(primary, secondary, time));
  }

  /** does this calculation require special bearing handling (prevent wrapping through 360 degs)
   *
   */
  public final boolean isWrappableData() {
    return false;
  }

}
