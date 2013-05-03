package org.mwc.cmap.plotViewer.editors.udig;

import static org.mwc.cmap.plotViewer.editors.udig.JtsAdapter.DEG_TO_METER_TRANSFORM;
import static org.mwc.cmap.plotViewer.editors.udig.JtsAdapter.MAP_PROJECTION;

import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.render.displayAdapter.IMapDisplay;

import org.geotools.geometry.jts.JTS;

import MWC.Algorithms.PlainProjection;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class UDigRendererProjection extends PlainProjection
{

	private static final long serialVersionUID = 1L;
	private static final double NEAR_ZERO = 0.00000000000000000001;
	private ViewportModel _viewportModel;

	public UDigRendererProjection()
	{
		super("udig");
	}

	public void setViewportModel(ViewportModel viewportModel)
	{
		this._viewportModel = viewportModel;

		setDataArea(JtsAdapter.toWorldArea(_viewportModel.getBounds()));
		IMapDisplay mapDisplay = _viewportModel.getRenderManagerInternal()
				.getMapDisplay();
		setScreenArea(mapDisplay.getDisplaySize());
		addListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent event)
			{
				String id = event.getPropertyName();
				if (PlainProjection.PAN_EVENT.equals(id))
				{
					WorldArea newBounds = (WorldArea) event.getNewValue();
					newBounds.trim(); // trim to reasonable bounds;
					gtTrim(newBounds);
					boolean deliver = _viewportModel.eDeliver();
					try
					{
						_viewportModel.eSetDeliver(false);
						_viewportModel.setCRS(MAP_PROJECTION);
					}
					finally
					{
						_viewportModel.eSetDeliver(deliver);
					}
					Envelope envelope = JtsAdapter.transform(JtsAdapter.toEnvelope(newBounds));
					_viewportModel.setBounds(envelope);
				}
				else if (PlainProjection.REPLACED_EVENT.equals(id))
				{
					System.out.println("replace event");
				}
				else if (PlainProjection.ZOOM_EVENT.equals(id))
				{
					System.out.println("zoom event");
				}
			}

		});
	}

	private void gtTrim(WorldArea theArea)
	{
		gtTrim(theArea.getTopLeft());
		gtTrim(theArea.getBottomRight());
	}

	private void gtTrim(WorldLocation loc)
	{
		loc.setLat(Math.min(loc.getLat(), 89.9999));
		loc.setLat(Math.max(loc.getLat(), -89.9999));

		loc.setLong(Math.min(loc.getLong(), 179.999));
		loc.setLong(Math.max(loc.getLong(), -179.999));
	}

	@Override
	public Point toScreen(WorldLocation val)
	{
		Coordinate coord = JtsAdapter.toCoord(val);
		try
		{
			coord = JTS.transform(coord, new Coordinate(), DEG_TO_METER_TRANSFORM);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		return _viewportModel.worldToPixel(coord);
	}

	@Override
	public WorldLocation toWorld(Point val)
	{
		Coordinate pixelToWorld = _viewportModel.pixelToWorld(val.x, val.y);
		pixelToWorld = JtsAdapter.transform(pixelToWorld);
		return JtsAdapter.toWorldLocation(pixelToWorld);
	}

	@Override
	public void zoom(double value)
	{
		if (_viewportModel != null)
		{
			if (value < NEAR_ZERO && value > NEAR_ZERO)
			{
				// don't zoom
			}
			else
			{
				_viewportModel.zoom(1 / value);
			}
		}
	}

}
