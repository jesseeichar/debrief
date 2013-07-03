package org.mwc.cmap.plotViewer.editors.render.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.mwc.cmap.plotViewer.editors.render.PositionedTile;
import org.mwc.cmap.plotViewer.editors.render.TileCache;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class LatLongTileCacheTest
{
	private TileCache _tileCache;

	@Before
	public void before()
	{
		_tileCache = new TileCache(new Dimension(512, 512),
				TileCache.DEFAULT_WGS84, new Coordinate(-180, -90), 90, -1,
				DefaultGeographicCRS.WGS84, new TestTileLoader(), null);
	}

	@Test
	public void testGetClosestScale()
	{
		double scale = _tileCache.getClosestScale(new Envelope(4.949, 5.4755,
				46.5204, 46.7463), new Dimension(1500, 650));

		assertEquals(10000000, scale, 0.0000001);
	}

	@Test
	public void testCalculateBoundsCenteredSmallScale()
			throws MismatchedDimensionException, TransformException, FactoryException
	{
		asserCorrectCalculateBounds(new Dimension(1500, 650),
				TileCache.DEFAULT_WGS84[5], new Coordinate(0, 0));
	}

	@Test
	public void testCalculateBoundsCloseToPoleLargeScale()
			throws MismatchedDimensionException, TransformException, FactoryException
	{
		asserCorrectCalculateBounds(new Dimension(1500, 650),
				TileCache.DEFAULT_WGS84[15], new Coordinate(0, 89.99999));
	}

	@Test
	public void testCalculateBoundsCloseToDatelineLargeScale()
			throws MismatchedDimensionException, TransformException, FactoryException
	{
		asserCorrectCalculateBounds(new Dimension(1500, 650),
				TileCache.DEFAULT_WGS84[15], new Coordinate(-180, 0));
	}

	@Test
	public void testCalculateBoundsCenteredLargeScale()
			throws MismatchedDimensionException, TransformException, FactoryException
	{
		asserCorrectCalculateBounds(new Dimension(1, 1),
				TileCache.DEFAULT_WGS84[15], new Coordinate(0, 0));

	}

	/**
	 * Check that calculate bounds give expected value
	 */
	protected void asserCorrectCalculateBounds(Dimension dimension,
			double desiredScale, Coordinate center) throws TransformException,
			FactoryException
	{
		Envelope bounds = _tileCache.calculateBounds(dimension, desiredScale,
				center);
		double scale = _tileCache.calculateScale(bounds, dimension);
		assertEquals(desiredScale, scale, 1);

		double ratio = ((double) dimension.width) / dimension.height;

		assertEquals(ratio, bounds.getWidth() / bounds.getHeight(), 0.000001);
	}

	@Test
	public void testGetTiles()
	{
		Dimension screenSize = new Dimension(900, 640);
		int scale = 1000000;
		Coordinate center = new Coordinate(1.34, 1.44);
		PositionedTile[][] tiles = _tileCache.getTiles(screenSize, scale, center);
		Envelope expectedBounds = _tileCache.calculateBounds(screenSize, scale,
				center);
		Envelope actualBounds = new Envelope(center);

		for (PositionedTile[] column : tiles)
		{
			for (PositionedTile tile : column)
			{
				actualBounds.expandToInclude(tile.getBounds());
			}
		}

		assertTrue(actualBounds.contains(expectedBounds));
	}
	@Test
	public void testCalculateScales()
	{
		TileCache tc = new TileCache(new Dimension(512, 512), 10, new Envelope(
				-0.001, 0.001, -0.001, 0.001),
				new Envelope(-180, 180, -90, 90), 90, -1, DefaultGeographicCRS.WGS84,
				new TestTileLoader(), null);

		double[] scales = tc.getScales();

		assertEquals(10, scales.length);

		for (int i = 1; i < scales.length; i++)
		{
			assertTrue(scales[i - 1] < scales[i]);
		}
	}
	@Test
	public void testCalculateScales2()
	{
		TileCache tc = new TileCache(new Dimension(512, 512), 10, 1,
				new Envelope(-180, 180, -90, 90), 90, -1, DefaultGeographicCRS.WGS84,
				new TestTileLoader(), null);
		
		double[] scales = tc.getScales();
		
		assertEquals(10, scales.length);
		
		for (int i = 1; i < scales.length; i++)
		{
			assertTrue(scales[i - 1] < scales[i]);
		}
	}
}
