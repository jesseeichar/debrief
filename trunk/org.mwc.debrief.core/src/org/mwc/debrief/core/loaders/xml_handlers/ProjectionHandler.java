package org.mwc.debrief.core.loaders.xml_handlers;

/**
 * Title:        Debrief 2000
 * Description:  Debrief 2000 Track Analysis Software
 * Copyright:    Copyright (c) 2000
 * Company:      MWC
 * @author Ian Mayo
 * @version 1.0
 */

import org.w3c.dom.Element;

import MWC.Algorithms.PlainProjection;
import MWC.GenericData.WorldArea;
import MWC.Utilities.ReaderWriter.XML.Util.LocationHandler;


abstract class ProjectionHandler extends  MWC.Utilities.ReaderWriter.XML.MWCXMLReader
{

  private static final String PRIMARY_ORIENTED = "PrimaryOriented";
	private static final String RELATIVE_MODE = "Relative";
	private static final String PRIMARY_CENTRED = "PrimaryOrigin";
	MWC.GenericData.WorldLocation _tl;
  MWC.GenericData.WorldLocation _br;
  String _type;
  double _border;
  boolean _primaryOriented;
  boolean _primaryCentred;

  public ProjectionHandler()
  {
    // inform our parent what type of class we are
    super("projection");

    // handlers for the corners
    addHandler(new LocationHandler("tl"){
      public void setLocation(MWC.GenericData.WorldLocation res)
      {
        _tl = res;
      }
    });
    addHandler(new LocationHandler("br"){
      public void setLocation(MWC.GenericData.WorldLocation res)
      {
        _br = res;
      }
    });

    addAttributeHandler(new HandleAttribute("Type"){
      public void setValue(String name, String val)
      {
        _type = val;
      }
    });
    addAttributeHandler(new HandleAttribute("Border"){
      public void setValue(String name, String val)
      {
        try{
          _border = readThisDouble(val);
        }
        catch(java.text.ParseException pe)
        {
          MWC.Utilities.Errors.Trace.trace(pe, "Failed reading in border size for projection:" + val);
        }
      }
    });
    addAttributeHandler(new HandleBooleanAttribute(RELATIVE_MODE){
      public void setValue(String name, boolean value)
      {
        _primaryOriented = value;
      }});
    addAttributeHandler(new HandleBooleanAttribute(PRIMARY_ORIENTED){
      public void setValue(String name, boolean value)
      {
        _primaryOriented = value;
      }});
    addAttributeHandler(new HandleBooleanAttribute(PRIMARY_CENTRED){
      public void setValue(String name, boolean value)
      {
        _primaryCentred = value;
      }});


  }



  public final void elementClosed()
  {
    MWC.Algorithms.PlainProjection newProj=null;
    if(_type.equals("Flat"))
    {
      newProj = new MWC.Algorithms.Projections.FlatProjection();
      newProj.setDataBorder(_border);
      newProj.setDataArea(new MWC.GenericData.WorldArea(_tl, _br));
      newProj.setPrimaryOriented(_primaryOriented);
      newProj.setPrimaryCentred(_primaryCentred);
    }

    if(newProj != null)
    {
    	// store the new projection details
			setProjection(newProj);
    }

  }
	
	public abstract void setProjection(PlainProjection proj);

  public static void exportProjection(MWC.Algorithms.PlainProjection projection, org.w3c.dom.Element parent, org.w3c.dom.Document doc)
  {

/*
<!ELEMENT projection (tl, br)>
<!ATTLIST projection
  type CDATA #REQUIRED
  border CDATA "1.0"
  relative (TRUE|FALSE) "FALSE"
>
*/

    Element proj = doc.createElement("projection");

    // see which type of projection it is
    if(projection instanceof MWC.Algorithms.Projections.FlatProjection)
    {
      MWC.Algorithms.Projections.FlatProjection flat = (MWC.Algorithms.Projections.FlatProjection)projection;

      // first the attributes for the projection
      proj.setAttribute("Type", "Flat");
      proj.setAttribute("Border", writeThis(flat.getDataBorder()));
      proj.setAttribute(PRIMARY_CENTRED, writeThis(flat.getPrimaryCentred()));
      proj.setAttribute(PRIMARY_ORIENTED, writeThis(flat.getPrimaryOriented()));

      // and now the corners
      WorldArea dataArea = flat.getDataArea();
			MWC.Utilities.ReaderWriter.XML.Util.LocationHandler.exportLocation(dataArea.getTopLeft(), "tl", proj, doc);
      MWC.Utilities.ReaderWriter.XML.Util.LocationHandler.exportLocation(dataArea.getBottomRight(), "br", proj, doc);

      parent.appendChild(proj);
    }
    else
    {
      // Hmm, we don't really know how tor handle this.
      java.lang.RuntimeException duffer = new java.lang.RuntimeException("Unable to store this projection type");
      throw duffer;
    }



  }


}