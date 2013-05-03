package org.mwc.cmap.plotViewer.editors.udig;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IGeoResourceInfo;
import net.refractions.udig.core.internal.CorePlugin;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import MWC.GUI.Layer;

public class PlottableGeoResource extends IGeoResource
{

	private Layer _layer;
	private URL _id;

	public PlottableGeoResource(MWC.GUI.Layer theLayer)
	{
		_layer = theLayer;
		_id = CorePlugin.createSafeURL(PlottableServiceExtension.ID + "#"
				+ UUID.randomUUID());

		ReferencedEnvelope jtsBounds = JtsAdapter.transform(JtsAdapter.toEnvelope(theLayer.getBounds()));

		CoordinateReferenceSystem crs = jtsBounds.getCoordinateReferenceSystem();
		info = new IGeoResourceInfo(_layer.getName(), _layer.getName(),
				_layer.getName(), null, jtsBounds, crs, new String[]
				{ _layer.getName() }, null);
		service = PlottableService.INSTANCE;
	}

	@Override
	public <T> boolean canResolve(Class<T> adaptee)
	{
		return adaptee.isAssignableFrom(MWC.GUI.Layer.class)
				|| super.canResolve(adaptee);
	}

	@Override
	protected IGeoResourceInfo createInfo(IProgressMonitor monitor)
			throws IOException
	{
		return info;
	}

	@Override
	public IGeoResourceInfo getInfo(IProgressMonitor monitor) throws IOException
	{
		return info;
	}

	@Override
	public URL getIdentifier()
	{
		return _id;
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
	public <T> T resolve(Class<T> adaptee, IProgressMonitor monitor)
			throws IOException
	{
		if (adaptee.isAssignableFrom(MWC.GUI.Layer.class))
		{
			return adaptee.cast(_layer);
		}
		return super.resolve(adaptee, monitor);
	}

	public Layer getLayer()
	{
		return _layer;
	}

}
