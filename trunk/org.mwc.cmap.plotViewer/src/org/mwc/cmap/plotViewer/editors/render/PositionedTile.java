package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Envelope;

/**
 * A wrapper of a tile object that is bound to a position on the screen and can
 * be drawn.
 * 
 * @author Jesse
 */
public class PositionedTile implements Tile
{

	private final Tile _tile;
	private final AffineTransform2D _worldToScreenTransform;

	public PositionedTile(Tile tile, AffineTransform2D worldToScreenTransform)
	{
		this._tile = tile;
		this._worldToScreenTransform = worldToScreenTransform;
	}

	public Rectangle getDrawArea()
	{
		try
		{
			Envelope bounds = new Envelope(_tile.getBounds());
			bounds = JTS.transform(bounds, _worldToScreenTransform);
			return new Rectangle((int) bounds.getMinX(), (int) bounds.getMinY(),
					(int) bounds.getWidth(), (int) bounds.getHeight());
		}
		catch (TransformException e)
		{
			throw new RuntimeException(e);
		}
	}

	public Image load()
	{
		return _tile.load();
	}

	@Override
	public void dispose()
	{
		_tile.dispose();
	}

	@Override
	public ReferencedEnvelope getBounds()
	{
		return _tile.getBounds();
	}

	@Override
	public Dimension getSize()
	{
		return _tile.getSize();
	}

	@Override
	public TileState getState()
	{
		return _tile.getState();
	}
}
