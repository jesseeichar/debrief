/*
 * Created by IntelliJ IDEA.
 * User: administrator
 * Date: Oct 31, 2001
 * Time: 1:23:18 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package ASSET.Util.XMLFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class XMLAttribute implements XMLObject
{
  /***************************************************************
   *  member variables
   ***************************************************************/

  /** the name of the attribute we are editing
   *
   */
  private String _name = null;

  /** the operation we are performing
   *
   */
  private XMLOperation _myOperation = null;

  /***************************************************************
   *  constructor
   ***************************************************************/
  public XMLAttribute(final Element element)
  {
    // read ourselves in from this element

    // read in the attribute name
    _name = element.getAttribute("name");

    // read in the operation
    NodeList obj = element.getElementsByTagName("Range");

    if(obj.getLength() == 0)
    {
      // oh well, try for a choice
      obj = element.getElementsByTagName("Choice");

      if(obj.getLength() > 0)
      {
        _myOperation = new XMLChoice((Element) obj.item(0));
      }
    }
    else
    {
      _myOperation = new XMLRange((Element) obj.item(0));
    }
  }

  private XMLAttribute(final XMLAttribute other)
  {
    _name = other._name;
    _myOperation = (XMLOperation)other._myOperation.clone();
  }

  /***************************************************************
   *  member methods
   ***************************************************************/
  public Object clone()
  {
    final XMLAttribute res = new XMLAttribute(this);
    return res;
  }


  /** randomise ourselves
   *
   */
  public void randomise()
  {
    _myOperation.newPermutation();
  }

  /** perform our update to the supplied element
   *
   */
  public void execute(final Element object)
  {
    // create the new permutation
//    _myOperation.newPermutation();

    // get a new value for the object
    final String newVal = (String) _myOperation.getValue();

    // update our attribute
    object.setAttribute(_name, newVal);
  }

  /** return the current value of the attribute for this operation in the supplied element
   * @object the node we are taking the attribute from
   * @return the String representation of the attribute in this object
   */
  public String getCurValueIn(final Element object)
  {
    final String res = object.getAttribute(_name);
    return res;
  }

  /** return the last value used for this attribute
   *
   */
  public String getCurValue()
  {
    return _myOperation.getValue();
  }

  /** return the name of this variable
   *
   */
  public String getName()
  {
    return _name;
  }


  /** merge ourselves with the supplied object
   *
   */
  public void merge(final XMLObject other)
  {
    final XMLAttribute xr = (XMLAttribute)other;
    _myOperation.merge(xr._myOperation);
  }

}
