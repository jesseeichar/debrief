package MWC.GUI.Properties;

// Copyright MWC 1999, Debrief 3 Project
// $RCSfile: LineStylePropertyEditor.java,v $
// @author $Author: Ian.Mayo $
// @version $Revision: 1.2 $
// $Log: LineStylePropertyEditor.java,v $
// Revision 1.2  2004/05/25 15:29:02  Ian.Mayo
// Commit updates from home
//
// Revision 1.1.1.1  2004/03/04 20:31:19  ian
// no message
//
// Revision 1.1.1.1  2003/07/17 10:07:24  Ian.Mayo
// Initial import
//
// Revision 1.2  2002-05-28 09:25:43+01  ian_mayo
// after switch to new system
//
// Revision 1.1  2002-05-28 09:14:42+01  ian_mayo
// Initial revision
//
// Revision 1.1  2002-05-23 13:15:55+01  ian
// end of 3d development
//
// Revision 1.1  2002-04-11 14:01:41+01  ian_mayo
// Initial revision
//
// Revision 1.1  2001-08-10 13:23:16+01  administrator
// improved implementation which stores strings in array
//
// Revision 1.0  2001-08-10 11:15:18+01  administrator
// Initial revision
//
// Revision 1.0  2001-07-17 08:43:50+01  administrator
// Initial revision
//
// Revision 1.1  2001-01-03 13:42:48+00  novatech
// Initial revision
//
// Revision 1.1.1.1  2000/12/12 21:45:13  ianmayo
// initial version
//
// Revision 1.2  1999-10-14 16:11:59+01  ian_mayo
// Added "CENTRE" location
//
// Revision 1.1  1999-10-12 15:36:50+01  ian_mayo
// Initial revision
//
// Revision 1.2  1999-08-04 09:45:30+01  administrator
// minor mods, tidying up
//
// Revision 1.1  1999-07-27 10:50:43+01  administrator
// Initial revision
//
// Revision 1.1  1999-07-23 14:03:57+01  administrator
// Initial revision
//

import java.beans.PropertyEditorSupport;

public class PlanningLegCalcModelPropertyEditor extends PropertyEditorSupport
{

  public static final int SPEED_TIME = 0;
  public static final int RANGE_TIME = 1;
  public static final int RANGE_SPEED = 2;

  protected Integer _myCalcModel;

  private String[] _myTags;

  public String[] getTags()
  {
    if(_myTags == null)
    {
      _myTags = new String[3];
      _myTags[SPEED_TIME] = "Speed/Time";
      _myTags[RANGE_TIME] = "Range/Time";
      _myTags[RANGE_SPEED] = "Range/Speed";
    }
    return _myTags;
  }

  public Object getValue()
  {
    return _myCalcModel;
  }



  public void setValue(Object p1)
  {
    if(p1 instanceof Integer)
    {
      _myCalcModel = (Integer)p1;
    }
    if(p1 instanceof String)
    {
      String val = (String) p1;
      setAsText(val);
    }
  }

  public void setAsText(String val)
  {
    for(int i=0;i<getTags().length;i++)
    {
      String thisStr = getTags()[i];
      if(thisStr.equals(val))
        _myCalcModel = new Integer(i);
    }
  }

  public String getAsText()
  {
    String res = null;
    int index = _myCalcModel.intValue();
    res = getTags()[index];
    return res;
  }
}

