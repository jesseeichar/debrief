package org.mwc.cmap.plotViewer.editors.render;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.mwc.cmap.core.ui_support.swt.SWTCanvasAdapter;

import MWC.GUI.Layer;

public class LayerRenderTask extends AbstractRenderTask
{
	private Layer _layer;

	public void setLayer(Layer thisLayer)
	{
		this._layer = thisLayer;
	}

	protected ImageData _myImageTemplate;
	private Image _image;

	public void setImageTemplate(ImageData myImageTemplate)
	{
		_myImageTemplate = myImageTemplate;
	}

	public void setImage(Image image)
	{
		this._image = image;
	}

	@Override
	public RenderTaskResult call()
	{
		// just check if this layer is visible
		if (isVisible())
		{
			try
			{
				// ok, and now the SWT image
				Image image = this._image;
				if (this._image == null) {
					image = Renderer.createSWTImage(_myImageTemplate);
				}

				// we need to wrap it into a GC so we can write to it.
				GC newGC = new GC(image);

				// in Windows 7 & OSX we've had problem where
				// anti-aliased text bleeds through assigned
				// transparent shade. This makes the text look really
				// blurry. So, turn off anti-aliasd text
				newGC.setTextAntialias(SWT.OFF);

				// wrap the GC into something we know how to plot to.
				SWTCanvasAdapter ca = new SWTCanvasAdapter(_destCanvas.getProjection());
				ca.setScreenSize(_projection.getScreenArea());

				// and store the GC
				ca.startDraw(newGC);
				paint(ca);

				// done.
				ca.endDraw(null);

				// and ditch the GC
				newGC.dispose();

				Rectangle bounds = image.getBounds();
				return new RenderTaskResult(image, new Rectangle(0, 0, bounds.width, bounds.height), _layer.getName())
				{

					@Override
					public void dispose()
					{
						_image.dispose();
					}
				};
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	protected boolean isVisible()
	{
		return _layer.getVisible();
	}

	protected void paint(SWTCanvasAdapter ca)
	{
		_layer.paint(ca);
	}

}
