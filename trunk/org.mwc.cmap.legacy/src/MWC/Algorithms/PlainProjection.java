// Copyright MWC 1999, Generated by Together
// @Author : Ian Mayo
// @Project: Debrief 3
// @File   : PlainProjection.java

package MWC.Algorithms;


import java.awt.Point;
import java.beans.IntrospectionException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyDescriptor;
import java.io.Serializable;

import MWC.GUI.Editable;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

/**
 * interface describing how to transform from
 * earth coordinates to screen coordinates
 */
abstract public class PlainProjection implements Serializable,
  Editable
{

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

/**
   * event name for zoom
   */
  public static final String ZOOM_EVENT = "Zoom";

  /**
   * event name for this projection being replaced by another
   */
  public static final String REPLACED_EVENT = "Replaced";

  /**
   * event name for pan
   */
  public static final String PAN_EVENT = "Pan";

  /**
   * the area of data which we are currently looking at
   */
  private WorldArea _theDataArea;

  /**
   * the screen area, in data coordinates
   */
  private WorldArea _theVisibleDataArea = null;

  /**
   * the current size of the screen
   */
  private java.awt.Dimension _theScreenArea;

  /**
   * the name of this projection
   */
  private String name;

  /**
   * whether we are in relative or absolute view
   */
  private boolean _primaryOriented = false;

  /**
   * the border to apply to the current data
   */
  private double _dataBorder;

  /**
   * the parent class which will give us the information
   * necessary to produce a relative plot
   */
  protected RelativeProjectionParent _relativePlotter;

  /**
   * our editor
   */
  transient protected Editable.EditorType _myEditor;

  /**
   * support for property listeners
   */
  protected java.beans.PropertyChangeSupport _pSupport = null;

  /** whether to centre the plot on the primary track's position
   * 
   */
	private boolean _primaryCentred;

  /////////////////////////////////////////////////////////////
  // constructor
  ////////////////////////////////////////////////////////////
  protected PlainProjection(String theName)
  {
    name = theName;
    _theDataArea = null;
    _theScreenArea = null;

    //    setDataBorder(1.05);
    setDataBorder(1.1);
  }

  public String toString()
  {
    return getName();
  }


  //////////////////////////////////////////////////
  // property change support
  //////////////////////////////////////////////////

  /**
   * add a listener
   *
   * @param listener the new listener
   */
  public void addListener(java.beans.PropertyChangeListener listener)
  {
    if (_pSupport == null)
      _pSupport = new PropertyChangeSupport(this);

    _pSupport.addPropertyChangeListener(listener);
  }

  /**
   * remove a listener
   */
  public void removeListener(PropertyChangeListener listener)
  {
    _pSupport.removePropertyChangeListener(listener);
  }

  /**
   * fire a property change, if we have any listeners
   *
   * @param event_type the type of event to fire
   * @param oldValue   the old value
   * @param newValue   the new value
   */
  public void firePropertyChange(String event_type, Object oldValue, Object newValue)
  {
    if (_pSupport != null)
    {
      _pSupport.firePropertyChange(event_type, oldValue, newValue);
    }
  }


  /**
   * allow the name to be modified
   */
  public void setName(String val)
  {
    name = val;
  }

  /**
   *
   */
  public String getName()
  {
    return name;
  }

  /**
   *
   */
  public void setDataArea(WorldArea theArea)
  {
    _theDataArea = theArea;

    // clear the visible area, since it's changed
    _theVisibleDataArea = null;

    this.firePropertyChange(PlainProjection.PAN_EVENT, null, _theDataArea);
  }

  /**
   * get the currently selected data area
   */
  public WorldArea getDataArea()
  {
    return _theDataArea;
  }


  /**
   * get the current area of the screen, in data coordinates
   *
   * @return the screen area, in data coordinates
   */
  public WorldArea getVisibleDataArea()
  {
    if (_theVisibleDataArea == null)
    {
      // we'll have to recalculate it then!
      WorldLocation origin = this.toWorld(new Point(0, 0));
      if (origin != null)
      {
        WorldLocation TL = new WorldLocation(origin);
        WorldLocation BR = new WorldLocation(toWorld(new Point((int) this.getScreenArea().getWidth(),
                                                               (int) this.getScreenArea().getHeight())));
        _theVisibleDataArea = new WorldArea(TL, BR);
      }
    }

    return _theVisibleDataArea;

  }


  /**
   *
   */
  public void setScreenArea(java.awt.Dimension theArea)
  {
    _theScreenArea = theArea;

    // and reset the visible data area - it's bound to change
    _theVisibleDataArea = null;
  }

  /**
   * the screen area
   *
   * @return the dimensions of the screen
   */
  public java.awt.Dimension getScreenArea()
  {
    return _theScreenArea;
  }

  public void setRelativeProjectionParent(RelativeProjectionParent par)
  {
    _relativePlotter = par;
  }

  // HACK : uncomment these fields
  // public abstract void shiftViewPort(WorldArea newArea);
  // public abstract void resizeScreen(java.awt.Dimension theSize);
  public abstract java.awt.Point toScreen(WorldLocation val);

  public abstract WorldLocation toWorld(java.awt.Point val);


  /**
   * zoom into the plot using the supplied scale factor (or fit to window)
   *
   * @param value the scale to zoom in on (or zero for fit to win)
   */
  abstract public void zoom(double value);


  /**
   * get the border around the data
   *
   * @return the border, as a proportion of the data area (e.g. 1.1)
   */
  public double getDataBorder()
  {
    return _dataBorder;
  }

  /**
   * set the border around the data
   *
   * @param theBorder the border, as a proportion of the data area (e.g. 1.1)
   */
  public void setDataBorder(double theBorder)
  {
    _dataBorder = theBorder;

    // we also need to trigger a redraw/zoom operation
    // and offset the origin
    zoom(0d);

  }
  
  /** allow the border to be set without triggering a zoom operation
   * 
   * @param theBorder the new border to use..
   */
  public void setDataBorderNoZoom(double theBorder)
  {
  	_dataBorder = theBorder;
  }

  public void setRelativeMode(boolean primaryCentred, boolean primaryOriented)
  {
  	_primaryOriented = primaryOriented;
  	_primaryCentred = primaryCentred;
  }
  
  /**
   * produce a relative plot
   */
  public void setPrimaryOriented(boolean val)
  {
    _primaryOriented = val;
  }
  
  /** indicate if there's anything strange going on
   * 
   * @return yes/no
   */
  public boolean getNonStandardPlotting()
  {
  	return _primaryOriented || _primaryCentred;
  }

  public boolean getPrimaryOriented()
  {
    return _primaryOriented;
  }
  
  public boolean getPrimaryCentred()
  {
  	return _primaryCentred;
  }
  
  public void setPrimaryCentred(boolean val)
  {
  	_primaryCentred = val;
  }

  public Editable.EditorType getInfo()
  {
    if (_myEditor == null)
      _myEditor = new PlainProjectionInfo(this);

    return _myEditor;
  }

  public boolean hasEditor()
  {
    return true;
  }


  ////////////////////////////////////////////////////////////////////////////
  //  embedded class, used for editing the projection
  ////////////////////////////////////////////////////////////////////////////
  public class PlainProjectionInfo extends Editable.EditorType
  {

    public PlainProjectionInfo(PlainProjection data)
    {
      super(data, data.getName(), "");
    }

    public PropertyDescriptor[] getPropertyDescriptors()
    {
      try
      {
        PropertyDescriptor[] res = {
          prop("DataBorder", "the border around the projection (1.0 is zero border, 1.1 gives 10% border)"),
        };

        return res;
      }
      catch (IntrospectionException e)
      {
        return super.getPropertyDescriptors();
      }
    }
  }

  ///////////////////////////////////////////////////////////////
  // interface for classes which are able to provide us with
  // sufficient information to allow use to produce a relative plot
  ///////////////////////////////////////////////////////////////
  static public interface RelativeProjectionParent
  {
    /**
     * return the current heading
     */
    public double getHeading();

    /**
     * return the current origin for the plot
     */
    public WorldLocation getLocation();
  }


}


