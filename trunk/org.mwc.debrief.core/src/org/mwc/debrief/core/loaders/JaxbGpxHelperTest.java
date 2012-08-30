package org.mwc.debrief.core.loaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import Debrief.Wrappers.FixWrapper;
import Debrief.Wrappers.TrackWrapper;
import Debrief.Wrappers.Track.TrackSegment;
import Debrief.Wrappers.Track.TrackWrapper_Support.SegmentList;
import MWC.GUI.Layers;
import MWC.GUI.Properties.LocationPropertyEditor;
import MWC.GenericData.WorldLocation;

/**
 * @author Aravind R. Yarram <yaravind@gmail.com>
 * @date August 21, 2012
 * @category gpx
 * 
 */
public class JaxbGpxHelperTest
{
	private final JaxbGpxHelper helper = new JaxbGpxHelper();

	@Test
	public void unmarshallTrackWithAllData()
	{
		Layers layers = helper.unmarshall(getClass().getResourceAsStream("gpx-data.xml"));
		assertEquals("Only 1 track is present in the gpx xml", 1, layers.size());

		// assert track
		TrackWrapper track = (TrackWrapper) layers.findLayer("NELSON");
		assertNotNull(track);
		assertEquals("NELSON", track.getName());
		assertFalse(track.getNameAtStart());
		assertEquals(2, track.getLineThickness());
		assertFalse(track.getInterpolatePoints());
		assertFalse(track.getLinkPositions());
		assertEquals(2, track.getLineStyle());
		assertEquals(Integer.valueOf(LocationPropertyEditor.CENTRE), track.getNameLocation());
		assertFalse(track.getNameVisible());
		assertFalse(track.getPlotArrayCentre());
		assertFalse(track.getLinkPositions());
		assertFalse(track.getSensors().getVisible());
		assertFalse(track.getSolutions().getVisible());
		assertFalse(track.getPositionsVisible());
		assertEquals("Square", track.getSymbolType());
		assertFalse(track.getVisible());

		// assert segment
		SegmentList segments = track.getSegments();
		assertNotNull(segments);
		assertEquals("Only 1 segment is present in the gpx xml", 1, segments.size());

		TrackSegment segment = (TrackSegment) segments.getData().iterator().next();

		// assert Fix
		WorldLocation trackStart = segment.getTrackStart();
		assertNotNull("Since there is only one track it should be the start of the track ", trackStart);

		assertEquals(22.1862861, trackStart.getLat(), 0.0000001);
		assertEquals(-21.6978806, trackStart.getLong(), 0.0000001);
		assertEquals(0.000, trackStart.getDepth(), 0.0000001);
		assertTrue("1 track point is present in the gpx xml", segment.elements().hasMoreElements());

		FixWrapper fix = (FixWrapper) segment.elements().nextElement();
		assertNotNull(fix.getTime());
	}

	/**
	 * Unmarshaller should use the default values specified in the schema
	 */
	@Test
	public void unmarshallShouldNotFailWhenOptionalDataMissing()
	{
		Layers layers = helper.unmarshall(getClass().getResourceAsStream("gpx-missing-optional-data.xml"));
		assertEquals("Only 1 track is present in the gpx xml", 1, layers.size());

		// assert track
		TrackWrapper track = (TrackWrapper) layers.findLayer("NELSON");
		assertNotNull(track);
		assertEquals("NELSON", track.getName());
		assertTrue(track.getNameAtStart());
		assertEquals(1, track.getLineThickness());
		assertTrue(track.getInterpolatePoints());
		assertTrue(track.getLinkPositions());
		assertEquals(1, track.getLineStyle());
		assertEquals(Integer.valueOf(LocationPropertyEditor.RIGHT), track.getNameLocation());
		assertTrue(track.getNameVisible());
		assertTrue(track.getPlotArrayCentre());
		assertTrue(track.getLinkPositions());
		assertTrue(track.getSensors().getVisible());
		assertTrue(track.getSolutions().getVisible());
		assertTrue(track.getPositionsVisible());
		assertEquals("Square", track.getSymbolType());
		assertTrue(track.getVisible());

		// assert segment
		SegmentList segments = track.getSegments();
		assertNotNull(segments);
		assertEquals("Only 1 segment is present in the gpx xml", 1, segments.size());

		TrackSegment segment = (TrackSegment) segments.getData().iterator().next();

		// assert Fix
		WorldLocation trackStart = segment.getTrackStart();
		assertNotNull("Since there is only one track it should be the start of the track ", trackStart);

		assertEquals(22.1862861, trackStart.getLat(), 0.0000001);
		assertEquals(-21.6978806, trackStart.getLong(), 0.0000001);
		assertEquals(0.000, trackStart.getDepth(), 0.0000001);
		assertTrue("1 track point is present in the gpx xml", segment.elements().hasMoreElements());

		FixWrapper fix = (FixWrapper) segment.elements().nextElement();
		assertNotNull(fix.getTime());
	}
}
