package org.mwc.cmap.plotViewer.editors.chart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.core.preferences.ChartPrefsPage.PreferenceConstants;
import org.mwc.cmap.core.ui_support.swt.SWTCanvasAdapter;
import org.mwc.cmap.gt2plot.data.WMSLayers;
import org.mwc.cmap.gt2plot.proj.GeoToolsPainter;
import org.mwc.cmap.gt2plot.proj.GtProjection;

import MWC.Algorithms.PlainProjection;
import MWC.GUI.BlockingLayer;
import MWC.GUI.CanvasType;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GUI.Canvas.MetafileCanvas;
import MWC.GenericData.WorldArea;

public class Renderer
{

	public static final class UpdateTimer implements Runnable
	{

		private SWTCanvasAdapter _canvas;
		private Renderer _renderer;
		private boolean _forceUpdate;

		public UpdateTimer(SWTCanvasAdapter canv, Renderer renderer,
				boolean forceUpdate)
		{
			this._canvas = canv;
			this._renderer = renderer;
			this._forceUpdate = forceUpdate;
		}

		@Override
		public void run()
		{
			try
			{
				_renderer.paint(_canvas, _forceUpdate);
			}
			catch (InterruptedException e)
			{
				// this is ok because it means the render was cancelled before finishing
			}
			catch (ExecutionException e)
			{
				// This could be a problem so let's open a dialog with a warning for now
				e.printStackTrace();
				MessageDialog.openError(
						Display.getCurrent().getActiveShell(),
						"Error during Rendering",
						"Oops, something went wrong while rendering: "
								+ e.getLocalizedMessage());
			}
		}
	}
	/**
	 * The time to wait between checking to see if a new layer has been rendered.
	 */
	private static final int CANVAS_UPDATE_INTERVAL_MILLIS = 200;

	private static final int INITIAL_CANVAS_UPDATE_INTERVAL_MILLIS = 50;

	/**
	 * create the transparent image we need to for collating multiple layers into
	 * an image
	 * 
	 * @param myImageTemplate
	 *          the image we're going to copy
	 * @return
	 */
	protected static org.eclipse.swt.graphics.Image createSWTImage(
			ImageData myImageTemplate)
	{
		Color trColor = Color.white;
		int transPx = myImageTemplate.palette.getPixel(new RGB(trColor.getRed(),
				trColor.getGreen(), trColor.getBlue()));
		myImageTemplate.transparentPixel = transPx;
		org.eclipse.swt.graphics.Image image = new org.eclipse.swt.graphics.Image(
				Display.getCurrent(), myImageTemplate);
		return image;
	}

	protected Layers _theLayers;

	protected SWTCanvas _theCanvas;

	protected Image _swtImage;

	protected WorldArea _lastDataArea;

	/**
	 * our list of layered images.
	 */
	protected HashMap<Layer, Image> _renderedImageCache = new HashMap<Layer, Image>();

	private ExecutorService _executorService = Executors.newCachedThreadPool();

	private LinkedHashMap<Layer, Future<Image>> _futures = new LinkedHashMap<Layer, Future<Image>>();

	private WMSLayers _wmsLayer;

	public synchronized void clearImages()
	{

		// tell the images to clear themselves out
		Iterator<Image> iter = _renderedImageCache.values().iterator();
		while (iter.hasNext())
		{
			Object nextI = iter.next();
			if (nextI instanceof Image)
			{
				Image thisI = (Image) nextI;
				thisI.dispose();
			}
			else
			{
				CorePlugin.logError(IStatus.ERROR,
						"unexpected type of image found in buffer:" + nextI, null);
			}
		}

		// and clear out our buffered layers (they all need to be repainted
		// anyway)
		_renderedImageCache.clear();

		// also ditch the GeoTools image, if we have one
		if (_swtImage != null)
		{
			// hey, we're done
			_swtImage.dispose();
			_swtImage = null;
		}
	}

	protected boolean doubleBufferPlot()
	{
		return true;
	}

	/**
	 * paint the solid background.
	 * 
	 * @param dest
	 *          where we're painting to
	 */
	private void paintBackground(final CanvasType dest)
	{
		// right, don't fill in the background if we're not painting to the
		// screen
		boolean paintedBackground = false;

		// also plot any GeoTools stuff
		PlainProjection proj = dest.getProjection();

		// fill the background, to start with
		final Dimension sa = proj.getScreenArea();
		final int width = sa.width;
		final int height = sa.height;

		if (proj instanceof GtProjection)
		{
			GtProjection gp = (GtProjection) proj;

			// do we have a cached image?
			if (_swtImage == null)
			{
				// nope, do we have any data?
				if (gp.numLayers() > 0)
				{
					// now, GeoTools paint is an expensive operation, so I'm
					// going to do
					// all I can to avoid doing it. So, I'm going to see if any
					// of the
					// layers
					// overlap with the current drawing area
					if (gp.layersOverlapWith(proj.getVisibleDataArea()))
					{

						// now, if we're in relative projection mode, the
						// projection-translate doesn't get
						// performed until the first toScreen call. So do a
						// toScreen before
						// we start plotting the images
						proj.toScreen(proj.getDataArea().getCentre());

						BufferedImage img = GeoToolsPainter.drawAwtImage(width, height, gp,
								dest.getBackgroundColor());
						if (img != null)
						{
							ImageData swtImage = SWTCanvasAdapter.awtToSwt(img, width + 1, height + 1);
							_swtImage = new Image(Display.getCurrent(), swtImage);
						}
					}
				}
			}

			// ok, now we can paint it
			if (dest instanceof SWTCanvasAdapter)
			{
				if (_swtImage != null)
				{
					SWTCanvasAdapter swtC = (SWTCanvasAdapter) dest;
					int alpha = 255;
					String alphaStr = CorePlugin.getDefault().getPreferenceStore()
							.getString(PreferenceConstants.CHART_TRANSPARENCY);
					if (alphaStr != null)
						if (alphaStr.length() > 0)
							alpha = Integer.parseInt(alphaStr);
					swtC.drawSWTImage(_swtImage, 0, 0, width, height, alpha);
					paintedBackground = true;
				}
			}
			else if (dest instanceof MetafileCanvas)
			{
				// but do we have any data?
				if (gp.numLayers() > 0)
				{
					// yes, generate the image
					BufferedImage img = GeoToolsPainter.drawAwtImage(width, height, gp,
							dest.getBackgroundColor());
					dest.drawImage(img, 0, 0, width, height, null);
				}
			}

		}

		// have we painted the background yet?
		if (!paintedBackground)
		{
			// hey, we don't have GeoTools to paint for us, fill in the
			// background
			// but, only fill in the background if we're not painting to the
			// screen
			if (dest instanceof SWTCanvas)
			{
				final Color theCol = dest.getBackgroundColor();
				dest.setBackgroundColor(theCol);
				dest.fillRect(0, 0, width, height);
			}
		}
	}

	public synchronized void removeImage(Layer changedLayer)
	{
		// get the image
		Image theImage = _renderedImageCache.get(changedLayer);

		// and ditch the image
		if (theImage != null)
		{
			// dispose of the image
			theImage.dispose();

			// and delete that layer
			_renderedImageCache.remove(changedLayer);
		}

	}

	public synchronized void render(CanvasType dest)
	{

		if (!(dest instanceof SWTCanvasAdapter))
		{
			throw new IllegalArgumentException(dest + " is not an SWTCanvas");
		}
		// just double-check we have some layers (if we're part of an overview
		// chart, we may not have...)
		if (_theLayers == null)
			return;

		// check if we are currently rendering
		if (!_futures.isEmpty())
		{
			return;
		}

		// check that we have a valid canvas (that the sizes are set)
		final java.awt.Dimension sArea = dest.getProjection().getScreenArea();
		if (sArea != null)
		{
			if (sArea.width > 0)
			{

				// hey, we've plotted at least once, has the data area
				// changed?
				PlainProjection projection = getCanvasProjection();
				if (_lastDataArea != projection.getDataArea())
				{
					// remember the data area for next time
					_lastDataArea = projection.getDataArea();

					// clear out all of the layers we are using
					clearImages();
				}

				// we also clear the layers if we're in relative projection mode
				if (projection.getNonStandardPlotting())
				{
					clearImages();
				}

				int canvasHeight = getDrawHeightOfCanvas();
				int canvasWidth = getDrawWidthOfCanvas();
				ImageData _myImageTemplate = null;

				_wmsLayer = new WMSLayers();

				try
				{
					_wmsLayer.setCapabilities(new URL(
							"http://129.206.228.72/cached/osm?Request=GetCapabilities"));
				}
				catch (MalformedURLException e)
				{
					e.printStackTrace();
				}
				_wmsLayer.addLayerName("osm_auto:all");
				_myImageTemplate = renderLayer(dest, projection, canvasHeight,
						canvasWidth, _myImageTemplate, _wmsLayer);

				// ok, pass through the layers, repainting any which need it
				Enumeration<Layer> numer = _theLayers.sortedElements();
				while (numer.hasMoreElements())
				{
					final Layer thisLayer = numer.nextElement();

					_myImageTemplate = renderLayer(dest, projection, canvasHeight,
							canvasWidth, _myImageTemplate, thisLayer);
				}


				Display.getCurrent().timerExec(INITIAL_CANVAS_UPDATE_INTERVAL_MILLIS,
						new UpdateTimer((SWTCanvasAdapter) dest, this, true));
			}
		}
	}

	/**
	 * @param dest
	 * @param projection
	 * @param canvasHeight
	 * @param canvasWidth
	 * @param _myImageTemplate
	 * @param thisLayer
	 * @return
	 */
	protected synchronized ImageData renderLayer(CanvasType dest,
			PlainProjection projection, int canvasHeight, int canvasWidth,
			ImageData _myImageTemplate, final Layer thisLayer)
	{
		// do our double-buffering bit do we have a layer for this object
		org.eclipse.swt.graphics.Image image = _renderedImageCache.get(thisLayer);
		if (image == null)
		{
			// ok - do we have an image template?
			if (_myImageTemplate == null)
			{
				// nope, better create one
				Image template = new Image(Display.getCurrent(), canvasWidth,
						canvasHeight);
				// and remember it.
				_myImageTemplate = template.getImageData();

				// and ditch the template itself
				template.dispose();
			}

			// ok, paint the layer into this canvas
			RenderTask task = new RenderTask();
			task.setImageTemplate(_myImageTemplate);
			task.setDestCanvas(dest);
			task.setDataProjection(projection);
			task.setLayer(thisLayer);
			if (thisLayer instanceof BlockingLayer)
			{
				_futures.put(thisLayer, _executorService.submit(task));
			}
			else
			{
				_renderedImageCache.put(thisLayer, task.call());
			}
		}
		return _myImageTemplate;
	}

	synchronized void paint(SWTCanvasAdapter canv, boolean forceUpdate)
			throws InterruptedException, ExecutionException
	{
		if (canv.getProjection() == null)
		{
			// canvas has been disposed so we don't need to continue
			return;
		}
		Set<Entry<Layer, Future<Image>>> entrySet = _futures.entrySet();

		// first check if there is a new layer render for display
		boolean needsUpdate = forceUpdate;
		for (Map.Entry<Layer, Future<Image>> entry : entrySet)
		{
			if (entry.getValue().isDone()
					&& !_renderedImageCache.containsKey(entry.getKey()))
			{
				needsUpdate = true;
				break;
			}
		}

		if (needsUpdate)
		{
			// a new layer is ready so we will draw the map again
			paintBackground(canv);
			paintLayer(canv, _wmsLayer);
			Enumeration<Layer> elements = _theLayers.sortedElements();
			while (elements.hasMoreElements())
			{
				Layer layer = elements.nextElement();

				paintLayer(canv, layer);
			}

			canv.flush();
		}

		if (!_futures.isEmpty())
		{
			Display.getCurrent().timerExec(CANVAS_UPDATE_INTERVAL_MILLIS,
					new UpdateTimer(canv, this, false));
		}
		else
		{
			canv.close();
		}
	}

	/**
	 * @param canv
	 * @param layer
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	protected void paintLayer(SWTCanvasAdapter canv, Layer layer)
			throws InterruptedException, ExecutionException
	{
		Image image = _renderedImageCache.get(layer);

		if (image == null)
		{
			// see if it is still rendering
			Future<Image> future = _futures.get(layer);
			if (future != null && future.isDone())
			{
				image = future.get();
				_renderedImageCache.put(layer, image);
				_futures.remove(layer);
			}
		}

		if (image != null)
		{
			Rectangle bounds = image.getBounds();
			canv.drawSWTImage(image, 0, 0, bounds.width, bounds.height, 255);
//			ImageLoader loader = new ImageLoader();
//
//			loader.data = new ImageData[] {
//					image.getImageData()
//			};
//
//			loader.save("E:\\Downloads\\debrief-" + layer.getName() + ".png", SWT.IMAGE_PNG);
		}
	}

	/**
	 * Get the projection from the source canvas.
	 * 
	 * Template method, can be overridden.
	 */
	protected synchronized PlainProjection getCanvasProjection()
	{
		return _theCanvas.getProjection();
	}

	/**
	 * Get the width of the component.
	 * 
	 * Template method, can be overridden.
	 */
	protected synchronized int getDrawWidthOfCanvas()
	{
		return _theCanvas.getSize().width;
	}

	/**
	 * Get the height of the component.
	 * 
	 * Template method, can be overridden.
	 */
	protected synchronized int getDrawHeightOfCanvas()
	{
		return _theCanvas.getSize().height;
	}

	public synchronized void setTheCanvas(SWTCanvas _theCanvas)
	{
		this._theCanvas = _theCanvas;
	}

	public synchronized void setTheLayers(Layers _theLayers)
	{
		this._theLayers = _theLayers;
	}

	public synchronized void close()
	{
		_executorService.shutdownNow();
		clearImages();

	}

	public synchronized void cancel()
	{
		for (Future<?> future : _futures.values())
		{
			future.cancel(true);
		}
	}

}
