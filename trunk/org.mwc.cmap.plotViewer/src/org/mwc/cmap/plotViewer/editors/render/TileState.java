package org.mwc.cmap.plotViewer.editors.render;

/**
 * The current state of the tile.
 * 
 * @author Jesse
 */
public enum TileState
{
	/**
	 * The tile has been correctly loaded and is ready to be drawn.
	 */
	READY,
	/**
	 * The tile is still being loaded, likely by another thread.
	 */
	LOADING,
	/**
	 * The last attempt at loading the tile resulted in an error.
	 */
	ERROR,
	/**
	 * The tile have not yet been loaded.
	 */
	BLANK,
	/**
	 * The tile has been disposed.
	 */
	DISPOSED
}