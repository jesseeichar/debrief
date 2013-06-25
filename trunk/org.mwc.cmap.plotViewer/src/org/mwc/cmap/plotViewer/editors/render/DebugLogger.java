package org.mwc.cmap.plotViewer.editors.render;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DebugLogger
{
	private static BufferedWriter writer;

	static {
		try
		{
			writer = new BufferedWriter(new FileWriter("renderlog.log"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void log(String msg)
	{
		return;
//		try
//		{
//			writer.append(msg);
//			writer.append('\n');
//			writer.flush();
//		}
//		catch (IOException e)
//		{
//			e.printStackTrace();
//		}
	}

	public static void close()
	{
		try
		{
			writer.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
