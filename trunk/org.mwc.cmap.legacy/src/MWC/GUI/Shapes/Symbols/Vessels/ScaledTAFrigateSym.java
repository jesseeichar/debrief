package MWC.GUI.Shapes.Symbols.Vessels;

import java.util.Vector;

import MWC.GUI.CanvasType;
import MWC.GenericData.WorldLocation;

public class ScaledTAFrigateSym extends ScreenScaledSym
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** initialise, and set the scale factor
	 * 
	 */	
	public ScaledTAFrigateSym()
	{
		super();
		// set the scale
		this.setScaleVal(2);
	}


	protected Vector<double[][]> getCoords()
	{
		Vector<double[][]> hullLines = new Vector<double[][]>();

		// start with the cross-over bits
		hullLines.add(new double[][]
		{
		{ -4, -2 }, { 4, 2 } });

		hullLines.add(new double[][]
		                       		{
		                       		{ -4, 2 }, { 4, -2 } });
		hullLines.add(new double[][]
			                       		{
			                       		{ -4, 2 }, { -4, -2 } });
		hullLines.add(new double[][]
			                       		{
			                       		{ 4, 2 }, { 4, -2 } });

		hullLines.add(new double[][]
			                       		{
			                       		{ -4, 2 }, { 4, -2 } });

		// now the dangly bits
		hullLines.add(new double[][]
			                       		{
			                       		{ -4, 2 }, { 0, 5 } });
		hullLines.add(new double[][]
			                       		{
			                       		{ 4, 2 }, { 0, 5 } });

		// and the tail
		hullLines.add(new double[][]
			                       		{
		{ 0,	5},
		{ -0.8,	5.3},
		{ -1,	6},
		{ -0.8,	6.7},
		{ 0,	7},
		{ 0.8,	7.3},
		{ 1,	8},
		{ 0.8,	8.7},
		{ 0,	9 },
		{ -0.4,	9.1 }
		});
	
		return hullLines;

	}

	/**
	 * getType
	 * 
	 * @return the returned String
	 */
	public String getType()
	{
		return "TA Frigate";
	}

}
