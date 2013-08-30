package org.mwc.cmap.plotViewer.editors.render;

import java.util.Enumeration;
import java.util.List;

import MWC.GUI.CanvasType;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Plottable;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

/**
 * Allows several layers to be rendered together as one. Only the paint methods
 * are implemented.
 * 
 * @author Jesse
 */
public class MultiLayerCompositeLayer implements Layer
{

	private static final long serialVersionUID = 1L;
	private List<Layer> _layers;

	public MultiLayerCompositeLayer(List<Layer> layers)
	{
		this._layers = layers;
	}

	@Override
	public boolean getVisible()
	{
		return true;
	}

	@Override
	public double rangeFrom(WorldLocation other)
	{
		return 0;
	}

	@Override
	public String getName()
	{
		return null;
	}

	@Override
	public int compareTo(Plottable o)
	{
		return 0;
	}

	@Override
	public boolean hasEditor()
	{
		return false;
	}

	@Override
	public EditorType getInfo()
	{
		return null;
	}

	@Override
	public void exportShape()
	{

	}

	@Override
	public void append(Layer other)
	{

	}

	@Override
	public void paint(CanvasType dest)
	{
		for (Layer layer : _layers)
		{
			synchronized (layer)
			{
				if (layer.getVisible())
				{
					layer.paint(dest);
				}
			}
		}
	}

	@Override
	public WorldArea getBounds()
	{
		return null;
	}

	@Override
	public void setName(String val)
	{
	}

	@Override
	public boolean hasOrderedChildren()
	{
		return false;
	}

	@Override
	public int getLineThickness()
	{
		return 0;
	}

	@Override
	public void add(Editable point)
	{
	}

	@Override
	public void removeElement(Editable point)
	{

	}

	@Override
	public Enumeration<Editable> elements()
	{
		return null;
	}

	@Override
	public void setVisible(boolean val)
	{
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_layers == null) ? 0 : _layers.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MultiLayerCompositeLayer other = (MultiLayerCompositeLayer) obj;
		if (_layers == null)
		{
			if (other._layers != null)
				return false;
		}
		else if (!_layers.equals(other._layers))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "MultiLayerCompositeLayer [_layers=" + _layers + "]";
	}

}
