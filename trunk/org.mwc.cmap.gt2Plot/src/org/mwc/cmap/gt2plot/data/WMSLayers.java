package org.mwc.cmap.gt2plot.data;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.eclipse.swt.widgets.Display;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.ows.ServiceException;

import MWC.GUI.BlockingLayer;
import MWC.GUI.CanvasType;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Plottable;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

public class WMSLayers implements Layer, BlockingLayer
{
	private static final long serialVersionUID = -5081001576656982185L;
	List<String> prioritizedFormats = Arrays.asList(new String[]
	{ "image/png", "image/png; mode=16bit", "image/png; mode=8bit", "image/tiff",
			"image/tiff8", "image/geotiff", "image/geotiff8", "image/gif",
			"image/jpeg", "image/jpg" });

	private List<String> _wmsLayers = new LinkedList<String>();

	private URL _capabilities;

	private transient WebMapServer _server;

	private boolean _visible = true;

	private String _name;

	@Override
	public void append(Layer other)
	{
		if (other instanceof WMSLayers)
		{
			WMSLayers otherWms = (WMSLayers) other;
			if (getServer() == null)
			{
				setCapabilities(otherWms._capabilities);
				setWmsLayers(otherWms._wmsLayers);
			}
			else
			{
				URI thisSource = getServer().getInfo().getSource();
				if (otherWms.getServer() != null
						&& thisSource.equals(otherWms.getServer().getInfo().getSource()))
				{
					_wmsLayers.addAll(otherWms._wmsLayers);
				}
			}
		}
	}

	private synchronized WebMapServer getServer()
	{
		if (_server == null)
		{
			try
			{
				_server = new WebMapServer(_capabilities);
			}
			catch (ServiceException e)
			{
				e.printStackTrace();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return _server;
	}

	@Override
	public void paint(final CanvasType dest)
	{
		Map<String, org.geotools.data.ows.Layer> layers = new HashMap<String, org.geotools.data.ows.Layer>();
		WMSCapabilities wmsCapabilities = getServer().getCapabilities();
		for (org.geotools.data.ows.Layer layer : wmsCapabilities.getLayerList())
		{
			layers.put(layer.getName(), layer);
		}

		GetMapRequest getMapRequest = getServer().createGetMapRequest();
		for (String layerName : _wmsLayers)
		{
			getMapRequest.addLayer(layers.get(layerName));
		}

		WorldArea dataArea = dest.getProjection().getVisibleDataArea();
		WorldLocation tl = dataArea.getTopLeft();
		WorldLocation br = dataArea.getBottomRight();
		String bbox = "" + tl.getLong() + "," + br.getLat() + "," + br.getLong()
				+ "," + tl.getLat();
		getMapRequest.setBBox(bbox);
		getMapRequest.setDimensions(dest.getSize());

		String format = findBestFormat(wmsCapabilities);
		getMapRequest.setFormat(format);
		getMapRequest.setSRS("EPSG:4326");
		final BufferedImage image = readImage(getMapRequest);

		if (image != null)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					dest.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
				}
			});
		}
		else
		{
			System.out.println("Warning wms did not return a valid image");
		}
	}

	private String findBestFormat(WMSCapabilities wmsCapabilities)
	{
		List<String> formats = new ArrayList<String>(wmsCapabilities.getRequest()
				.getGetMap().getFormats());
		Collections.sort(formats, new Comparator<String>()
		{

			@Override
			public int compare(String o1, String o2)
			{
				int priority1 = priotize(o1);
				int priority2 = priotize(o2);
				return priority1 - priority2;
			}

			protected int priotize(String o1)
			{
				int indexOf = prioritizedFormats.indexOf(o1);
				if (indexOf == -1)
				{
					return Integer.MAX_VALUE;
				}
				else
				{
					return indexOf;
				}
			}

		});
		return formats.get(0);
	}

	private BufferedImage readImage(GetMapRequest getMapRequest)
	{
		URL url = getMapRequest.getFinalURL();
		try
		{
			return ImageIO.read(url);
		}
		catch (IOException e)
		{
			return null;
		}
	}

	@Override
	public boolean getVisible()
	{
		return _visible;
	}

	@Override
	public double rangeFrom(WorldLocation other)
	{
		return 0;
	}

	@Override
	public String getName()
	{
		return _name;
	}

	@Override
	public int compareTo(Plottable o)
	{
		return _name.compareTo(o.getName());
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
		// TODO Auto-generated method stub

	}

	@Override
	public WorldArea getBounds()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String val)
	{
		_name = val;
	}

	@Override
	public boolean hasOrderedChildren()
	{
		return true;
	}

	@Override
	public int getLineThickness()
	{
		return 0;
	}

	@Override
	public void add(Editable point)
	{
		System.out.println("not supported");
	}

	@Override
	public void removeElement(Editable point)
	{
		System.out.println("not supported");
	}

	@Override
	public Enumeration<Editable> elements()
	{
		return null;
	}

	@Override
	public synchronized void setVisible(boolean val)
	{
		_visible = val;
	}

	public synchronized void setWmsLayers(List<String> wmsLayers)
	{
		this._wmsLayers = wmsLayers;
	}

	public synchronized void setCapabilities(URL capabilities)
	{
		this._capabilities = capabilities;
	}

	public void addLayerName(String layerName)
	{
		this._wmsLayers.add(layerName);
	}
}
