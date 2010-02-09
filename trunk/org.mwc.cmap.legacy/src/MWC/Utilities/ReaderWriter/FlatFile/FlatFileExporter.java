package MWC.Utilities.ReaderWriter.FlatFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FlatFileExporter
{
	private String createTabs(int num)
	{
		StringBuffer res = new StringBuffer();
		for (int i = 0; i < num; i++)
		{
			res.append("\t");
		}
		return res.toString();
	}

	
	private final String BRK = "" + (char)13 + (char)10;

	public String exportThis()
	{
		String header = "STRAND Scenario Report 1.00"
				+ createTabs(33)
				+ BRK
				+ "MISSION_NAME"
				+ createTabs(33)
				+ BRK
				+ "Vessel"
				+ createTabs(33)
				+ BRK
				+ "OS track 0100-0330"
				+ createTabs(33)
				+ BRK
				+ "GapsFatBowBTH_5-4-04"
				+ createTabs(33)
				+ BRK
				+ "tla"
				+ createTabs(33)
				+ BRK
				+ "tla"
				+ createTabs(33)
				+ BRK
				+ "01:45:00	22/12/2002"
				+ createTabs(32)
				+ BRK
				+ "02:40:00	22/12/2002"
				+ createTabs(32)
				+ BRK
				+ "0"
				+ createTabs(33)
				+ BRK
				+ "0"
				+ createTabs(33)
				+ BRK
				+ "0"
				+ createTabs(33)
				+ BRK
				+ "5"
				+ createTabs(33)
				+ BRK
				+ "-1.23E+04	-654321"
				+ createTabs(32)
				+ BRK
				+ "Time	OS_Status	OS_X	OS_Y	OS_Speed	OS_Heading	Sensor_Status	Sensor_X	Sensor_Y	Sensor_Brg	Sensor_Bacc	Sensor_Freq	Sensor_Facc	Sensor_Speed	Sensor_Heading	Sensor_Type	Msd_Status	Msd_X	Msd_Y	Msd_Speed	Msd_Heading	Prd_Status	Prd_X	Prd_Y	Prd_Brg	Prd_Brg_Acc	Prd_Range	Prd_Range_Acc	Prd_Course	Prd_Cacc	Prd_Speed	Prd_Sacc	Prd_Freq	Prd_Freq_Acc" + BRK;
		;
		String body = "0	7	6.32332	-5555.55	2.7	200.1	0	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9	-999	6	-999.9	-999.9	1.1	11.12	0	-999.9	-999.9	-999.9	-999.9	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9"+ BRK
				+ "1	7	6.32332	-5555.551	2.7	200	0	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9	-999	6	-999.9	-999.9	1.1	11.12	0	-999.9	-999.9	-999.9	-999.9	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9"+ BRK
				+ "2	7	6.32332	-5555.55	2.7	200	0	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9	-999	6	-999.9	-999.9	1.1	11.12	0	-999.9	-999.9	-999.9	-999.9	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9"+ BRK
				+ "3	7	6.32332	-5521.2	4.6	200	0	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9	-999	6	-999.9	-999.9	1.1	11.12	0	-999.9	-999.9	-999.9	-999.9	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9"+ BRK
				+ "4	7	6.32332	-5555.32	4.7	200	0	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9	-999	6	-999.9	-999.9	1.1	11.12	0	-999.9	-999.9	-999.9	-999.9	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9"+ BRK
				+ "5	7	6.32332	-5543.73	4.8	200.1	0	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9	-999	6	-999.9	-999.9	1.1	11.12	0	-999.9	-999.9	-999.9	-999.9	-999	-999	-999.9	-999.9	-999.9	-999.9	-999.9	-999.9";

		return header + body;
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

		private String getTestData()
		{
			String res = null;
			try
			{
				res = readFileAsString("src/MWC/Utilities/ReaderWriter/FlatFile/fakedata.txt");
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
			assertEquals("has data", 2157, TARGET_STR.length());

			FlatFileExporter fa = new FlatFileExporter();
			String res = fa.exportThis();			
			assertEquals("correct string", TARGET_STR, res);

		}
	}

}
