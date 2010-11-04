/**
 * 
 */
package org.mwc.debrief.core.creators.shapes;

import MWC.GUI.Shapes.*;
import MWC.GenericData.WorldDistance;
import MWC.GenericData.WorldLocation;

/**
 * @author ian.mayo
 * 
 */
public class InsertWheel extends CoreInsertShape
{

	/**
	 * produce the shape for the user
	 * 
	 * @param centre
	 *          the current centre of the screen
	 * @return a shape, based on the centre
	 */
	protected PlainShape getShape(WorldLocation centre)
	{
		WorldDistance outerRadius = new WorldDistance(12000, WorldDistance.YARDS);
		// generate the shape
		PlainShape res = new WheelShape(centre, new WorldDistance(4000,
				WorldDistance.YARDS), outerRadius);
		return res;
	}

	/**
	 * return the name of this shape, used give the shape an initial name
	 * 
	 * @return the name of this type of shape, eg: rectangle
	 */
	protected String getShapeName()
	{
		return "wheel";
	}

}