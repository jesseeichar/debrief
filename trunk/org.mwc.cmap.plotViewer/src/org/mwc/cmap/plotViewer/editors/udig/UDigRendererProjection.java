package org.mwc.cmap.plotViewer.editors.udig;

import java.awt.Point;

import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.render.displayAdapter.IMapDisplay;
import MWC.Algorithms.PlainProjection;
import MWC.GenericData.WorldLocation;

public class UDigRendererProjection extends PlainProjection
{

	private static final long serialVersionUID = 1L;
	private double _zoom;
	private ViewportModel _viewportModel;

	public UDigRendererProjection(ViewportModel viewportModel)
	{
		super("udig");
		this._viewportModel = viewportModel;
		setDataArea(JtsAdapter.toWorldArea(_viewportModel.getBounds()));
		IMapDisplay mapDisplay = _viewportModel.getRenderManagerInternal().getMapDisplay();
		setScreenArea(mapDisplay.getDisplaySize());
	}

	@Override
	public Point toScreen(WorldLocation val)
	{
		return _viewportModel.worldToPixel(JtsAdapter.toCoord(val));
	}

	@Override
	public WorldLocation toWorld(Point val)
	{
		return JtsAdapter.toWorldLocation(_viewportModel.pixelToWorld(val.x, val.y));
	}

	@Override
	public void zoom(double value)
	{
		this._zoom = value;

	}

}
