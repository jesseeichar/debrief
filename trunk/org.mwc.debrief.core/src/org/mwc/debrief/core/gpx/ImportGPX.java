package org.mwc.debrief.core.gpx;

import java.awt.Color;
import java.io.InputStream;

import org.mwc.debrief.core.loaders.GpxHelper;
import org.mwc.debrief.core.loaders.JaxbGpxHelper;

import Debrief.ReaderWriter.Replay.ImportReplay;
import Debrief.Wrappers.TrackWrapper;
import MWC.GUI.Layers;

/**
 * importer for GPS files
 * 
 * @author ian
 * 
 */
public class ImportGPX
{

	// cache the last layer - for speed
	private static TrackWrapper lastLayer = null;

	/**
	 * keep track of how many tracks we've created, so we can generate unique
	 * colors
	 */
	private static int colorCounter = 0;

	/**
	 * the time-stamp presumed for LineString data that may not contain time data
	 * 
	 */
	// private static int DEFAULT_TIME_STEP = 1000;

	/**
	 * create a track using the supplied name
	 * 
	 * @param target
	 * @param trackName
	 */
	private static void createTrack(Layers target, String trackName)
	{
		lastLayer = new TrackWrapper();
		lastLayer.setName(trackName);

		// sort out a color
		Color theCol = ImportReplay.replayColorFor(colorCounter++);
		lastLayer.setColor(theCol);

		target.addThisLayer(lastLayer);
	}

	public static void doImport(Layers theLayers, InputStream inputStream, String fileName)
	{
		GpxHelper helper = new JaxbGpxHelper();
		helper.unmarshall(inputStream, theLayers);

		// THE JAXB MAGIC GOES IN HERE!!!

		// try
		// {
		//
		// // get the main part of the file - we use it for the track name
		// String prefix = tidyFileName(fileName);
		//
		// // read the file into a string
		// String theStr = inputStreamAsString(inputStream);//
		// readFileAsString(thePath);
		//
		// // now ditch any non-compatible chars (such as the degree symbol)
		// String tidyStr = stripNonValidXMLCharacters(theStr);
		//
		// // wrap the string in a source
		// InputSource s = new InputSource(new StringReader(tidyStr));

		// // get the bits ready to do the document parsing
		// DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		// // then we have to create document-loader:
		// DocumentBuilder loader = factory.newDocumentBuilder();
		//
		// // get parsing
		// Document doc = loader.parse(s);
		//
		// // normalise the DOM - to make it a little more stable/predictable
		// doc.getDocumentElement().normalize();
		//
		// // get our XML date parser ready
		// final SimpleDateFormat parser = new SimpleDateFormat(
		// "yyyy-MM-d'T'HH:mm:ss'Z'");
		// parser.setTimeZone(TimeZone.getTimeZone("GMT"));
		//
		// // find the placemarks
		// NodeList nodeList = doc.getElementsByTagName("Placemark");
		//
		// // right, work through them
		// for (int i = 0; i < nodeList.getLength(); i++)
		// {
		// // ok - we have a placemark, see if it's got useful data
		// Node thisNode = nodeList.item(i);
		// Element thisP = (Element) thisNode;
		//
		// // look for the indicator for our child nodes
		// NodeList theGeom = thisP.getElementsByTagName("MultiGeometry");
		// if (theGeom.getLength() > 0)
		// {
		// // yup, it's one of ours.
		// // sort it out.
		// String theName = thisP.getElementsByTagName("name").item(0)
		// .getTextContent();
		// String[] nameTokens = theName.split("-");
		//
		// // get the first part of the track id
		// String trackIdTxt = nameTokens[0].trim();
		// int trackID = Integer.parseInt(trackIdTxt);
		//
		// // now for the time
		// String timeTxt = thisP.getElementsByTagName("when").item(0)
		// .getTextContent();
		// Date theD = parser.parse(timeTxt);
		//
		// // and the location
		// String coordsTxt = thisP.getElementsByTagName("coordinates").item(0)
		// .getTextContent();
		// // and now parse the string
		// String[] coords = coordsTxt.split(",");
		// double longVal = Double.parseDouble(coords[0]);
		// double latVal = Double.parseDouble(coords[1]);
		// double altitudeVal = Double.parseDouble(coords[2]);
		//
		// // lastly, the course/speed
		// double courseDegs;
		// double speedKts;
		// String descriptionTxt = thisP.getElementsByTagName("description")
		// .item(0).getTextContent();
		//
		// courseDegs = courseFrom(descriptionTxt);
		// speedKts = speedFrom(descriptionTxt);
		//
		// addFix(theLayers, prefix + "-" + trackID, new HiResDate(theD
		// .getTime()), new WorldLocation(latVal, longVal, -altitudeVal),
		// courseDegs, speedKts);
		// }
		// else
		// {
		// // see if it's from a GPS tracker
		// NodeList lineString = thisP.getElementsByTagName("LineString");
		// if (lineString != null)
		// {
		// // yup, suspect it's from a NokiaSportsTracker file
		// String trimmedFile = fileName.substring(1, fileName.length() - 1);
		// trimmedFile = trimmedFile.substring(0, trimmedFile.length() - 4);
		//
		// // get our XML date parser ready
		// final SimpleDateFormat nokiaDateFormat = new SimpleDateFormat(
		// "yyyyMMddHHmms");
		// nokiaDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		// Date theDate = new Date();
		//
		// try
		// {
		// theDate = nokiaDateFormat.parse(trimmedFile);
		// }
		// catch (Exception e)
		// {
		// e.printStackTrace();
		// }
		//
		// Element theString = (Element) lineString.item(0);
		// if (theString != null)
		// {
		// NodeList theCoords = theString
		// .getElementsByTagName("coordinates");
		// if (theCoords != null)
		// {
		// Node theCoordStr = theCoords.item(0);
		// String contents = theCoordStr.getTextContent();
		// parseTheseCoords(contents, theLayers, theDate);
		// }
		// }
		// }
		//
		// }
		// }
		//
		// }
		// catch (ParserConfigurationException e)
		// {
		// e.printStackTrace();
		// }
		// catch (SAXException e)
		// {
		// e.printStackTrace();
		// }
		// catch (IOException e)
		// {
		// e.printStackTrace();
		// }
		// catch (ParseException e)
		// {
		// e.printStackTrace();
		// }

		// lastly, clear the 'last layer' object
		lastLayer = null;

	}

	// /** utility to run through the contents of a LineString item - presuming
	// the presence of altitude data
	// *
	// * @param contents the inside of the linestring construct
	// * @param theLayers where we're going to stick the data
	// * @param startDate the start date for the track
	// */
	// private static void parseTheseCoords(String contents, Layers theLayers,
	// Date startDate)
	// {
	// StringTokenizer token = new StringTokenizer(contents, ",\n", false);
	//
	// Date newDate = new Date(startDate.getTime());
	//
	// while (token.hasMoreElements())
	// {
	// String longV = token.nextToken();
	// String latV = token.nextToken();
	// String altitude = token.nextToken();
	//
	// // just check that we have altitude data
	// double theAlt = 0;
	// if(altitude.length() > 0)
	// theAlt = Double.valueOf(altitude);
	//
	// addFix(theLayers, startDate.toString(), new HiResDate(newDate.getTime()),
	// new WorldLocation(Double.valueOf(latV), Double.valueOf(longV),
	// -theAlt), 0, 0);
	//
	// // add a second incremenet to the date, to create the new date
	// newDate = new Date(newDate.getTime() + DEFAULT_TIME_STEP);
	// }
	//
	// }
	//
	// /**
	// * extract the course element from the supplied string
	// *
	// * @param descriptionTxt
	// * @return
	// */
	// private static double courseFrom(String descriptionTxt)
	// {
	// double res = 0;
	// // STRING LOOKS LIKE
	// // <![CDATA[<b>RADAR PLOT 20:01:12 (GMT)</b><br><hr>Lat:
	// // 05.9696<br>Lon: 07.9633<br>Course: 253.0<br>Speed: 7.1
	// // knots<br>Date: September 12, 2009]]>
	// int startI = descriptionTxt.indexOf("Course");
	// int endI = descriptionTxt.indexOf("<br>Speed");
	// if ((startI > 0) && (endI > 0))
	// {
	// String subStr = descriptionTxt.substring(startI + 7, endI - 1);
	// res = Double.valueOf(subStr.trim());
	// }
	// return res;
	// }
	//
	// /**
	// * extract the course element from the supplied string
	// *
	// * @param descriptionTxt
	// * @return
	// */
	// private static double speedFrom(String descriptionTxt)
	// {
	// double res = 0;
	// // STRING LOOKS LIKE
	// // <![CDATA[<b>RADAR PLOT 20:01:12 (GMT)</b><br><hr>Lat:
	// // 05.9696<br>Lon: 07.9633<br>Course: 253.0<br>Speed: 7.1
	// // knots<br>Date: September 12, 2009]]>
	// int startI = descriptionTxt.indexOf("Speed");
	// int endI = descriptionTxt.indexOf("knots");
	// if ((startI > 0) && (endI > 0))
	// {
	// String subStr = descriptionTxt.substring(startI + 6, endI - 1);
	// res = Double.valueOf(subStr.trim());
	// }
	// return res;
	// }
	//
	// /**
	// * This method ensures that the output String has only valid XML unicode
	// * characters as specified by the XML 1.0 standard. For reference, please
	// see
	// * <a href="http://www.w3.org/TR/2000/REC-xml-20001006#NT-Char">the
	// * standard</a>. This method will return an empty String if the input is
	// null
	// * or empty.
	// *
	// * @param in
	// * The String whose non-valid characters we want to remove.
	// * @return The in String, stripped of non-valid characters.
	// */
	// private static String stripNonValidXMLCharacters(String in)
	// {
	// StringBuffer out = new StringBuffer(); // Used to hold the output.
	// char current; // Used to reference the current character.
	//
	// if (in == null || ("".equals(in)))
	// return ""; // vacancy test.
	// for (int i = 0; i < in.length(); i++)
	// {
	// current = in.charAt(i); // NOTE: No IndexOutOfBoundsException caught here;
	// // it should not happen.
	// if ((current == 0x9) || (current == 0xA) || (current == 0xD)
	// || ((current >= 0x20) && (current <= 0xD7FF))
	// || ((current >= 0xE000) && (current <= 0xFFFD))
	// || ((current >= 0x10000) && (current <= 0x10FFFF)))
	// out.append(current);
	// }
	// return out.toString();
	// }
	//
	// private static String inputStreamAsString(InputStream stream)
	// throws IOException
	// {
	// InputStreamReader isr = new InputStreamReader(stream);
	// BufferedReader br = new BufferedReader(isr);
	// StringBuilder sb = new StringBuilder();
	// String line = null;
	//
	// while ((line = br.readLine()) != null)
	// {
	// sb.append(line + "\n");
	// }
	//
	// br.close();
	// return sb.toString();
	// }
	//
	// /**
	// * Remove the suffix from the passed file name, together with any leading
	// * path.
	// *
	// * @param fileName
	// * File name to remove suffix from.
	// *
	// * @return <TT>fileName</TT> without a suffix.
	// *
	// * @throws IllegalArgumentException
	// * if <TT>null</TT> file name passed.
	// */
	// private static String tidyFileName(final String fileName)
	// {
	// if (fileName == null)
	// {
	// throw new IllegalArgumentException("file name == null");
	// }
	//
	// // start off by ditching the path
	// File holder = new File(fileName);
	// String res = holder.getName();
	//
	// // now ditch the file suffix
	// int pos = res.lastIndexOf('.');
	// if (pos > 0 && pos < res.length() - 1)
	// {
	// res = res.substring(0, pos);
	// }
	// return res;
	// }

}
