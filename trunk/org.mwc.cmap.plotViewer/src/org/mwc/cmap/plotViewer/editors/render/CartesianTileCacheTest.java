package org.mwc.cmap.plotViewer.editors.render;

import static org.junit.Assert.*;

import java.awt.Dimension;

import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.junit.Before;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.util.AssertionFailedException;

public class CartesianTileCacheTest
{
	private TileCache _tileCache;

	int[] scales = new int[]
	{ 1, 10, 100, 1000, 10000, 100000, 1000000 };

	@Before
	public void before()
	{

		_tileCache = new TileCache(new Dimension(10, 10), scales, new Coordinate(
				-100, -100), 100  / 2.54, DefaultEngineeringCRS.CARTESIAN_2D,
				new TestTileLoader());
	}

	@Test
	public void testGetClosestScaleExceptionalCases()
	{
		int scale = _tileCache.getClosestScale(new Envelope(-100, 100, -10, 10),
				new Dimension(10, 10));

		assertEquals(100000, scale);

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

		assertEquals(1, scale);
	}

	@Test
	public void testGetClosestScale()
	{
		int scale = _tileCache.getClosestScale(new Envelope(10, 10, 110, 110),
				new Dimension(100, 100));

		assertEquals(1, scale);
	}

	@Test
	public void testGetTiles()
	{
		Dimension screenSize = new Dimension(200, 200);
		Coordinate center = new Coordinate(100, 100);

		

		Tile[][] tiles = _tileCache.getTiles(screenSize, 1, center);

		assertEquals(20, tiles.length);
		for (Tile[] row : tiles)
		{
			assertEquals(20, row.length);
		}
		fail("not implemented");
	}

}
