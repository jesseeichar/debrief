package org.mwc.cmap.core.ui_support.udig;

import org.eclipse.swt.widgets.Control;

import MWC.GUI.CanvasType;

public interface ControlCanvasType extends CanvasType
{
	/**
	 * Get an swt control.
	 */
	public Control getControl();
}
