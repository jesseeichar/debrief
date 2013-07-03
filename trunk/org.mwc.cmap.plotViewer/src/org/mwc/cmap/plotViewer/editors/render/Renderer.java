package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.mwc.cmap.core.ui_support.swt.SWTCanvasAdapter;
import org.mwc.cmap.gt2plot.data.GridCoverageLayer;
import org.mwc.cmap.gt2plot.data.JtsAdapter;
import org.mwc.cmap.gt2plot.proj.GtProjection;
import org.mwc.cmap.plotViewer.editors.chart.SWTCanvas;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import MWC.Algorithms.PlainProjection;
import MWC.GUI.BlockingLayer;
import MWC.GUI.CanvasType;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GenericData.WorldArea;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

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

	protected WorldArea _lastDataArea;

	private TileCacheManager _tileCache;

	private Map<Layer, Image> _images = new HashMap<Layer, Image>();

	private ExecutorService _executorService = Executors.newCachedThreadPool();

	private List<RenderFuture> _futures = new ArrayList<RenderFuture>();

	private boolean _rendering = false;

	private Map<String, GridCoverageLayer> _gcLayers = new HashMap<String, GridCoverageLayer>();

	protected boolean doubleBufferPlot()
	{
		return true;
	}

	public synchronized void removeImage(Layer changedLayer)
	{
		_tileCache.remove(changedLayer);
	}

	public synchronized void render(CanvasType dest)
	{

		DebugLogger.log("start rendering");

		if (!(dest instanceof SWTCanvasAdapter))
		{
			throw new IllegalArgumentException(dest + " is not an SWTCanvas");
		}

		// just double-check we have some layers (if we're part of an overview
		// chart, we may not have...)
		if (_theLayers == null)
		{
			DebugLogger.log("no Layers stopping rendering");
			return;
		}

		// check if we are currently rendering
		if (_rendering)
		{
			DebugLogger.log("already rendering");
			return;
		}

		_rendering = true;
		_futures.clear();

		// check that we have a valid canvas (that the sizes are set)
		final java.awt.Dimension sArea = dest.getProjection().getScreenArea();

		if (sArea != null)
		{
			if (sArea.width > 0)
			{

				ReferencedEnvelope envelope = JtsAdapter
						.toEnvelope(getCanvasProjection().getDataArea());

				Dimension dimension = getCanvasProjection().getScreenArea();
				StringBuilder builder = new StringBuilder("Starting to render:");
				builder
						.append("\n\t")
						.append(
								"before scale: "
										+ JtsAdapter.toEnvelope(_theCanvas.getProjection()
												.getDataArea()))
						.append("\n\t")
						.append("after scale: " + envelope)
						.append("\n\t")
						.append("center: " + envelope.centre())
						.append("\n\t")
						.append("Scale: " + _tileCache.getClosestScale(envelope, dimension))
						.append("\n\t").append("ScreenSize: " + dimension);

				DebugLogger.log(builder.toString());

				// hey, we've plotted at least once, has the data area
				// changed?
				PlainProjection projection = getCanvasProjection();
				if (_lastDataArea != projection.getDataArea())
				{
					// remember the data area for next time
					_lastDataArea = projection.getDataArea();
				}

				ImageData _myImageTemplate = null;

				int canvasWidth = getCanvasProjection().getScreenArea().width;
				int canvasHeight = getCanvasProjection().getScreenArea().height;

				// ok, pass through the layers, repainting any which need it
				// Enumeration<Layer> numer = _theLayers.sortedElements();

				// The following is just to add the hardcoded files. For integration
				// deleted.
				Enumeration<Layer> numer = new Enumeration<Layer>()
				{
					Iterator<String> hardCoded = Arrays
							.asList("wsiearth.tif"/*, "clds.tif"*/).iterator();
					Enumeration<Layer> actualLayers = _theLayers.sortedElements();

					@Override
					public boolean hasMoreElements()
					{
						return hardCoded.hasNext() || actualLayers.hasMoreElements();
					}

					@Override
					public Layer nextElement()
					{
						if (hardCoded.hasNext())
						{
							String fileName = hardCoded.next();
							GridCoverageLayer layer;
							if (!_gcLayers.containsKey(fileName))
							{
								layer = new org.mwc.cmap.gt2plot.data.GridCoverageLayer();
								layer.setImageFile(new File(fileName));
								layer.setVisible(true);

								_gcLayers.put(fileName, layer);
							}
							else
							{
								layer = _gcLayers.get(fileName);
							}
							return layer;
						}
						else
						{
							return actualLayers.nextElement();
						}
					}
				};
				while (numer.hasMoreElements())
				{
					final Layer thisLayer = numer.nextElement();

					_myImageTemplate = renderLayer(dest, projection, canvasHeight,
							canvasWidth, _myImageTemplate, thisLayer);
				}

				int done = 0;
				for (RenderFuture future : _futures)
				{
					if (future.isCancelled() || future.isDone())
					{
						done++;
					}
				}

				if (done == _futures.size())
				{
					try
					{
						paint((SWTCanvasAdapter) dest, true);
					}
					catch (InterruptedException e)
					{
						throw new RuntimeException(e);
					}
					catch (ExecutionException e)
					{
						throw new RuntimeException(e);
					}
				}
				if (done < _futures.size())
				{
					Display.getCurrent().timerExec(CANVAS_UPDATE_INTERVAL_MILLIS,
							new UpdateTimer((SWTCanvasAdapter) dest, this, true));
				}
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
		if (thisLayer instanceof BlockingLayer)
		{
			TileCache tileCache = _tileCache
					.getTileCache(thisLayer, _myImageTemplate);
			Dimension dimension = new Dimension(canvasWidth, canvasHeight);

			Envelope envelope = JtsAdapter.toEnvelope(projection.getDataArea());
			double scale = _tileCache.getClosestScale(envelope, dimension);

			Coordinate centre = envelope.centre();
			PositionedTile[][] tiles = tileCache.getTiles(dimension, scale, centre);

			for (int i = 0; i < tiles.length; i++)
			{
				PositionedTile[] tileColumn = tiles[i];
				for (int j = 0; j < tileColumn.length; j++)
				{
					PositionedTile tile = tileColumn[j];
					TileRenderTask task = new TileRenderTask();
					task.setDestCanvas(dest);
					task.setDataProjection(projection);
					task.setTile(i, j, tile);

					switch (tile.getState())
					{
					case READY:
						_futures.add(new RenderFuture(new SynchronousFuture(task)));
						break;
					case ERROR:
					case LOADING:
					case BLANK:
						_futures.add(new RenderFuture(_executorService.submit(task)));
						break;
					default:
						throw new RuntimeException("Unkown value: " + tile.getState());
					}
				}
			}
		}
		else
		{
			LayerRenderTask task = new LayerRenderTask();

			Image image = _images.get(thisLayer);
			if (image == null)
			{
				task.setImageTemplate(_myImageTemplate);
			}
			else
			{
				task.setImage(image);
			}
			task.setDestCanvas(dest);
			task.setDataProjection(projection);
			task.setLayer(thisLayer);

			_futures.add(new RenderFuture(new SynchronousFuture(task)));
		}
		return _myImageTemplate;
	}

	synchronized void paint(SWTCanvasAdapter canv, boolean forceUpdate)
			throws InterruptedException, ExecutionException
	{
		if (canv.getProjection() == null)
		{
			_futures.clear();
			// canvas has been disposed so we don't need to continue
			return;
		}
		// first check if there is a new layer render for display
		boolean needsUpdate = forceUpdate;
		for (RenderFuture renderFuture : _futures)
		{
			if (renderFuture.isDone() && !renderFuture.hasBeenDisplayed())
			{
				needsUpdate = true;
				break;
			}
		}

		int done = 0;
		if (needsUpdate)
		{
			// a new layer is ready so we will draw the map again
			for (RenderFuture future : _futures)
			{
				done += paintRenderFuture(canv, future);
			}

			canv.flush();
		}

		this._rendering = done < _futures.size();
		if (_rendering)
		{
			Display.getCurrent().timerExec(CANVAS_UPDATE_INTERVAL_MILLIS,
					new UpdateTimer(canv, this, false));
		}
		else
		{
			DebugLogger.log("finished rendering: " + done + " out of "
					+ _futures.size() + " tasks");
			for (RenderFuture future : _futures)
			{
				future.dispose();
			}
			_futures.clear();
		}
	}

	/**
	 * @param canv
	 * @param future2
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	protected int paintRenderFuture(SWTCanvasAdapter canv, RenderFuture future)
			throws ExecutionException
	{

		if (future.isDone())
		{
			try
			{
				RenderTaskResult result = future.get();
				result.draw(canv);
			}
			catch (InterruptedException e)
			{
				// indicates render has been cancelled.
			}
			catch (java.util.concurrent.CancellationException e)
			{
				// indicates render has been cancelled.
			}
			return 1;
		}
		else
		{
			return future.isCancelled() ? 1 : 0;
		}

	}

	/**
	 * Get the projection from the source canvas.
	 * 
	 * Template method, can be overridden.
	 */
	private synchronized PlainProjection getCanvasProjection()
	{
		GtProjection rawProjection = (GtProjection) _theCanvas.getProjection();
		GtProjection gtProjection = new GtProjection();
		WorldArea dataArea = rawProjection.getDataArea();
		Envelope envelope = JtsAdapter.toEnvelope(dataArea);
		Dimension dimension = rawProjection.getScreenArea();

		gtProjection.setScreenArea(rawProjection.getScreenArea());

		double scale = _tileCache.getClosestScale(envelope, dimension);
		Envelope bounds = _tileCache.calculateBounds(dimension, scale,
				envelope.centre());
		gtProjection.setDataArea(JtsAdapter.toWorldArea(bounds));

		return rawProjection;
	}

	public synchronized void setTheCanvas(SWTCanvas _theCanvas)
	{
		this._theCanvas = _theCanvas;
		double dpi = 90;
		CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
		this._tileCache = new TileCacheManager(new Dimension(256, 256), 40, 1,
				new Envelope(-180, 180, -90, 90), dpi, -1, crs);
	}

	public synchronized void setTheLayers(Layers _theLayers)
	{
		this._theLayers = _theLayers;
	}

	public synchronized void close()
	{
		_executorService.shutdownNow();
		clearCaches();
	}

	/**
	 * 
	 */
	protected void clearCaches()
	{
		_tileCache.clear();
		for (Image image : _images.values())
		{
			image.dispose();
		}

		_images.clear();
	}

	public synchronized void cancel()
	{
		DebugLogger.log("starting cancelling");
		long start = System.currentTimeMillis();
		for (Iterator<RenderFuture> iterator = _futures.iterator(); iterator
				.hasNext();)
		{
			RenderFuture future = iterator.next();
			future.cancel(true);
			if (future.isDone() || future.isCancelled())
			{
				future.dispose();
				iterator.remove();
			}
		}

		// try to cancel within 3 seconds and if impossible then bail.
		while (!_futures.isEmpty() && System.currentTimeMillis() - start < 3000)
		{
			synchronized (this)
			{
				try
				{
					this.wait(50);
				}
				catch (InterruptedException e)
				{
					// continue?
				}
			}

			for (Iterator<RenderFuture> iterator = _futures.iterator(); iterator
					.hasNext();)
			{
				RenderFuture future = iterator.next();
				future.cancel(true);
				if (future.isDone() || future.isCancelled())
				{
					future.dispose();
					iterator.remove();
				}
			}
		}

		DebugLogger.log("done cancelling: " + _futures.size() + " tasks");
		_futures.clear();
		_rendering = false;

	}

	public void canvasResized()
	{
		clearCaches();
	}

}
