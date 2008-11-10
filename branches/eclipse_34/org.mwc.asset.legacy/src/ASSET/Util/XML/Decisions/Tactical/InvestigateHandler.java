package ASSET.Util.XML.Decisions.Tactical;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

import ASSET.Models.Decision.Tactical.Investigate;
import ASSET.Models.Decision.TargetType;
import ASSET.Models.Detection.DetectionEvent;
import ASSET.Util.XML.Decisions.Util.TargetTypeHandler;
import MWC.GenericData.WorldDistance;

abstract public class InvestigateHandler extends CoreDecisionHandler
{

  private final static String type = "Investigate";

  private final static String TARGET_TYPE = "TargetType";
  private final static String DETECTION_LEVEL = "DetectionLevel";
  private final static String INVESTIGATE_HEIGHT = "Height";


  private TargetType _myTargetType;
  private String _detLevel = null;
  private WorldDistance _investigateHeight;

  /**
   * get the handler ready
   */
  private static DetectionEvent.DetectionStatePropertyEditor _detectionHandler = new DetectionEvent.DetectionStatePropertyEditor();


  public InvestigateHandler()
  {
    super(type);

    addAttributeHandler(new HandleAttribute(DETECTION_LEVEL)
    {
      public void setValue(String name, String value)
      {
        _detLevel = value;
      }
    });

    addHandler(new ASSET.Util.XML.Decisions.Util.TargetTypeHandler(TARGET_TYPE)
    {
      public void setTargetType(final TargetType type)
      {
        _myTargetType = type;
      }
    });

    addHandler(new MWC.Utilities.ReaderWriter.XML.Util.WorldDistanceHandler(INVESTIGATE_HEIGHT)
    {
      public void setWorldDistance(WorldDistance res)
      {
        _investigateHeight = res;
      }
    });
  }


  public void elementClosed()
  {
    // find out the det level
    _detectionHandler.setAsText(_detLevel);

    int val = _detectionHandler.getIndex();

    final Investigate tr = new Investigate(null, _myTargetType, val, _investigateHeight);

    // update the parent fields
    super.setAttributes(tr);

    setModel(tr);

    _myTargetType = null;
    _detLevel = null;
    _investigateHeight = null;

  }


  abstract public void setModel(ASSET.Models.DecisionType dec);

  static public void exportThis(final Object toExport, final org.w3c.dom.Element parent,
                                final org.w3c.dom.Document doc)
  {
    // create ourselves
    final org.w3c.dom.Element element = doc.createElement(type);

    // get data item
    final Investigate bb = (Investigate) toExport;

    _detectionHandler.setIndex(bb.getDetectionLevel());


    // output the parent fields
    CoreDecisionHandler.exportThis(bb, element, doc);

    // output it's attributes
    TargetTypeHandler.exportThis(TARGET_TYPE, bb.getTargetType(), element, doc);
    element.setAttribute(DETECTION_LEVEL, _detectionHandler.getAsText());

    if (bb.getInvestigateHeight() != null)
    {
      MWC.Utilities.ReaderWriter.XML.Util.WorldDistanceHandler.exportDistance(INVESTIGATE_HEIGHT, bb.getInvestigateHeight(), element, doc);
    }

    parent.appendChild(element);
  }


}