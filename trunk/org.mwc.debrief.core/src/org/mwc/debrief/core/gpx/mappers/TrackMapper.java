package org.mwc.debrief.core.gpx.mappers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.Status;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.debrief.core.gpx.TrackExtensionType;
import org.mwc.debrief.core.loaders.DebriefJaxbContextAware;
import org.w3c.dom.Node;

import Debrief.Wrappers.FixWrapper;
import Debrief.Wrappers.TrackWrapper;
import Debrief.Wrappers.Track.TrackSegment;
import MWC.GUI.Editable;
import MWC.GUI.Properties.LocationPropertyEditor;

import com.topografix.gpx.v10.Gpx;
import com.topografix.gpx.v10.Gpx.Trk;
import com.topografix.gpx.v10.Gpx.Trk.Trkseg;
import com.topografix.gpx.v10.ObjectFactory;
import com.topografix.gpx.v11.ExtensionsType;
import com.topografix.gpx.v11.GpxType;
import com.topografix.gpx.v11.TrkType;
import com.topografix.gpx.v11.TrksegType;
import com.topografix.gpx.v11.WptType;

/**
 * @author Aravind R. Yarram <yaravind@gmail.com>
 * @date August 21, 2012
 * @category gpx
 * 
 *           <pre>
 * /plot/session/layers/track										/gpx/trk								Debrief.Wrappers.TrackWrapper
 * /plot/session/layers/track/TrackSegment 			/gpx/trk/trkseg 				Debrief.Wrappers.Track.TrackSegment	
 * /plot/session/layers/track/TrackSegment/fix 	/gpx/trk/trkseg/trkpt 	Debrief.Wrappers.FixWrapper
 * </pre>
 */
public class TrackMapper implements DebriefJaxbContextAware
{
	private final TrackSegmentMapper segmentMapper = new TrackSegmentMapper();
	private final FixMapper fixMapper = new FixMapper();
	private JAXBContext debriefContext;
	private static final ObjectFactory GPX_1_0_OBJ_FACTORY = new ObjectFactory();

	/**
	 * @category gpx11
	 */
	public List<TrackWrapper> fromGpx(GpxType gpx)
	{
		List<TrackWrapper> tracks = new ArrayList<TrackWrapper>(gpx.getTrk().size());

		for (TrkType gpxTrack : gpx.getTrk())
		{
			TrackWrapper track = new TrackWrapper();

			mapGpxTrack(gpxTrack, track);

			for (TrksegType gpxSegment : gpxTrack.getTrkseg())
			{
				TrackSegment segment = segmentMapper.fromGpx(gpxSegment);
				track.add(segment);

				// keep track of the previous fix, in case we wish to calculate course
				// and speed
				FixWrapper previousFix = null;

				for (WptType waypointType : gpxSegment.getTrkpt())
				{
					fixMapper.setJaxbContext(debriefContext);
					FixWrapper fix = fixMapper.fromGpx(waypointType, previousFix);
					segment.add(fix);

					previousFix = fix;
				}
			}
			tracks.add(track);
		}
		return tracks;
	}

	/**
	 * @category gpx10
	 */
	public List<TrackWrapper> fromGpx10(Gpx gpx)
	{
		List<TrackWrapper> tracks = new ArrayList<TrackWrapper>(gpx.getTrk().size());

		for (Gpx.Trk gpxTrack : gpx.getTrk())
		{
			TrackWrapper track = new TrackWrapper();

			mapGpx10Track(gpxTrack, track);

			for (Gpx.Trk.Trkseg gpxSegment : gpxTrack.getTrkseg())
			{
				TrackSegment segment = segmentMapper.fromGpx10(gpxSegment);
				track.add(segment);

				// keep track of the previous fix, in case we wish to calculate course
				// and speed
				FixWrapper previousFix = null;

				for (Gpx.Trk.Trkseg.Trkpt waypointType : gpxSegment.getTrkpt())
				{
					fixMapper.setJaxbContext(debriefContext);
					FixWrapper fix = fixMapper.fromGpx10(waypointType, previousFix);
					segment.add(fix);

					previousFix = fix;
				}
			}
			tracks.add(track);
		}
		return tracks;
	}

	/**
	 * @category gpx11
	 */
	private void mapGpxTrack(TrkType gpxTrack, TrackWrapper track)
	{
		track.setName(gpxTrack.getName());

		try
		{
			ExtensionsType extensions = gpxTrack.getExtensions();
			if (extensions != null)
			{
				List<Object> any = extensions.getAny();

				Unmarshaller unmarshaller = debriefContext.createUnmarshaller();
				Object object = unmarshaller.unmarshal((Node) any.get(0));
				TrackExtensionType trackExtension = (TrackExtensionType) JAXBIntrospector.getValue(object);

				track.setNameAtStart(trackExtension.isNameAtStart());
				track.setLineThickness(trackExtension.getLineThickness().intValue());
				track.setInterpolatePoints(trackExtension.isInterpolatePoints());
				track.setLinkPositions(trackExtension.isLinkPositions());
				track.setLineStyle(trackExtension.getLineStyle().intValue());
				LocationPropertyEditor nameLocationConverter = new LocationPropertyEditor();
				nameLocationConverter.setAsText(trackExtension.getNameLocation());
				track.setNameLocation(((Integer) nameLocationConverter.getValue()).intValue());
				track.getSensors().setVisible(trackExtension.isSensorsVisible());
				track.getSolutions().setVisible(trackExtension.isSolutionsVisible());
				track.setNameVisible(trackExtension.isNameVisible());
				track.setPlotArrayCentre(trackExtension.isPlotArrayCentre());
				track.setPositionsVisible(trackExtension.isPositionsVisible());
				track.setLinkPositions(trackExtension.isLinkPositions());
				track.setVisible(trackExtension.isVisible());
				track.setSymbolType(trackExtension.getSymbol());
			}
		}
		catch (JAXBException e)
		{
			CorePlugin.logError(Status.ERROR, "Error while mapping Track from GPX", e);
		}
	}

	/**
	 * @category gpx10
	 */
	private void mapGpx10Track(Trk gpxTrack, TrackWrapper track)
	{
		track.setName(gpxTrack.getName());
		// Ignore handling of debrief extensions as they are not required for now
	}

	public List<Trk> toGpx10(List<TrackWrapper> tracks)
	{
		List<Trk> gpxTracks = new ArrayList<Trk>(tracks.size());
		for (TrackWrapper track : tracks)
		{
			Trk gpxTrack = GPX_1_0_OBJ_FACTORY.createGpxTrk();
			gpxTrack.setName(track.getName());

			Enumeration<Editable> segs = track.getSegments().elements();
			while (segs.hasMoreElements())
			{
				Editable nextElement = segs.nextElement();

				if (nextElement instanceof TrackSegment)
				{
					exportSegment(gpxTrack, nextElement);
				}
				else
				{
					CorePlugin.logError(Status.INFO, "Ignoring " + nextElement + " while marshalling Track GPX as it is not a Fix", null);
				}
			}
			gpxTracks.add(gpxTrack);
		}
		return gpxTracks;
	}

	/**
	 * @category gpx10
	 */
	private void exportSegment(Trk gpxTrack, Editable nextElement)
	{
		TrackSegment seg = (TrackSegment) nextElement;
		Trkseg gpxSeg = GPX_1_0_OBJ_FACTORY.createGpxTrkTrkseg();
		gpxTrack.getTrkseg().add(gpxSeg);
		exportFixes(seg, gpxSeg);
	}

	/**
	 * @category gpx10
	 */
	private void exportFixes(TrackSegment seg, Trkseg gpxSeg)
	{
		Collection<Editable> pts = seg.getData();
		for (Iterator<Editable> iterator = pts.iterator(); iterator.hasNext();)
		{
			FixWrapper fix = (FixWrapper) iterator.next();
			gpxSeg.getTrkpt().add(fixMapper.toGpx10(fix));
		}
	}

	@Override
	public void setJaxbContext(JAXBContext ctx)
	{
		debriefContext = ctx;
	}
}
