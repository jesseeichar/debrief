package org.mwc.cmap.plotViewer.editors.udig;

import java.io.Serializable;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import net.refractions.udig.catalog.IService;
import net.refractions.udig.catalog.ServiceExtension2;

/**
 * The extension point for loading services in uDig. The catalog uses these
 * extension points for loading
 * 
 * @author Jesse
 */
public class PlottableServiceExtension implements ServiceExtension2
{
	public final static String ID = "plottable://service";
	static final String KEY = "url";

	@Override
	public Map<String, Serializable> createParams(URL url)
	{
		return Collections.<String, Serializable> singletonMap(KEY, url);
	}

	@Override
	public IService createService(URL id, Map<String, Serializable> params)
	{
		if (reasonForFailure(params) == null)
		{
			return PlottableService.INSTANCE;
		}
		return null;
	}

	@Override
	public String reasonForFailure(Map<String, Serializable> params)
	{
		Serializable val = params.get(KEY);
		if (val instanceof URL)
		{
			URL url = (URL) val;
			return reasonForFailure(url);
		}
		return "No url in parameters";
	}

	@Override
	public String reasonForFailure(URL url)
	{
		if (!url.toExternalForm().equals(ID))
		{
			return "url must be " + ID + " not " + url;
		}

		return null;
	}

}
