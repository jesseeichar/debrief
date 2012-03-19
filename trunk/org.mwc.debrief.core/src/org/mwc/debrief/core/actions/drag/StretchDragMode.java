package org.mwc.debrief.core.actions.drag;

import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.widgets.Display;
import org.mwc.debrief.core.DebriefPlugin;

import Debrief.Wrappers.FixWrapper;
import Debrief.Wrappers.TrackWrapper;
import Debrief.Wrappers.Track.CoreTMASegment;
import Debrief.Wrappers.Track.TrackSegment;
import MWC.GUI.Layers;
import MWC.GUI.Shapes.DraggableItem;
import MWC.GenericData.WorldLocation;
import MWC.GenericData.WorldVector;

public class StretchDragMode extends RotateDragMode
{

	
	public static class StretchOperation extends RotateOperation
	{
		private Double lastRange;

		public StretchOperation(WorldLocation cursorLoc, WorldLocation origin,
				CoreTMASegment segment, TrackWrapper parent, Layers theLayers)
		{
			super(cursorLoc, origin, segment, parent, theLayers);
		}

		public void shift(WorldVector vector)
		{
			CoreTMASegment seg = (CoreTMASegment) _segment;
			
			// find out where the cursor currently is
			workingLoc.addToMe(vector);
	
			// what's the bearing from the origin
			WorldVector thisVector = workingLoc.subtract(_origin);
	
			// work out the distance from the start
			double rng =  thisVector.getRange(); //- _originalDistDegs;
	
			// undo the previous turn
			if (lastRange != null)
			{
				seg.stretch(-lastRange, _origin);
			}
	
			// now do the current one
			seg.stretch(rng, _origin);
			
			// tell the segment it's shifted
			seg.clearBounds();
			
			// and remember it
			lastRange = new Double(rng);
			
			// and tell the props view to update itself
			updatePropsView(seg, _parent, _layers);

			
		}
	
		public Cursor getHotspotCursor()
		{
			if (_hotspotCursor == null)
				_hotspotCursor =  new Cursor(Display.getDefault(), DebriefPlugin
					.getImageDescriptor("icons/SelectFeatureHitStretch.ico").getImageData(), 4,
					2);	
			return _hotspotCursor;
		}
	}
	
	/**
	 * generate an operation for when the centre of the line segment is dragged
	 * 
	 * @param seg
	 *          the segment being dragged
	 * @return an operation we can use to do this
	 */
	protected DraggableItem getCentreOperation(final TrackSegment seg, TrackWrapper parent, Layers theLayers)
	{
		return new StretchFanOperation(seg, parent, theLayers);
	}


	public StretchDragMode()
	{
		super("Stretch", "Apply stretch operation to TMA solution");
	}

	@Override
	protected DraggableItem getEndOperation(WorldLocation cursorLoc,
			TrackSegment seg, FixWrapper subject, TrackWrapper parent, Layers theLayers)
	{
		return new StretchOperation(cursorLoc, subject.getFixLocation(), (CoreTMASegment) seg, parent, theLayers);
	}

	@Override
	protected boolean isAcceptable(TrackSegment seg)
	{
		return (seg instanceof CoreTMASegment);
	}		
	
	
}
