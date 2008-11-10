/*
* @(#)MetalToolBarUI.java	1.19 00/02/02
*
* Copyright 1998-2000 Sun Microsystems, Inc. All Rights Reserved.
*
* This software is the proprietary information of Sun Microsystems, Inc.
* Use is subject to license terms.
*
*/

package MWC.GUI.Tools.Swing;


import javax.swing.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;

import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.plaf.metal.*;

/**
 * A Metal Look and Feel implementation of ToolBarUI.  This implementation
 * is a "combined" view/controller.
 * <p>
 *
 * @version 1.19 02/02/00
 * @author Jeff Shapiro
 */
public class MyMetalToolBarUI extends MyBasicToolBarUI
{
  private static Border rolloverBorder = new CompoundBorder(
          new MetalBorders.RolloverButtonBorder(), new BasicBorders.MarginBorder() );

  private static Border nonRolloverBorder = new CompoundBorder(
          new MetalBorders.ButtonBorder(), new BasicBorders.MarginBorder() );

  protected ContainerListener contListener;
  protected PropertyChangeListener rolloverListener;

  private Hashtable borderTable = new Hashtable();
  private Hashtable marginTable = new Hashtable();

  private boolean rolloverBorders = false;

  private static String IS_ROLLOVER = "JToolBar.isRollover";

  private final static Insets insets0 = new Insets( 0, 0, 0, 0 );

  /** the name of this session for this toolbar
   *
   */
  protected ToolbarOwner _owner = null;

  /** constructor for this class
   * @param owner the name of the session, displayed when the toolbar is floated
   */
  public MyMetalToolBarUI(ToolbarOwner owner)
  {
    _owner = owner;
  }

  /** get the name of the session, if we know it
   *
   */
  protected String getToolbarOwner()
  {
    if(_owner != null)
      return _owner.getName();
    else
      return "n/a";
  }


  public static ComponentUI createUI( JComponent c )
  {
    return new MyMetalToolBarUI(null);
  }

  public void installUI( JComponent c )
  {
    super.installUI( c );

    Object rolloverProp = c.getClientProperty( IS_ROLLOVER );

    if ( rolloverProp != null )
    {
      rolloverBorders = ((Boolean)rolloverProp).booleanValue();
    }
    else
    {
      rolloverBorders = false;
    }

    SwingUtilities.invokeLater( new Runnable()
    {
      public void run()
      {
        setRolloverBorders( isRolloverBorders() );
      }
    });
  }

  public void uninstallUI( JComponent c )
  {
    super.uninstallUI( c );

    installNormalBorders( c );
  }

  protected void installListeners( )
  {
    super.installListeners( );

    contListener = createContainerListener( );
    if ( contListener != null ) toolBar.addContainerListener( contListener );

    rolloverListener = createRolloverListener( );
    if ( rolloverListener != null ) toolBar.addPropertyChangeListener( rolloverListener );
  }

  protected void uninstallListeners( )
  {
    super.uninstallListeners( );

    if ( contListener != null ) toolBar.removeContainerListener( contListener );
    contListener = null;

    if ( rolloverListener != null ) toolBar.removePropertyChangeListener( rolloverListener );
    rolloverListener = null;
  }

  protected ContainerListener createContainerListener( )
  {
    return new MetalContainerListener( );
  }

  protected PropertyChangeListener createRolloverListener( )
  {
    return new MetalRolloverListener( );
  }

  protected MouseInputListener createDockingListener( )
  {
    return new MetalDockingListener( toolBar );
  }

  protected void setDragOffset( Point p )
  {
    if (dragWindow == null)
      dragWindow = createMyDragWindow(toolBar);

    dragWindow.setOffset( p );
  }

  public boolean isRolloverBorders()
  {
    return rolloverBorders;
  }

  public void setRolloverBorders( boolean rollover )
  {
    rolloverBorders = rollover;

    if ( rolloverBorders )
    {
      installRolloverBorders( toolBar );
    }
    else
    {
      installNonRolloverBorders( toolBar );
    }
  }

  protected void installRolloverBorders ( JComponent c )
  {
    // Put rollover borders on buttons
    Component[] components = c.getComponents();

    for ( int i = 0; i < components.length; ++i )
    {
      if ( components[ i ] instanceof JComponent )
      {
        ( (JComponent)components[ i ] ).updateUI();

        setBorderToRollover( components[ i ] );
      }
    }
  }

  protected void installNonRolloverBorders ( JComponent c )
  {
    // Put nonrollover borders on buttons
    Component[] components = c.getComponents();

    for ( int i = 0; i < components.length; ++i )
    {
      if ( components[ i ] instanceof JComponent )
      {
        ( (JComponent)components[ i ] ).updateUI();

        setBorderToNonRollover( components[ i ] );
      }
    }
  }

  protected void installNormalBorders ( JComponent c )
  {
    // Put back the normal borders on buttons
    Component[] components = c.getComponents();

    for ( int i = 0; i < components.length; ++i )
    {
      setBorderToNormal( components[ i ] );
    }
  }

  protected void setBorderToRollover( Component c )
  {
    if ( c instanceof JButton )
    {
      JButton b = (JButton)c;

      if ( b.getUI() instanceof MetalButtonUI )
      {
        if ( b.getBorder() instanceof UIResource )
        {
          borderTable.put( b, b.getBorder() );
        }

        if ( b.getBorder() instanceof UIResource || b.getBorder() == nonRolloverBorder )
        {
          b.setBorder( rolloverBorder );
        }

        if ( b.getMargin() == null || b.getMargin() instanceof UIResource )
        {
          marginTable.put( b, b.getMargin() );
          b.setMargin( insets0 );
        }

        b.setRolloverEnabled( true );
      }
    }
  }

  protected void setBorderToNonRollover( Component c )
  {
    if ( c instanceof JButton )
    {
      JButton b = (JButton)c;

      if ( b.getUI() instanceof MetalButtonUI )
      {
        if ( b.getBorder() instanceof UIResource )
        {
          borderTable.put( b, b.getBorder() );
        }

        if ( b.getBorder() instanceof UIResource || b.getBorder() == rolloverBorder )
        {
          b.setBorder( nonRolloverBorder );
        }

        if ( b.getMargin() == null || b.getMargin() instanceof UIResource )
        {
          marginTable.put( b, b.getMargin() );
          b.setMargin( insets0 );
        }

        b.setRolloverEnabled( false );
      }
    }
  }

  protected void setBorderToNormal( Component c )
  {
    if ( c instanceof JButton )
    {
      JButton b = (JButton)c;

      if ( b.getUI() instanceof MetalButtonUI )
      {
        if ( b.getBorder() == rolloverBorder || b.getBorder() == nonRolloverBorder )
        {
          b.setBorder( (Border)borderTable.remove( b ) );
        }

        if ( b.getMargin() == insets0 )
        {
          b.setMargin( (Insets)marginTable.remove( b ) );
        }

        b.setRolloverEnabled( false );
      }
    }
  }


  protected class MetalContainerListener implements ContainerListener
  {
    public void componentAdded( ContainerEvent e )
    {
      Component c = e.getChild();

      if ( rolloverBorders )
      {
        setBorderToRollover( c );
      }
      else
      {
        setBorderToNonRollover( c );
      }
    }

    public void componentRemoved( ContainerEvent e )
    {
      Component c = e.getChild();
      setBorderToNormal( c );
    }

  } // end class MetalContainerListener


  protected class MetalRolloverListener implements PropertyChangeListener
  {
    public void propertyChange( PropertyChangeEvent e )
    {
      String name = e.getPropertyName();

      if ( name.equals( IS_ROLLOVER ) )
      {
        if ( e.getNewValue() != null )
        {
          setRolloverBorders( ((Boolean)e.getNewValue()).booleanValue() );
        }
        else
        {
          setRolloverBorders( false );
        }
      }
    }
  } // end class MetalRolloverListener


  protected class MetalDockingListener extends DockingListener
  {
    private boolean pressedInBumps = false;

    public MetalDockingListener( JToolBar t )
    {
      super( t );
    }

    public void mousePressed( MouseEvent e )
    {
      super.mousePressed( e );
      if (!toolBar.isEnabled()) {
        return;
      }
      pressedInBumps = false;

      Rectangle bumpRect = new Rectangle();

      if ( toolBar.getSize().height <= toolBar.getSize().width )  // horizontal
      {
        if(toolBar.getComponentOrientation().isLeftToRight() ) {
          bumpRect.setBounds( 0, 0, 14, toolBar.getSize().height );
        } else {
          bumpRect.setBounds( toolBar.getSize().width-14, 0,
                              14, toolBar.getSize().height );
        }
      }
      else  // vertical
      {
        bumpRect.setBounds( 0, 0, toolBar.getSize().width, 14 );
      }

      if ( bumpRect.contains( e.getPoint() ) )
      {
        pressedInBumps = true;

        Point dragOffset = e.getPoint();
        if( !toolBar.getComponentOrientation().isLeftToRight() ) {
          dragOffset.x -= toolBar.getSize().width
                  - toolBar.getPreferredSize().width;
        }
        setDragOffset( dragOffset );

      }
    }

    public void mouseDragged( MouseEvent e )
    {
      if ( pressedInBumps )
      {
        super.mouseDragged( e );
      }
    }

  } // end class MetalDockingListener

  /** embedded interface for objects which are capable of owning a toolbar
   * this is needed for MDI interfaces when it is not clear which document
   * owns the various toolbars
   */
  public static interface ToolbarOwner
  {
    public String getName();
  }

}


