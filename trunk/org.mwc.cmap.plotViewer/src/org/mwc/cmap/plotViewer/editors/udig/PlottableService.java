/**
 * 
 */
package org.mwc.cmap.plotViewer.editors.udig;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.catalog.IServiceInfo;
import net.refractions.udig.core.internal.CorePlugin;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Jesse
 * 
 */
public class PlottableService extends IService
{

	private static final URL URL = CorePlugin
			.createSafeURL(PlottableServiceExtension.ID);


	private static final Map<String, Serializable> PARAMS = Collections
			.<String, Serializable> singletonMap(PlottableServiceExtension.KEY, URL);

	/**
	 * The singleton 
	 * 
	 * Note: URL has to be defined before INSTANCE because it is used in constructor
	 */
	public static final PlottableService INSTANCE = new PlottableService();

	private CopyOnWriteArrayList<PlottableGeoResource> _resources = new CopyOnWriteArrayList<PlottableGeoResource>();

	private PlottableService()
	{
		try
		{
			info = new IServiceInfo("Plottable service", "Plottable service",
					"Plottable service", URL.toURI(), URL.toURI(), URL.toURI(),
					new String[]
					{ "Debrief" }, null);
		}
		catch (URISyntaxException e)
		{
			// can't happen because URL is hardcoded
			throw new RuntimeException(e);
		}
	}

	public PlottableGeoResource addLayer(MWC.GUI.Layer theLayer)
	{
		for (PlottableGeoResource resource : _resources)
		{
			if (resource.getLayer() == theLayer)
			{
				return resource;
			}
		}
		PlottableGeoResource resource = new PlottableGeoResource(theLayer);
		_resources.add(resource);
		return resource;
	}

	@Override
	public IServiceInfo getInfo(IProgressMonitor monitor) throws IOException
	{
		return info;
	}

	@Override
	protected IServiceInfo createInfo(IProgressMonitor monitor)
			throws IOException
	{
		return info;
	}

	@Override
	public Map<String, Serializable> getConnectionParams()
	{
		return PARAMS;
	}

	@Override
	public URL getIdentifier()
	{
		return URL;
	}

	@Override
	public Throwable getMessage()
	{
		return null;
	}

	@Override
	public Status getStatus()
	{
		return Status.CONNECTED;
	}

	@Override
	public List<? extends IGeoResource> resources(IProgressMonitor monitor)
			throws IOException
	{
		return Collections.unmodifiableList(_resources);
	}

}
