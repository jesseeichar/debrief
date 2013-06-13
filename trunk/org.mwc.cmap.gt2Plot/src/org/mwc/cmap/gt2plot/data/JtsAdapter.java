package org.mwc.cmap.gt2plot.data;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;

import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

public class JtsAdapter
{

	public static final CoordinateReferenceSystem LATLONG;
	static {
		try {
			LATLONG = CRS.decode("EPSG:4326", true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public static ReferencedEnvelope toEnvelope(WorldArea worldArea)
	{
		if (worldArea != null) {
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
		WorldArea worldArea = new WorldArea(new WorldLocation(bounds.getMaxY(), bounds.getMinX(), 0), new WorldLocation(bounds.getMinY(), bounds.getMaxX(), 0));
		return worldArea;
	}

}