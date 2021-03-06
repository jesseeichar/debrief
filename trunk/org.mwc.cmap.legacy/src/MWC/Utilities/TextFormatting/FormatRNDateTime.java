// Copyright MWC 1999, Generated by Together
// @Author : Ian Mayo
// @Project: Debrief 3
// @File   : FormatRNDateTime.java

package MWC.Utilities.TextFormatting;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class FormatRNDateTime 
{
  static private SimpleDateFormat _df = null;

  static public String toString(long theVal)
  {
		return toStringLikeThis(theVal, 
														"ddHHmm.ss");
  }
  
  
  
	static public String toMediumString(long theVal)
	{
		return toStringLikeThis(theVal, 
														"ddHHmm");
	}
  
	static public String toShortString(long theVal)
	{
		return toStringLikeThis(theVal, 
														"HHmm");
	}
	
	static public String toStringLikeThis(long theVal,
																				String thePattern)
	{
    java.util.Date theTime = new java.util.Date(theVal);
    String res;

    if(_df == null)
    {
      _df = new SimpleDateFormat(thePattern);
      _df.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    // do we need to change the pattern?
    if(_df.toPattern().equals(thePattern))
    {
      // hey, don't bother, we're ok
    }
    else
    {
      // and update the pattern
      _df.applyPattern(thePattern);
    }

    res = _df.format(theTime);

    return res;
	}
	
  static public String getExample(){
    return "ddHHmm.ss";
  }
}


