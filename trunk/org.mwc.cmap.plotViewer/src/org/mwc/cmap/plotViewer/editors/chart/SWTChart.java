// Copyright MWC 1999, Debrief 3 Project
// $RCSfile: SWTChart.java,v $
// @author $Author$
// @version $Revision$
// $Log: SWTChart.java,v $
// Revision 1.41  2007/02/08 09:02:38  ian.mayo
// Minor tidying
//
// Revision 1.40  2007/01/25 15:53:57  ian.mayo
// Better GC maangement
//
// Revision 1.39  2007/01/04 16:23:57  ian.mayo
// Fix integration issue associated with buffer clearing
//
// Revision 1.38  2006/12/18 10:14:17  Ian.Mayo
// Improve comment
//
// Revision 1.37  2006/11/28 10:52:29  Ian.Mayo
// Improve management of double-buffering
//
// Revision 1.36  2006/08/11 08:24:31  Ian.Mayo
// Don't let repaints stack up
//
// Revision 1.35  2006/08/08 13:54:59  Ian.Mayo
// Remove dependency on Debrief classes
//
// Revision 1.34  2006/07/28 10:16:22  Ian.Mayo
// Reset normal cursor on mouse up
//
// Revision 1.33  2006/05/24 14:50:10  Ian.Mayo
// Always redraw the whole plot if in relative mode
//
// Revision 1.32  2006/04/26 12:39:46  Ian.Mayo
// Remove d-lines
//
// Revision 1.31  2006/04/21 08:13:51  Ian.Mayo
// keep a cached copy of the image - to reduce replotting time
//
// Revision 1.30  2006/04/11 08:10:42  Ian.Mayo
// Include support for mouse-move event (in addition to mouse-drag).
//
// Revision 1.29  2006/04/06 13:01:05  Ian.Mayo
// Ditch performance timers
//
// Revision 1.28  2006/04/05 08:15:42  Ian.Mayo
// Refactoring, improvements
//
// Revision 1.27  2006/02/23 11:48:31  Ian.Mayo
// Become selection provider
//
// Revision 1.26  2006/02/08 09:32:22  Ian.Mayo
// Eclipse tidying
//
// Revision 1.25  2006/01/20 14:10:29  Ian.Mayo
// Tidier plotting of background (ready for metafile plotting)
//
// Revision 1.24  2006/01/13 15:22:25  Ian.Mayo
// Minor refactoring, plus make sure we get the layers in sorted order (background & buffered before tracks)
//
// Revision 1.23  2006/01/03 14:03:33  Ian.Mayo
// Better right-click support
//
// Revision 1.22  2005/12/12 09:07:14  Ian.Mayo
// Minor tidying to comments
//
// Revision 1.21  2005/12/09 14:54:38  Ian.Mayo
// Add right-click property editing
//
// Revision 1.20  2005/09/29 15:29:46  Ian.Mayo
// Provide initial drag-mode (zoom)
//
// Revision 1.19  2005/09/16 10:11:37  Ian.Mayo
// Reflect changed mouse event signature
//
// Revision 1.18  2005/09/13 15:43:25  Ian.Mayo
// Try to get dragging modes working
//
// Revision 1.17  2005/09/13 13:46:25  Ian.Mayo
// Better drag mode support
//
// Revision 1.16  2005/08/31 15:03:38  Ian.Mayo
// Minor tidying
//
// Revision 1.15  2005/07/01 08:17:46  Ian.Mayo
// refactor, so we can override layer painting
//
// Revision 1.14  2005/06/14 15:21:03  Ian.Mayo
// fire update after zoom
//
// Revision 1.13  2005/06/14 09:49:29  Ian.Mayo
// Eclipse-triggered tidying (unused variables)
//
// Revision 1.12  2005/06/09 14:51:51  Ian.Mayo
// Implement SWT plotting
//
// Revision 1.11  2005/06/07 15:29:57  Ian.Mayo
// Add panel to show current cursor position
//
// Revision 1.10  2005/06/07 10:50:02  Ian.Mayo
// Ignore right-clicks for drag,mouse-up
//
// Revision 1.9  2005/05/26 14:04:51  Ian.Mayo
// Tidy up double-buffering
//
// Revision 1.8  2005/05/26 07:34:47  Ian.Mayo
// Minor tidying
//
// Revision 1.7  2005/05/25 15:31:55  Ian.Mayo
// Get double-buffering going
//
// Revision 1.6  2005/05/25 13:24:42  Ian.Mayo
// Tidy background painting
//
// Revision 1.5  2005/05/24 14:09:49  Ian.Mayo
// Sort out mouse-clicked events
//
// Revision 1.4  2005/05/24 13:26:43  Ian.Mayo
// Start including double-click support.
//
// Revision 1.3  2005/05/24 07:39:54  Ian.Mayo
// Start mouse support
//
// Revision 1.2  2005/05/20 15:34:45  Ian.Mayo
// Hey, practically working!
//
// Revision 1.1  2005/05/20 13:45:04  Ian.Mayo
// Start doing chart
//
//

package org.mwc.cmap.plotViewer.editors.chart;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.core.preferences.ChartPrefsPage.PreferenceConstants;
import org.mwc.cmap.core.property_support.EditableWrapper;
import org.mwc.cmap.core.property_support.RightClickSupport;
import org.mwc.cmap.core.ui_support.swt.SWTCanvasAdapter;
import org.mwc.cmap.gt2plot.proj.GeoToolsPainter;
import org.mwc.cmap.gt2plot.proj.GtProjection;
import org.mwc.cmap.plotViewer.actions.Pan;
import org.mwc.cmap.plotViewer.actions.ZoomIn;
import org.mwc.cmap.plotViewer.editors.udig.InteractiveChart;

import MWC.Algorithms.PlainProjection;
import MWC.GUI.BaseLayer;
import MWC.GUI.CanvasType;
import MWC.GUI.Editable;
import MWC.GUI.ExternallyManagedDataLayer;
import MWC.GUI.GeoToolsHandler;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GUI.PlainChart;
import MWC.GUI.Plottable;
import MWC.GUI.Canvas.MetafileCanvas;
import MWC.GUI.Tools.Chart.HitTester;
import MWC.GUI.Tools.Chart.RightClickEdit;
import MWC.GUI.Tools.Chart.RightClickEdit.ObjectConstruct;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

/**
 * The Chart is a canvas placed in a panel. the majority of functionality is
 * contained in the PlainChart parent class, only the raw comms is in this
 * class. This is configured by setting the listeners to the chart/panel to be
 * the listener functions defined in the parent.
 */
public abstract class SWTChart extends InteractiveChart implements ISelectionProvider
{

	// ///////////////////////////////////////////////////////////
	// member variables
	// //////////////////////////////////////////////////////////

	/**
	 * customised SWTCanvas class that supports our right-click editing
	 * 
	 * @author ian.mayo
	 */
	public abstract class CustomisedSWTCanvas extends SWTCanvas
	{
		private static final long serialVersionUID = 1L;

		public CustomisedSWTCanvas(Composite parent, GeoToolsHandler projection)
		{
			super(parent, (PlainProjection) projection);
		}

		/**
		 * @param menuManager
		 * @param selected
		 * @param theParentLayer
		 */
		abstract public void doSupplementalRightClickProcessing(
				MenuManager menuManager, Plottable selected, Layer theParentLayer);

		@Override
		protected void fillContextMenu(MenuManager mmgr, Point scrPoint,
				WorldLocation loc)
		{
			// let the parent do it's stuff
			super.fillContextMenu(mmgr, scrPoint, loc);

			// ok, get a handle to our layers
			Layers theData = getLayers();
			double layerDist = -1;

			// find the nearest editable item
			ObjectConstruct vals = new ObjectConstruct();
			int num = theData.size();
			for (int i = 0; i < num; i++)
			{
				Layer thisL = theData.elementAt(i);
				if (thisL.getVisible())
				{
					// find the nearest items, this method call will recursively pass down
					// through
					// the layers
					RightClickEdit.findNearest(thisL, loc, vals);

					if ((layerDist == -1) || (vals.distance < layerDist))
					{
						layerDist = vals.distance;
					}
				}
			}

			// ok, now retrieve the values produced by the range-finding algorithm
			Plottable res = vals.object;
			Layer theParent = vals.parent;
			double dist = vals.distance;
			Vector<Plottable> noPoints = vals.rangeIndependent;

			// see if this is in our dbl-click range
			if (HitTester.doesHit(new java.awt.Point(scrPoint.x, scrPoint.y), loc,
					dist, getProjection()))
			{
				// do nothing, we're all happy
			}
			else
			{
				res = null;
			}

			// have we found something editable?
			if (res != null)
			{
				// so get the editor
				Editable.EditorType e = res.getInfo();
				if (e != null)
				{
					RightClickSupport.getDropdownListFor(mmgr, new Editable[]
					{ res }, new Layer[]
					{ theParent }, new Layer[]
					{ theParent }, getLayers(), false);

					doSupplementalRightClickProcessing(mmgr, res, theParent);
				}
			}
			else
			{
				// not found anything useful,
				// so edit just the projection

				RightClickSupport.getDropdownListFor(mmgr, new Editable[]
				{ getProjection() }, null, null, getLayers(), true);

				// also see if there are any other non-position-related items
				if (noPoints != null)
				{
					// stick in a separator
					mmgr.add(new Separator());

					for (Iterator<Plottable> iter = noPoints.iterator(); iter.hasNext();)
					{
						final Plottable pl = iter.next();
						RightClickSupport.getDropdownListFor(mmgr, new Editable[]
						{ pl }, null, null, getLayers(), true);

						// ok, is it editable
						if (pl.getInfo() != null)
						{
							// ok, also insert an "Edit..." item
							Action editThis = new Action("Edit " + pl.getName() + " ...")
							{
								@Override
								public void run()
								{
									// ok, wrap the editab
									EditableWrapper pw = new EditableWrapper(pl, getLayers());
									ISelection selected = new StructuredSelection(pw);
									parentFireSelectionChanged(selected);
								}
							};

							mmgr.add(editThis);
							// hey, stick in another separator
							mmgr.add(new Separator());
						}
					}
				}
			}

			final CustomisedSWTCanvas chart = this;

			Action changeBackColor = new Action("Edit base chart")
			{
				@Override
				public void run()
				{
					EditableWrapper wrapped = new EditableWrapper(chart, getLayers());
					ISelection selected = new StructuredSelection(wrapped);
					parentFireSelectionChanged(selected);
				}

			};
			mmgr.add(changeBackColor);

			Action editProjection = new Action("Edit Projection")
			{
				@Override
				public void run()
				{
					EditableWrapper wrapped = new EditableWrapper(getProjection(),
							getLayers());
					ISelection selected = new StructuredSelection(wrapped);
					parentFireSelectionChanged(selected);
				}

			};
			mmgr.add(editProjection);

		}

		public abstract void parentFireSelectionChanged(ISelection selected);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	transient SWTCanvas _theCanvas;

	/**
	 * our list of layered images.
	 */
	protected transient HashMap<Layer, Image> _myLayers = new HashMap<Layer, Image>();

	/**
	 * the data area we last plotted (so that we know when a full layered repaint
	 * is needed).
	 */
	protected transient WorldArea _lastDataArea = null;

	/**
	 * keep a cached copy of the image - to reduce replotting time
	 */
	protected transient ImageData _myImageTemplate = null;

	/**
	 * keep track of if we're repainting, don't stack them up
	 */
	private boolean _repainting = false;

	private Image _swtImage;

	/**
	 * colour palette for our image
	 * 
	 */
	private static final PaletteData PALETTE_DATA = new PaletteData(0xFF0000,
			0xFF00, 0xFF);

	/** RGB value to use as transparent color */
	private static final int TRANSPARENT_COLOR = 0x123456;

	// ///////////////////////////////////////////////////////////
	// constructor
	// //////////////////////////////////////////////////////////
	/**
	 * constructor, providing us with the set of layers to plot.
	 * 
	 * @param theLayers
	 *          the data to plot
	 * @param _myProjection
	 */
	public SWTChart(final Layers theLayers, Composite parent,
			GeoToolsHandler _myProjection)
	{
		super(theLayers);
		_theCanvas = createCanvas(parent, (GtProjection) _myProjection);

		// sort out the area of coverage of the plot
		if (theLayers != null)
		{
			WorldArea area = theLayers.getBounds();
			_theCanvas.getProjection().setDataArea(area);
		}

		// add us as a painter to the canvas
		_theCanvas.addPainter(this);

		// catch any resize events
		_theCanvas.addControlListener(new ControlAdapter()
		{
			@Override
			public void controlResized(final ControlEvent e)
			{
				canvasResized();
			}
		});

		Dimension dim = _theCanvas.getSize();

		if (dim != null)
			_theCanvas.getProjection().setScreenArea(dim);

		_theCanvas.addMouseMoveListener(new MouseMoveListener()
		{

			@Override
			public void mouseMove(MouseEvent e)
			{
				doMouseMove(e);
			}
		});
		_theCanvas.addMouseListener(new MouseListener()
		{

			@Override
			public void mouseDoubleClick(MouseEvent e)
			{
				doMouseDoubleClick(e);
			}

			@Override
			public void mouseDown(MouseEvent e)
			{
				doMouseDown(e);
			}

			@Override
			public void mouseUp(MouseEvent e)
			{
				doMouseUp(e);
			}
		});
		_theCanvas.addMouseWheelListener(new MouseWheelListener()
		{
			@Override
			public void mouseScrolled(MouseEvent e)
			{
				// is ctrl held down?
				if ((e.stateMask & SWT.CONTROL) != 0)
				{
					// set the scale factor
					double scale = 1.1;

					// ok, which dir?
					if (e.count > 0)
					{
						scale = 1 / scale;
					}

					// get the projection to refit-itself
					getCanvas().getProjection().zoom(scale);

					// and force repaint
					update();

				}
			}
		});

		// create the tooltip handler
		_theCanvas.setTooltipHandler(new MWC.GUI.Canvas.BasicTooltipHandler(
				theLayers));

	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener)
	{
	}

	// ///////////////////////////////////////////////////////////
	// member functions
	// //////////////////////////////////////////////////////////
	@Override
	public void canvasResized()
	{

		clearImages();

		// and ditch our image template (since it's size related)
		_myImageTemplate = null;

		// now we've cleared the layers, call the parent resize method (which causes
		// a repaint
		// of the layers)
		super.canvasResized();
	}

	public abstract void chartFireSelectionChanged(ISelection sel);

	/**
	 * ditch the images we're remembering
	 */
	private void clearImages()
	{
		// tell the images to clear themselves out
		Iterator<Image> iter = _myLayers.values().iterator();
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

		// and clear out our buffered layers (they all need to be repainted anyway)
		_myLayers.clear();

		// also ditch the GeoTools image, if we have one
		if (_swtImage != null)
		{
			// hey, we're done
			_swtImage.dispose();
			_swtImage = null;
		}

	}

	/**
	 * provide method to clear stored data.
	 */
	@Override
	public void close()
	{
		// clear the layers
		clearImages();
		_myLayers = null;

		// and ditch the image template
		_myImageTemplate = null;

		// instruct the canvas to close
		_theCanvas.close();
		_theCanvas = null;

		super.close();
	}

	/**
	 * over-rideable member function which allows us to over-ride the canvas which
	 * gets used.
	 * 
	 * @param projection
	 * 
	 * @return the Canvas to use
	 */
	public SWTCanvas createCanvas(Composite parent, GtProjection projection)
	{
		return new CustomisedSWTCanvas(parent, projection)
		{

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void doSupplementalRightClickProcessing(MenuManager menuManager,
					Plottable selected, Layer theParentLayer)
			{
			}

			@Override
			public void parentFireSelectionChanged(ISelection selected)
			{
				chartFireSelectionChanged(selected);
			}
		};
	}

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

	/**
	 * property to indicate if we are happy to perform double-buffering. -
	 * override it to change the response
	 */
	protected boolean doubleBufferPlot()
	{
		return true;
	}

	@Override
	public final SWTCanvas getCanvas()
	{
		return _theCanvas;
	}

	public final Control getCanvasControl()
	{
		return _theCanvas.getCanvas();
	}

	// ////////////////////////////////////////////////////////
	// methods for handling requests from our canvas
	// ////////////////////////////////////////////////////////

	@Override
	public final Component getPanel()
	{
		System.err.println("NOT RETURNING PANEL");
		return null;
		// return _theCanvas;
	}

	/**
	 * get the size of the canvas.
	 * 
	 * @return the dimensions of the canvas
	 */
	@Override
	public final java.awt.Dimension getScreenSize()
	{
		Dimension dim = _theCanvas.getSize();
		// get the current size of the canvas
		return dim;
	}

	@Override
	public ISelection getSelection()
	{
		return null;
	}

	public final SWTCanvas getSWTCanvas()
	{
		return _theCanvas;
	}

	/**
	 * paint the solid background.
	 * 
	 * @param dest
	 *          where we're painting to
	 */
	protected void paintBackground(final CanvasType dest)
	{
		// right, don't fill in the background if we're not painting to the screen
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
					// now, GeoTools paint is an expensive operation, so I'm going to do
					// all I can to avoid doing it. So, I'm going to see if any of the
					// layers
					// overlap with the current drawing area
					if (gp.layersOverlapWith(proj.getVisibleDataArea()))
					{

						// now, if we're in relative projection mode, the
						// projection-translate doesn't get
						// performed until the first toScreen call. So do a toScreen before
						// we start plotting the images
						proj.toScreen(proj.getDataArea().getCentre());

						BufferedImage img = GeoToolsPainter.drawAwtImage(width, height, gp,
								dest.getBackgroundColor());
						if (img != null)
						{
							ImageData swtImage = awtToSwt(img, width + 1, height + 1);
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
			// hey, we don't have GeoTools to paint for us, fill in the background
			// but, only fill in the background if we're not painting to the screen
			if (dest instanceof SWTCanvas)
			{
				final Color theCol = dest.getBackgroundColor();
				dest.setBackgroundColor(theCol);
				dest.fillRect(0, 0, width, height);
			}
		}
	}

	/**
	 * over-ride the parent's version of paint, so that we can try to do it by
	 * layers.
	 */
	@Override
	public void paintMe(final CanvasType dest)
	{
		// just double-check we have some layers (if we're part of an overview
		// chart, we may not have...)
		if (_theLayers == null)
			return;

		if (_repainting)
		{
			return;
		}
		else
		{
			_repainting = true;
		}

		try
		{

			// check that we have a valid canvas (that the sizes are set)
			final java.awt.Dimension sArea = dest.getProjection().getScreenArea();
			if (sArea != null)
			{
				if (sArea.width > 0)
				{

					// hey, we've plotted at least once, has the data area changed?
					if (_lastDataArea != _theCanvas.getProjection().getDataArea())
					{
						// remember the data area for next time
						_lastDataArea = _theCanvas.getProjection().getDataArea();

						// clear out all of the layers we are using
						clearImages();
					}

					// we also clear the layers if we're in relative projection mode
					if (_theCanvas.getProjection().getNonStandardPlotting())
					{
						clearImages();
					}

					int canvasHeight = _theCanvas.getSize().height;
					int canvasWidth = _theCanvas.getSize().width;

					paintBackground(dest);

					// ok, pass through the layers, repainting any which need it
					Enumeration<Layer> numer = _theLayers.sortedElements();
					while (numer.hasMoreElements())
					{
						final Layer thisLayer = numer.nextElement();

						boolean isAlreadyPlotted = false;

						// just check if this layer is visible
						if (thisLayer.getVisible())
						{
							// System.out.println("painting:" + thisLayer.getName());

							if (doubleBufferPlot())
							{
								// check we're plotting to a SwingCanvas, because we don't
								// double-buffer anything else
								if (dest instanceof SWTCanvas)
								{
									// does this layer want to be double-buffered?
									if (thisLayer instanceof BaseLayer)
									{
										// just check if there is a property which over-rides the
										// double-buffering
										final BaseLayer bl = (BaseLayer) thisLayer;
										if (bl.isBuffered())
										{
											isAlreadyPlotted = true;

											// do our double-buffering bit
											// do we have a layer for this object
											org.eclipse.swt.graphics.Image image = _myLayers
													.get(thisLayer);
											if (image == null)
											{
												// ok - do we have an image template?
												if (_myImageTemplate == null)
												{
													// nope, better create one
													Image template = new Image(Display.getCurrent(),
															canvasWidth, canvasHeight);
													// and remember it.
													_myImageTemplate = template.getImageData();

													// and ditch the template itself
													template.dispose();
												}

												// ok, and now the SWT image
												image = createSWTImage(_myImageTemplate);

												// we need to wrap it into a GC so we can write to it.
												GC newGC = new GC(image);

												// in Windows 7 & OSX we've had problem where
												// anti-aliased text bleeds through assigned
												// transparent shade. This makes the text look really
												// blurry. So, turn off anti-aliasd text
												newGC.setTextAntialias(SWT.OFF);

												// wrap the GC into something we know how to plot to.
												SWTCanvasAdapter ca = new SWTCanvasAdapter(
														dest.getProjection());
												ca.setScreenSize(_theCanvas.getProjection()
														.getScreenArea());

												// and store the GC
												ca.startDraw(newGC);

												// ok, paint the layer into this canvas
												thisLayer.paint(ca);

												// done.
												ca.endDraw(null);

												// store this image in our list, indexed by the layer
												// object itself
												_myLayers.put(thisLayer, image);

												// and ditch the GC
												newGC.dispose();
											}

											// have we ended up with an image to paint?
											if (image != null)
											{
												// get the graphics to paint to
												SWTCanvas canv = (SWTCanvas) dest;

												// lastly add this image to our Graphics object
												canv.drawSWTImage(image, 0, 0, canvasWidth,
														canvasHeight, 255);
											}

										}
									}
								} // whether we were plotting to a SwingCanvas (which may be
								// double-buffered
							} // whther we are happy to do double-buffering

							// did we manage to paint it
							if (!isAlreadyPlotted)
							{
								paintThisLayer(thisLayer, dest);

								isAlreadyPlotted = true;
							}
						}
					}

				}
			}
		}
		finally
		{
			_repainting = false;
		}

	}

	/**
	 * Convenience method added, to allow child classes to override how we plot
	 * non-background layers. This was originally inserted to let us support snail
	 * trails
	 * 
	 * @param thisLayer
	 * @param dest
	 */
	protected void paintThisLayer(final Layer thisLayer, CanvasType dest)
	{
		// draw into it
		thisLayer.paint(dest);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener)
	{
	}

	@Override
	public final void repaint()
	{
		// we were doing a repaint = now an updaet
		_theCanvas.updateMe();
	}

	@Override
	public final void repaintNow(final java.awt.Rectangle rect)
	{
		_theCanvas.redraw(rect.x, rect.y, rect.width, rect.height, true);
		// _theCanvas.paintImmediately(rect);
	}

	@Override
	public final void rescale()
	{
		// do a rescale
		_theCanvas.rescale();

	}

	/**
	 * specify whether paint events should get deferred, with only the most recent
	 * one getting painted
	 * 
	 * @param val
	 *          yes/no
	 */
	public void setDeferPaints(boolean val)
	{
		_theCanvas.setDeferPaints(val);
	}

	@Override
	public void setSelection(ISelection selection)
	{
	}

	@Override
	public final void update()
	{
		// clear out the layers object
		clearImages();

		// and start the update
		_theCanvas.updateMe();
	}

	@Override
	public final void update(final Layer changedLayer)
	{
		if (changedLayer == null)
		{
			update();
		}
		else
		{
			// get the image
			Image theImage = _myLayers.get(changedLayer);

			// and ditch the image
			if (theImage != null)
			{
				// dispose of the image
				theImage.dispose();

				// and delete that layer
				_myLayers.remove(changedLayer);
			}

			// NO, don't GC. If we change lots of items, we do lots of garbage
			// collections, and each
			// one takes a finite time. Leave the app to do it on it's own
			// --- chuck in a GC, to clear the old image allocation
			// --- System.gc();

			// hey, it's not one of our GeoTools layers is it?
			if (changedLayer instanceof ExternallyManagedDataLayer)
			{
				// ok, ditch the swt image
				if (_swtImage != null)
				{
					_swtImage.dispose();
					_swtImage = null;
				}
			}

			// and trigger update
			_theCanvas.updateMe();

		}
	}

	public static ImageData awtToSwt(BufferedImage bufferedImage, int width,
			int height)
	{
		System.err.println("DOING AWT TO SWT!!!!");
		final int[] awtPixels = new int[width * height];
		ImageData swtImageData = new ImageData(width, height, 24, PALETTE_DATA);
		swtImageData.transparentPixel = TRANSPARENT_COLOR;
		int step = swtImageData.depth / 8;
		final byte[] data = swtImageData.data;
		bufferedImage.getRGB(0, 0, width, height, awtPixels, 0, width);
		for (int i = 0; i < height; i++)
		{
			int idx = (0 + i) * swtImageData.bytesPerLine + 0 * step;
			for (int j = 0; j < width; j++)
			{
				int rgb = awtPixels[j + i * width];
				for (int k = swtImageData.depth - 8; k >= 0; k -= 8)
				{
					data[idx++] = (byte) ((rgb >> k) & 0xFF);
				}
			}
		}

		return swtImageData;
	}

}
