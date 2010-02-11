package Debrief.ReaderWriter.FlatFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/** exporter class to replicate old Strand export format
 * 
 * @author ianmayo
 *
 */
public class FlatFileExporter
{
	/** header line
	 * 
	 */
	private static final String HEADER_LINE = "Time	OS_Status	OS_X	OS_Y	OS_Speed	OS_Heading	Sensor_Status	Sensor_X	Sensor_Y	Sensor_Brg	Sensor_Bacc	Sensor_Freq	Sensor_Facc	Sensor_Speed	Sensor_Heading	Sensor_Type	Msd_Status	Msd_X	Msd_Y	Msd_Speed	Msd_Heading	Prd_Status	Prd_X	Prd_Y	Prd_Brg	Prd_Brg_Acc	Prd_Range	Prd_Range_Acc	Prd_Course	Prd_Cacc	Prd_Speed	Prd_Sacc	Prd_Freq	Prd_Freq_Acc";

	/* line break
	 * 
	 */
	private final String BRK = "" + (char) 13 + (char) 10;

	/** convenience object to store tab
	 * 
	 */
	final String tab = "\t";



	/** append indicated number of tabs
	 * 
	 * @param num how many tabs to create
	 * @return the series of tabs
	 */
	private String createTabs(int num)
	{
		StringBuffer res = new StringBuffer();
		for (int i = 0; i < num; i++)
		{
			res.append("\t");
		}
		return res.toString();
	}
	
	/** extract a date from the supplied string, expecting date 
	 * in the following format:  HH:mm:ss	dd/MM/yyyy
	 * @param dateStr date to convert
	 * @return string as java date
	 * @throws ParseException if the string doesn't match
	 */
	public static Date dateFrom(String dateStr) throws ParseException
	{
		DateFormat df = new SimpleDateFormat("HH:mm:ss	dd/MM/yyyy");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date res = null;
		res = df.parse(dateStr);
		return res;
	}

	/** format this date in the prescribed format
	 * 
	 * @param val the date to format
	 * @return the formatted date
	 */
	static protected String formatThis(Date val)
	{
		DateFormat df = new SimpleDateFormat("HH:mm:ss	dd/MM/yyyy");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		return df.format(val);
	}

	public String testExport()
	{
		final String StartTime = "04:45:00	20/04/2009";
		final String endTime = "04:45:05	20/04/2009";
		String res = getHeader("Vessel", "OS track 0100-0330",
				"GapsFatBowBTH_5-4-04", "tla", StartTime, endTime, "5", "-1.23E+04",
				"-654321");
		res += getBody();
		return res;
	}

	/** provide a formatted header block using the supplied params
	 * 
	 * @param OWNSHIP
	 * @param OS_TRACK_NAME
	 * @param SENSOR_NAME
	 * @param TGT_NAME
	 * @param START_TIME
	 * @param END_TIME
	 * @param NUM_RECORDS
	 * @param X_ORIGIN_YDS
	 * @param Y_ORIGIN_YDS
	 * @return
	 */
	public String getHeader(final String OWNSHIP, String OS_TRACK_NAME,
			String SENSOR_NAME, String TGT_NAME, String START_TIME, String END_TIME,
			String NUM_RECORDS, String X_ORIGIN_YDS, String Y_ORIGIN_YDS)
	{

		String header = "STRAND Scenario Report 1.00" + createTabs(33) + BRK
				+ "MISSION_NAME" + createTabs(33) + BRK + OWNSHIP + createTabs(33)
				+ BRK + OS_TRACK_NAME + createTabs(33) + BRK + SENSOR_NAME
				+ createTabs(33) + BRK + TGT_NAME + createTabs(33) + BRK + TGT_NAME
				+ createTabs(33) + BRK + START_TIME + createTabs(32) + BRK + END_TIME
				+ createTabs(32) + BRK + "0" + createTabs(33) + BRK + "0"
				+ createTabs(33) + BRK + "0" + createTabs(33) + BRK + NUM_RECORDS
				+ createTabs(33) + BRK + X_ORIGIN_YDS + "	" + Y_ORIGIN_YDS + createTabs(32)
				+ BRK + HEADER_LINE + BRK;
		return header;
	}

	public String getBody()
	{
		String body = collateLine(0, 7, 6.32332, -5555.55, 2.7, 200.1, 0, -999,
				-999, -999.9, -999.9, -999.9, -999.9, -999.9, -999.9, "-999", 6,
				-999.9, -999.9, 1.1, 11.12, 0, -999.9, -999.9, -999.9, -999.9, -999,
				-999, -999.9, -999.9, -999.9, -999.9, -999.9, -999.9)
				+ BRK
				+ collateLine(1, 7, 6.32332, -5555.551, 2.7, 200, 0, -999, -999,
						-999.9, -999.9, -999.9, -999.9, -999.9, -999.9, "-999", 6, -999.9,
						-999.9, 1.1, 11.12, 0, -999.9, -999.9, -999.9, -999.9, -999, -999,
						-999.9, -999.9, -999.9, -999.9, -999.9, -999.9)
				+ BRK
				+ collateLine(2, 7, 6.32332, -5555.55, 2.7, 200, 0, -999, -999, -999.9,
						-999.9, -999.9, -999.9, -999.9, -999.9, "-999", 6, -999.9, -999.9,
						1.1, 11.12, 0, -999.9, -999.9, -999.9, -999.9, -999, -999, -999.9,
						-999.9, -999.9, -999.9, -999.9, -999.9)
				+ BRK
				+ collateLine(3, 7, 6.32332, -5521.2, 4.6, 200, 0, -999, -999, -999.9,
						-999.9, -999.9, -999.9, -999.9, -999.9, "-999", 6, -999.9, -999.9,
						1.1, 11.12, 0, -999.9, -999.9, -999.9, -999.9, -999, -999, -999.9,
						-999.9, -999.9, -999.9, -999.9, -999.9)
				+ BRK
				+ collateLine(4, 7, 6.32332, -5555.32, 4.7, 200, 0, -999, -999, -999.9,
						-999.9, -999.9, -999.9, -999.9, -999.9, "-999", 6, -999.9, -999.9,
						1.1, 11.12, 0, -999.9, -999.9, -999.9, -999.9, -999, -999, -999.9,
						-999.9, -999.9, -999.9, -999.9, -999.9)
				+ BRK
				+ collateLine(5, 7, 6.32332, -5543.73, 4.8, 200.1, 0, -999, -999,
						-999.9, -999.9, -999.9, -999.9, -999.9, -999.9, "-999", 6, -999.9,
						-999.9, 1.1, 11.12, 0, -999.9, -999.9, -999.9, -999.9, -999, -999,
						-999.9, -999.9, -999.9, -999.9, -999.9, -999.9);

		return body;
	}

	private String collateLine(int secs, int osStat, double osX_yds, double osY_yds,
			double spdKts, double headDegs, int sensorStat, int sensorX_yds, int sensorY_yds,
			double sensorBrg, double sensorBacc, double sensorFreq,
			double sensorFacc, double sensorSpdKts, double sensorHdg, String sensorType,
			int msdStat, double msdX_yds, double msdY_yds, double msdSpdKts, double msdHdg,
			int prdStat, double prdX_yds, double prdY_yds, double prdBrg, double prdBrgAcc,
			int prdRangeYds, int prdRangeAcc, double prdCourse, double prdCourseAcc,
			double prdSpdKts, double prdSpdAcc, double prdFreq, double prdFreqAcc)
	{
		String res = null;
		res = secs + tab + osStat + tab + osX_yds + tab + osY_yds + tab + spdKts + tab
				+ headDegs + tab + sensorStat + tab + sensorX_yds + tab + sensorY_yds + tab
				+ sensorBrg + tab + sensorBacc + tab + sensorFreq + tab + sensorFacc
				+ tab + sensorSpdKts + tab + sensorHdg + tab + sensorType + tab + msdStat
				+ tab + msdX_yds + tab + msdY_yds + tab + msdSpdKts + tab + msdHdg + tab + prdStat
				+ tab + prdX_yds + tab + prdY_yds + tab + prdBrg + tab + prdBrgAcc + tab
				+ prdRangeYds + tab + prdRangeAcc + tab + prdCourse + tab + prdCourseAcc
				+ tab + prdSpdKts + tab + prdSpdAcc + tab + prdFreq + tab + prdFreqAcc;

		return res;
	}

	static public final class testMe extends junit.framework.TestCase
	{

		static public final String TEST_ALL_TEST_TYPE = "UNIT";

		public testMe(final String val)
		{
			super(val);
		}

		private String readFileAsString(String filePath) throws java.io.IOException
		{
			StringBuffer fileData = new StringBuffer(1000);
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[1024];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1)
			{
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
			return fileData.toString();
		}

		private void dumpToFile(String str, String filename)
		{
			File outFile = new File(filename);
			FileWriter out;
			try
			{
				out = new FileWriter(outFile);
				out.write(str);
				out.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		private String getTestData()
		{
			String res = null;
			try
			{
				res = readFileAsString("src/Debrief/ReaderWriter/FlatFile/fakedata.txt");
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			return res;
		}

		public void testExport() throws IOException
		{
			final String TARGET_STR = getTestData();
			assertNotNull("test data found", TARGET_STR);
			// assertEquals("has data", 2157, TARGET_STR.length());

			FlatFileExporter fa = new FlatFileExporter();
			String res = fa.testExport();
			assertEquals("correct string", TARGET_STR, res);

			dumpToFile(res, "src/Debrief/ReaderWriter/FlatFile/data_out.txt");

		}

		public void testDateFormat() throws ParseException
		{
			Date theDate = dateFrom("01:45:00	22/12/2002");

			// Date theDate = new Date(2002,12,22,1,45,00);
			String val = formatThis(theDate);
			assertEquals("correct start date", "01:45:00	22/12/2002", val);
		}
	}

}
