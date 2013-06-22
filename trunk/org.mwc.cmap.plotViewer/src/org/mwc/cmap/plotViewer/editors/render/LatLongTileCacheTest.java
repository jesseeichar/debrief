package org.mwc.cmap.plotViewer.editors.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.awt.Dimension;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.AssertionFailedException;

public class LatLongTileCacheTest
{
	private TileCache _tileCache;

	@Before
	public void before()
	{
		_tileCache = new TileCache(new Dimension(512, 512),
				TileCache.DEFAULT_WGS84, new Coordinate(-180, -90), 90, -1,
				DefaultGeographicCRS.WGS84, new TestTileLoader());
	}

	@Test
	public void testGetClosestScaleExceptionalCases()
	{
		double scale = _tileCache.getClosestScale(new Envelope(-180, 180, -90, 90),
				new Dimension(5, 5));

		assertEquals(TileCache.DEFAULT_WGS84[TileCache.DEFAULT_WGS84.length - 1],
				scale, 0.0000001);

		try
		{
			scale = _tileCache.getClosestScale(new Envelope(-180, 180, -90, 90),
					new Dimension(5000000, 5000000));
			fail("Expected Assertion error because dimension is too big");
		}
		catch (AssertionFailedException e)
		{
			// good
		}

		scale = _tileCache.getClosestScale(new Envelope(0, 0.00001, 0, 0.000001),
				new Dimension(5000, 5000));

		assertEquals(TileCache.DEFAULT_WGS84[0], scale, 0.0000001);
	}

	@Test
	public void testGetClosestScale()
	{
		double scale = _tileCache.getClosestScale(new Envelope(4.949, 5.4755,
				46.5204, 46.7463), new Dimension(1500, 650));

		assertEquals(100000, scale, 0.0000001);
	}

	@Test
	public void testCalculateBoundsCenteredSmallScale()
			throws MismatchedDimensionException, TransformException, FactoryException
	{
		asserCorrectCalculateBounds(new Dimension(1500, 650), TileCache.DEFAULT_WGS84[5], new Coordinate(
				0, 0));
	}

	@Test
	public void testCalculateBoundsCloseToPoleLargeScale()
			throws MismatchedDimensionException, TransformException, FactoryException
	{
		asserCorrectCalculateBounds(new Dimension(1500, 650), TileCache.DEFAULT_WGS84[15],
				new Coordinate(0, 89.99999));
	}

	@Test
	public void testCalculateBoundsCloseToDatelineLargeScale()
			throws MismatchedDimensionException, TransformException, FactoryException
	{
		asserCorrectCalculateBounds(new Dimension(1500, 650), TileCache.DEFAULT_WGS84[15],
				new Coordinate(-180, 0));
	}

	@Test
	public void testCalculateBoundsCenteredLargeScale()
			throws MismatchedDimensionException, TransformException, FactoryException
	{
		asserCorrectCalculateBounds(new Dimension(1, 1), TileCache.DEFAULT_WGS84[15],
				new Coordinate(0, 0));
		
		
	}

	/**
	 * Check that calculate bounds give expected value
	 */
	protected void asserCorrectCalculateBounds(Dimension dimension,
			int desiredScale, Coordinate center) throws TransformException,
			FactoryException
	{
		Envelope bounds = _tileCache.calculateBounds(dimension, desiredScale,
				center);
		double scale = _tileCache.calculateScale(bounds, dimension);
		assertEquals(desiredScale, scale, 1);
		
		double ratio = ((double)dimension.width) / dimension.height;
		
		assertEquals(ratio, bounds.getWidth() / bounds.getHeight(), 0.000001);
	}

}
