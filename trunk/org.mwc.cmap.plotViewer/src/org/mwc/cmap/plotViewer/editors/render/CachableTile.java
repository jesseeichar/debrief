package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;

import org.eclipse.swt.graphics.Image;
import org.geotools.geometry.jts.ReferencedEnvelope;

/**
 * A tile of rendered (or to be rendered) map layer. It has not position on the screen only on the world
 * and thus can be reused on any map or screen size.
 *
 * @author Jesse
 */
public class CachableTile implements Tile
{
	private TileState _state = TileState.BLANK;
	private Image _image;
	private final ReferencedEnvelope _bounds;
	private final Dimension _size;
	private final TileLoader _loader;
	private Image _errorImage;

	public CachableTile(TileLoader loader, Dimension tileSize, ReferencedEnvelope bounds, Image errorImage)
	{
		this._loader = loader;
		this._size = tileSize;
		this._bounds = bounds;
		this._errorImage = errorImage;
	}
	/* (non-Javadoc)
	 * @see org.mwc.cmap.plotViewer.editors.render.Tile#dispose()
	 */
	@Override
	public final synchronized void dispose(){
		if (_image != null) {
			_image.dispose();
		}
		_state = TileState.DISPOSED;
		_image = null;
	}
	@Override
	public String toString()
	{
		return "Tile [" + _state + ", " + _bounds + "]";
	}
	/* (non-Javadoc)
	 * @see org.mwc.cmap.plotViewer.editors.render.Tile#getBounds()
	 */
	@Override
	public ReferencedEnvelope getBounds()
	{
		return _bounds;
	}
	
	/* (non-Javadoc)
	 * @see org.mwc.cmap.plotViewer.editors.render.Tile#getSize()
	 */
	@Override
	public Dimension getSize()
	{
		return _size;
	}

	/* (non-Javadoc)
	 * @see org.mwc.cmap.plotViewer.editors.render.Tile#getState()
	 */
	@Override
	public synchronized TileState getState()
	{
		return _state;
	}
	/* (non-Javadoc)
	 * @see org.mwc.cmap.plotViewer.editors.render.Tile#load()
	 */
	@Override
	public synchronized Image load()
	{
		if (_image == null) {
			try {
				this._state = TileState.LOADING;
				this._image = _loader.load(_size, _bounds);
				if (_image == null) {
					this._state = TileState.BLANK;
				} else {
					this._state = TileState.READY;
				}
			} catch (Throwable t) {
				_state = TileState.ERROR;
				return _errorImage;
			}
		}
		return _image;
	}
	
}
