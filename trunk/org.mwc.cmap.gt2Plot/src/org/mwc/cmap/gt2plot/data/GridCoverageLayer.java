package org.mwc.cmap.gt2plot.data;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.gridcoverage2d.GridCoverageRenderer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.StyleBuilder;
import org.mwc.cmap.gt2plot.proj.GtProjection;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import MWC.GUI.BlockingLayer;
import MWC.GUI.CanvasType;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Plottable;
import MWC.GenericData.WorldArea;
import MWC.GenericData.WorldLocation;

import com.vividsolutions.jts.geom.Envelope;

public class GridCoverageLayer implements Layer, BlockingLayer
{
	private static final long serialVersionUID = -5081001576656982185L;
	private volatile boolean _visible = true;
	private volatile String _name;

	private Object _sourceObject;
	private RasterSymbolizer _symbolizer;

	public GridCoverageLayer()
	{
		StyleBuilder styleBuilder = new StyleBuilder();
		_symbolizer = styleBuilder.createRasterSymbolizer();
		_symbolizer.setOpacity(styleBuilder.literalExpression(1.0));
	}
	@Override
	public void append(Layer other)
	{
		// not supported now
	}

	@Override
	public void paint(final CanvasType dest)
	{
		GtProjection prj = (GtProjection) dest.getProjection();
		WorldArea visibleArea = prj.getVisibleDataArea();
		Dimension screenArea = prj.getScreenArea();
		CoordinateReferenceSystem destinationCRS = DefaultGeographicCRS.WGS84;
		Envelope envelope = JtsAdapter.toEnvelope(visibleArea);
		
		Rectangle screenSize = new Rectangle(screenArea);

		Graphics2D g2d = null;
		try
		{
			AffineTransform worldToScreen = RendererUtilities.worldToScreenTransform(envelope, screenSize, destinationCRS);
			GridCoverageRenderer gcr = new GridCoverageRenderer(destinationCRS,
					envelope, screenSize, worldToScreen);
			BufferedImage image = new BufferedImage(screenArea.width, screenArea.height,
					BufferedImage.TYPE_4BYTE_ABGR);
			g2d = image.createGraphics();
			GridCoverage2D coverage = read(_sourceObject);
			gcr.paint(g2d, coverage, _symbolizer);
			dest.drawImage(image, 0, 0, screenArea.width, screenArea.height, null);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			if (g2d != null)
			{
				g2d.dispose();
			}
		}
	}

	private GridCoverage2D read(Object _sourceObject2)
			throws IllegalArgumentException, IOException
	{
		AbstractGridFormat format = GridFormatFinder.findFormat(_sourceObject);
		AbstractGridCoverage2DReader reader = format.getReader(_sourceObject2);
		return reader.read(new GeneralParameterValue[0]);
	}

	@Override
	public boolean getVisible()
	{
		return _visible;
	}

	@Override
	public double rangeFrom(WorldLocation other)
	{
		return 0;
	}

	@Override
	public String getName()
	{
		return _name;
	}

	@Override
	public int compareTo(Plottable o)
	{
		return _name.compareTo(o.getName());
	}

	@Override
	public boolean hasEditor()
	{
		return false;
	}

	@Override
	public EditorType getInfo()
	{
		return null;
	}

	@Override
	public void exportShape()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public WorldArea getBounds()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setName(String val)
	{
		_name = val;
	}

	@Override
	public boolean hasOrderedChildren()
	{
		return true;
	}

	@Override
	public int getLineThickness()
	{
		return 0;
	}

	@Override
	public void add(Editable point)
	{
		System.out.println("not supported");
	}

	@Override
	public void removeElement(Editable point)
	{
		System.out.println("not supported");
	}

	@Override
	public Enumeration<Editable> elements()
	{
		return null;
	}

	@Override
	public synchronized void setVisible(boolean val)
	{
		_visible = val;
	}
	public synchronized void setImageFile(File file) {

		AbstractGridFormat format = GridFormatFinder.findFormat(file);
		if (format == null) {
			throw new IllegalArgumentException(file+" is not a recognized GridCoverage file");
		}
		this._sourceObject = file;
	}
	
	public synchronized Object getSourceObject()
	{
		return _sourceObject;
	}
	
	public synchronized void setSymbolizer(RasterSymbolizer symbolizer)
	{
		this._symbolizer = symbolizer;
	}
	
	public synchronized RasterSymbolizer getSymbolizer()
	{
		return _symbolizer;
	}
}
