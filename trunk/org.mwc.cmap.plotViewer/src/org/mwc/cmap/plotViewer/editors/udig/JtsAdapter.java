package org.mwc.cmap.plotViewer.editors.udig;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class JtsAdapter
{

	static final CoordinateReferenceSystem MAP_PROJECTION;
	static final MathTransform DEG_TO_METER_TRANSFORM;

	public static final CoordinateReferenceSystem LATLONG;
	static
	{
		try
		{
			LATLONG = CRS.decode("EPSG:4326", true);
			MAP_PROJECTION = CRS.decode("EPSG:3395");
			DEG_TO_METER_TRANSFORM = CRS.findMathTransform(LATLONG, MAP_PROJECTION);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public static ReferencedEnvelope toEnvelope(WorldArea worldArea)
	{
		if (worldArea != null)
		{
			WorldLocation bottomLeft = worldArea.getBottomLeft();
			WorldLocation topRight = worldArea.getTopRight();
			return new ReferencedEnvelope(bottomLeft.getLong(), topRight.getLong(),
					bottomLeft.getLat(), topRight.getLat(), LATLONG);
		}
		return new ReferencedEnvelope(LATLONG);
	}

	public static Coordinate toCoord(WorldLocation val)
	{
		return new Coordinate(val.getLong(), val.getLat(), val.getDepth());
	}

	public static WorldLocation toWorldLocation(Coordinate pixelToWorld)
	{
		return new WorldLocation(pixelToWorld.y, pixelToWorld.x, pixelToWorld.z);
	}

	public static WorldArea toWorldArea(ReferencedEnvelope bounds)
	{
		WorldArea worldArea = new WorldArea(new WorldLocation(bounds.getMaxY(),
				bounds.getMinX(), 0), new WorldLocation(bounds.getMinY(),
				bounds.getMaxX(), 0));
		return worldArea;
	}
	public static ReferencedEnvelope transform(Envelope envelope)
	{
		try
		{
			envelope = JTS.transform(envelope,
					DEG_TO_METER_TRANSFORM);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		return new ReferencedEnvelope(envelope, MAP_PROJECTION);
	}

	public static Coordinate transform(Coordinate coord)
	{
		try
		{
			coord = JTS.transform(coord, new Coordinate(),
					DEG_TO_METER_TRANSFORM.inverse());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		return coord;
	}

}
