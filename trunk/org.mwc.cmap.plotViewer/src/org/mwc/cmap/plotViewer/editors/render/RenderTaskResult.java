package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Color;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.mwc.cmap.core.ui_support.swt.SWTCanvasAdapter;

public abstract class RenderTaskResult
{
	protected final Image _image;
	private Rectangle _drawArea;
	private String _debugInfo;

	public RenderTaskResult(Image image, Rectangle drawArea, String debugInfo)
	{
		super();
		this._image = image;
		this._drawArea = drawArea;
		this._debugInfo = debugInfo;
	}

	/**
	 * Implementation determines if the image should be disposed of by renderer or
	 * if image management is controlled in another way (TileCache for example.)
	 */
	public abstract void dispose();

	public void draw(SWTCanvasAdapter canv)
	{
		
		if (_image == null) {
			return;
		}
		int destX = _drawArea.x;
		int destY = _drawArea.y;
		int width = _drawArea.width;
		int height = _drawArea.height;

		int srcX = 0;
		int srcY = 0;
		if (destX < 0)
		{
			srcX += Math.abs(destX);
			width += destX;
			destX = 0;
		}

		if (destY < 0)
		{
			srcY += Math.abs(destY);
			height += destY;
			destY = 0;
		}

		if (width <= 0 || height <= 0) {
			return;
		}

		canv.drawSWTImage(_image, srcX, srcY, destX, destY, width, height, 255);

		if (TileCache.isDebug()) {
			canv.setColor(Color.YELLOW);
			canv.setLineWidth(1);
			canv.drawRect(destX, destY, width, height);
		}
	}

	@Override
	public String toString()
	{
		return "RenderTaskResult [debugInfo=" + _debugInfo + ", drawArea="
				+ _drawArea + "]";
	}

}
