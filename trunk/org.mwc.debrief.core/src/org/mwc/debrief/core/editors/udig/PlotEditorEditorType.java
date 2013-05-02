package org.mwc.debrief.core.editors.udig;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;

import MWC.GUI.Editable.EditorType;

public class PlotEditorEditorType extends EditorType
{

	public PlotEditorEditorType(PlotEditor editor)
	{
		super(editor, editor.toString(), "");
	}

	public final PropertyDescriptor[] getPropertyDescriptors()
	{
		try
		{
			final PropertyDescriptor[] res =
			{ prop("BackgroundColor", "the background color"),
					prop("LineThickness", "the line thickness")
					};

			return res;

		}
		catch (IntrospectionException e)
		{
			return super.getPropertyDescriptors();
		}
	}
}
