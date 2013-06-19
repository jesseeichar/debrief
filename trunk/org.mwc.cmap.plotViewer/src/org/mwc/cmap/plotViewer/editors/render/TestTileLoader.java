/**
 * 
 */
package org.mwc.cmap.plotViewer.editors.render;

import java.awt.Dimension;

import org.eclipse.swt.graphics.Image;
import org.geotools.geometry.jts.ReferencedEnvelope;

import com.vividsolutions.jts.util.Assert;

/**
 * Just for loading.
 */
public class TestTileLoader implements TileLoader
{

	Image image;
	
	@Override
	public Image load(Dimension tileSize, ReferencedEnvelope envelope)
	{
		Assert.isTrue(image != null, "first call set image");
		return image;
	}

	public void setImage(Image image)
	{
		this.image = image;
	}
}
