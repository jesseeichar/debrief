package org.mwc.cmap.plotViewer.editors.udig;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;

import com.vividsolutions.jts.geom.Coordinate;

import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

public class JtsAdapter
{

	public static ReferencedEnvelope toEnvelope(WorldArea worldArea)
	{
		if (worldArea != null) {
				WorldLocation bottomLeft = worldArea.getBottomLeft();
				WorldLocation topRight = worldArea.getTopRight();
				return new ReferencedEnvelope(bottomLeft.getLong(), topRight.getLong(),
						bottomLeft.getLat(), topRight.getLat(), DefaultGeographicCRS.WGS84);
		}
		return new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
	}

	public static Coordinate toCoord(WorldLocation val)
	{
		return new Coordinate(val.getLong(), val.getLat(), val.getDepth());
	}

	public static WorldLocation toWorldLocation(Coordinate pixelToWorld)
	{
		return new WorldLocation(pixelToWorld.x, pixelToWorld.y, pixelToWorld.z);
	}

	public static WorldArea toWorldArea(ReferencedEnvelope bounds)
	{
		return new WorldArea(new WorldLocation(bounds.getMinX(), bounds.getMaxY(), 0), new WorldLocation(bounds.getMaxX(), bounds.getMinY(), 0));
	}

}
