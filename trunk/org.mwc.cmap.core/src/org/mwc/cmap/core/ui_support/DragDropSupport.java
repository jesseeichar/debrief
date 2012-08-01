package org.mwc.cmap.core.ui_support;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.DropTargetListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.TreeItem;
import org.mwc.cmap.core.CorePlugin;
import org.mwc.cmap.core.operations.RightClickCutCopyAdaptor;
import org.mwc.cmap.core.operations.RightClickCutCopyAdaptor.EditableTransfer;
import org.mwc.cmap.core.property_support.EditableWrapper;

import Debrief.Wrappers.FixWrapper;
import Debrief.Wrappers.NarrativeWrapper;
import Debrief.Wrappers.TacticalDataWrapper;
import Debrief.Wrappers.TrackWrapper;
import MWC.GUI.BaseLayer;
import MWC.GUI.Editable;
import MWC.GUI.Layer;
import MWC.GUI.Layers;
import MWC.GUI.Chart.Painters.ETOPOPainter;
import MWC.GUI.VPF.VPFDatabase;
import MWC.TacticalData.NarrativeEntry;

/**
 * nice drag-drop support for layer manager
 * 
 * @author ian.mayo
 */
public class DragDropSupport implements DragSourceListener, DropTargetListener
{

	/**
	 * the control that's providing us with our selection
	 */
	private StructuredViewer _parent;

	/**
	 * it appears that the copy/move operations gets cancelled after we mark
	 * something as "don't drop". remember the previous setting, so that when we
	 * want to indicate that something is a valid drop-target, it can be dropped.
	 * that's all/
	 */
	private int _oldDetail = -1;

	/**
	 * a list of helper classes - that allow more items to be dropped onto us.
	 */
	private static Vector<XMLFileDropHandler> _myDropHelpers;

	/**
	 * constructor - something that tells us about the current selection
	 * 
	 * @param parent
	 */
	public DragDropSupport(StructuredViewer parent)
	{
		_parent = parent;
	}

	/**
	 * provide another class to assist with loaded dropped files
	 * 
	 * @param handler
	 */
	public static void addDropHelper(XMLFileDropHandler handler)
	{
		if (_myDropHelpers == null)
		{
			_myDropHelpers = new Vector<XMLFileDropHandler>(1, 1);
		}
		_myDropHelpers.add(handler);
	}

	public void dragFinished(DragSourceEvent event)
	{
	}

	public void dragSetData(DragSourceEvent event)
	{
		StructuredSelection sel = getSelection();
		event.data = sel;
	}

	public void dragStart(DragSourceEvent event)
	{
		// ok, clear the old detail flag
		_oldDetail = -1;

		boolean res = true;

		// get what's selected
		StructuredSelection sel = getSelection();
		EditableWrapper first = (EditableWrapper) sel.getFirstElement();
		Editable pl = first.getEditable();

		// so, is this draggable?
		if (pl instanceof BaseLayer)
			res = true;
		else if (pl instanceof ETOPOPainter)
			res = false;
		else if (pl instanceof TrackWrapper)
			res = false;
		else if (pl instanceof FixWrapper)
			res = false;
		else if (pl instanceof NarrativeWrapper)
			res = false;
		else if (pl instanceof NarrativeEntry)
			res = false;
		else if (pl instanceof TacticalDataWrapper)
			res = false;

		event.doit = res;
	}

	/**
	 * find out what's currently selected
	 * 
	 * @return
	 */
	private StructuredSelection getSelection()
	{
		// ok, get the selection
		StructuredSelection sel = (StructuredSelection) _parent.getSelection();
		return sel;
	}

	public void dragEnter(DropTargetEvent event)
	{
	}

	public void dragLeave(DropTargetEvent event)
	{
	}

	public void dragOperationChanged(DropTargetEvent event)
	{
	}

	public void dragOver(DropTargetEvent event)
	{
		boolean allowDrop = false;

		// hmm, what type of data are we receiving, is it a file?
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType))
		{
			// do we have any loaders?
			if (_myDropHelpers != null)
			{
				TreeItem ti = (TreeItem) event.item;

				// check the tree isn't empty
				if (ti != null)
				{
					EditableWrapper ew = (EditableWrapper) ti.getData();
					for (Iterator<XMLFileDropHandler> iter = _myDropHelpers.iterator(); iter
							.hasNext();)
					{
						XMLFileDropHandler handler = (XMLFileDropHandler) iter.next();

						// right, does it handle this kind of element?
						if (handler.canBeDroppedOn(ew.getEditable()))
						{
							// yup, can it drop on our target?
							Object tgt = event.item.getData();
							if (tgt instanceof EditableWrapper)
							{
								allowDrop = true;
								break;
							}
						}
					}
				}
			}
		}
		else
		{
			// right, we're dragging something off the layer manager itself. have a
			// look at it.

			// hmm, do we want to accept this?
			TreeItem ti = (TreeItem) event.item;
			// right, do we have a target?
			if (ti != null)
			{
				EditableWrapper pw = (EditableWrapper) ti.getData();
				Editable pl = pw.getEditable();

				if (pl instanceof ETOPOPainter)
				{
					allowDrop = false;
				}
				if (pl instanceof VPFDatabase)
				{
					// no, we won't let them drop something into a VPF database
					allowDrop = false;
				}
				else if (pl instanceof BaseLayer)
				{
					// start off with reasonably sensible default
					allowDrop = true;

					// hmm, just double-check that we're not dragging a layer over this
					StructuredSelection sel = getSelection();

					// cycle through the elements
					for (@SuppressWarnings("rawtypes")
					Iterator iter = sel.iterator(); iter.hasNext();)
					{
						EditableWrapper thisP = (EditableWrapper) iter.next();
						Editable dragee = thisP.getEditable();
						if (dragee instanceof BaseLayer)
						{
							allowDrop = false;
							continue;
						}
					}

				}
				else
				{
					allowDrop = false;
				}

			}
		}

		if (allowDrop)
		{
			// restore what we were looking at...
			if (event.detail == DND.DROP_NONE)
			{
				event.detail = _oldDetail;
			}

			// ok - and the update status of the component under the cursor
			event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL;
		}
		else
		{
			if (event.detail != DND.DROP_NONE)
			{
				_oldDetail = event.detail;
			}

			event.feedback = DND.FEEDBACK_NONE;
			event.detail = DND.DROP_NONE;
		}
	}

	abstract public static class XMLFileDropHandler
	{
		/**
		 * the type of elements we're interested in
		 */
		private final String[] _elements;

		/**
		 * the types of object we can drop onto
		 */
		@SuppressWarnings(
		{ "rawtypes" })
		private final Class[] targets;

		/**
		 * constructor
		 * 
		 * @param elementTypes
		 *          the top-level XML elements we can process
		 * @param targetTypes
		 *          the types of thing we drop onto
		 */
		@SuppressWarnings("rawtypes")
		public XMLFileDropHandler(String[] elementTypes, Class[] targetTypes)
		{
			_elements = elementTypes;
			targets = targetTypes;
		}

		public boolean handlesThis(String firstElement)
		{
			boolean res = false;
			for (int i = 0; i < _elements.length; i++)
			{
				String thisE = _elements[i];
				if (thisE.toUpperCase().equals(firstElement.toUpperCase()))
				{
					res = true;
					break;
				}
			}
			return res;
		}

		@SuppressWarnings("rawtypes")
		public boolean canBeDroppedOn(Editable targetElement)
		{
			boolean res = false;
			for (int i = 0; i < targets.length; i++)
			{
				Class thisE = targets[i];
				if (targetElement.getClass() == thisE)
				{
					res = true;
					break;
				}
			}
			return res;
		}

		/**
		 * ok, load the item, and add it to the indicated layer
		 * 
		 * @param source
		 * @param targetElemet
		 * @param parent
		 *          TODO
		 */
		abstract public void handleDrop(InputStream source, Editable targetElemet,
				Layers parent);
	}

	@SuppressWarnings("rawtypes")
	public void drop(DropTargetEvent event)
	{
		// hmm, what type of data are we receiving, is it a file?
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType))
		{
			String[] names = (String[]) event.data;
			String fileName = names[0];
			File theFile = new File(fileName);
			if (theFile.exists())
			{
				// right, is it our correct type?

				// is it an xml file
				int fileSep = fileName.lastIndexOf('.');
				String suffix = fileName.substring(fileSep + 1);
				String uSuffix = suffix.toUpperCase();
				if (uSuffix.equals("XML"))
				{
					// hey, could be. Extract the first 100 characters
					try
					{
						FileReader fr = new FileReader(theFile);
						BufferedReader re = new BufferedReader(fr);
						String firstLine = re.readLine();

						// get some more data
						boolean inComplete = true;
						while ((firstLine.length() < 200) && (inComplete))
						{
							String newLine = re.readLine();
							if (newLine == null)
								inComplete = false;

							firstLine += newLine;
						}
						
						re.close();
						re = null;

						// our text (called firstLine) should have around 200 chars in it
						// now.

						// does it have an xml declaration
						int index = firstLine.indexOf("<?");

						// hey, either the number is looking at the first occurence
						// of the declaration, or it's looking at minus one. switch
						// minus one to zero, so we can get started
						if (index == -1)
							index = 0;
						else
							index = Math.max(index, 5);

						// now find the next xml marker
						index = firstLine.indexOf('<', index);

						// now, find the end of this XML item
						int endOfElement = firstLine.indexOf(" ", index);
						String thisElement = firstLine.substring(index + 1, endOfElement);

						// do we have any loaders?
						if (_myDropHelpers != null)
						{
							for (Iterator<XMLFileDropHandler> iter = _myDropHelpers
									.iterator(); iter.hasNext();)
							{
								XMLFileDropHandler handler = (XMLFileDropHandler) iter.next();

								// right, does it handle this kind of element?
								if (handler.handlesThis(thisElement))
								{
									// yup, can it drop on our target?
									Object tgt = event.item.getData();
									if (tgt instanceof EditableWrapper)
									{
										EditableWrapper ew = (EditableWrapper) tgt;
										if (handler.canBeDroppedOn(ew.getEditable()))
										{
											// yes, go for it!
											handler.handleDrop(new FileInputStream(theFile),
													ew.getEditable(), ew.getLayers());
											break;
										}
									}
								}
							}
						}

					}
					catch (FileNotFoundException e)
					{
						CorePlugin.logError(Status.ERROR, "File not found for drag/drop:"
								+ fileName, e);
					}
					catch (IOException e)
					{
						CorePlugin.logError(Status.ERROR, "IOException handling drag/drop:"
								+ fileName, e);
					}
				}
				else
				{
					// no chance
				}

				// hmm, does it start off with <SC
			}
		}
		else
		{

			StructuredSelection sel = getSelection();

			// cycle through the elements
			for (Iterator iter = sel.iterator(); iter.hasNext();)
			{
				EditableWrapper thisP = (EditableWrapper) iter.next();
				Editable dragee = thisP.getEditable();

				// right, are we cutting?
				if ((_oldDetail & DND.DROP_MOVE) != 0)
				{
					// remove from current parent
					EditableWrapper parent = thisP.getParent();

					// is this a top-level item?
					if (parent == null)
					{
						Layers layers = thisP.getLayers();
						layers.removeThisLayer((Layer) dragee);
					}
					else
					{
						BaseLayer parentLayer = (BaseLayer) parent.getEditable();
						parentLayer.removeElement(dragee);
					}
				}

				// add to new parent
				TreeItem ti = (TreeItem) event.item;
				EditableWrapper destination = (EditableWrapper) ti.getData();

				// ok, we need to add a new instance of the dragee (so we can support
				// multiple instances)
				Editable newDragee = (Editable) RightClickCutCopyAdaptor
						.cloneThis(dragee);

				// also add it to the plottable layer target
				BaseLayer dest = (BaseLayer) destination.getEditable();
				dest.add(newDragee);
			}
		}
		// fire update
		Layers destL = (Layers) _parent.getInput();
		destL.fireExtended();
	}

	public void dropAccept(DropTargetEvent event)
	{
		// right, is htis
	}

	public Transfer[] getTypes()
	{
		Transfer[] res = new Transfer[]
		{ EditableTransfer.getInstance(), FileTransfer.getInstance() };
		return res;
	}

}
