package org.mwc.cmap.plotViewer.editors.render;

import java.util.concurrent.Callable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.mwc.cmap.core.ui_support.swt.SWTCanvasAdapter;

import MWC.Algorithms.PlainProjection;
import MWC.GUI.CanvasType;
import MWC.GUI.Layer;

public class RenderTask implements Callable<Image>
{

	private ImageData _myImageTemplate;
	private CanvasType _destCanvas;
	private PlainProjection _projection;
	private Layer _layer;

	public void setImageTemplate(ImageData myImageTemplate)
	{
		_myImageTemplate = myImageTemplate;
	}

	public void setDestCanvas(CanvasType dest)
	{
		this._destCanvas = dest;
	}

	public void setDataProjection(PlainProjection projection)
	{
		this._projection = projection;

	}

	public void setLayer(Layer thisLayer)
	{
		this._layer = thisLayer;
	}

	@Override
	public Image call()
	{
		// just check if this layer is visible
		if (_layer.getVisible())
		{
			try
			{
				// ok, and now the SWT image
				Image image = Renderer.createSWTImage(_myImageTemplate);

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
				_layer.paint(ca);

				// done.
				ca.endDraw(null);

				// and ditch the GC
				newGC.dispose();

				return image;
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
}
