package org.mwc.debrief.core.interfaces;

import org.mwc.cmap.core.interfaces.IControllableViewport;

import java.awt.Color;

import org.eclipse.core.runtime.IAdaptable;

public interface IPlotEditor extends IAdaptable, IControllableViewport {

	void loadingComplete(Object complete);

	void setBackgroundColor(Color theColor);

	Color getBackgroundColor();

}
