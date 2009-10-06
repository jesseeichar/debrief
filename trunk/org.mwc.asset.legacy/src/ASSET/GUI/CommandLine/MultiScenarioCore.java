package ASSET.GUI.CommandLine;

import ASSET.Scenario.CoreScenario;
import ASSET.Scenario.LiveScenario.ISimulation;
import ASSET.Scenario.LiveScenario.ISimulationQue;
import ASSET.Scenario.Observers.CoreObserver;
import ASSET.Scenario.Observers.IntraScenarioObserverType;
import ASSET.Scenario.Observers.RecordToFileObserverType;
import ASSET.Scenario.Observers.ScenarioObserver;
import ASSET.Scenario.Observers.ScenarioStatusObserver;
import ASSET.Scenario.Observers.TimeObserver;
import ASSET.Util.MonteCarlo.ScenarioGenerator;
import ASSET.Util.SupportTesting;
import ASSET.Util.XML.ASSETReaderWriter;
import ASSET.Util.XML.ASSETReaderWriter.ResultsContainer;
import MWC.Algorithms.LiveData.IAttribute;

import org.w3c.dom.Document;

import java.io.*;
import java.util.Iterator;
import java.util.Vector;

/**
 * Created by IntelliJ IDEA. User: Ian.Mayo Date: 02-Jun-2003 Time: 15:05:23
 * Class providing multi scenario support to the command line class Log:
 */

public class MultiScenarioCore implements ISimulationQue
{
	/**
	 * success code to prove it ran ok
	 */
	static int SUCCESS = 0;

	/**
	 * error code to return when we've rx the wrong parameters
	 */
	static int WRONG_PARAMETERS = 1;

	/**
	 * error code to return when we can't load our data
	 */
	static int PROBLEM_LOADING = 2;

	/**
	 * error code to indicate we couldn't find one of the files
	 */
	static int FILE_NOT_FOUND = 3;

	/**
	 * error code to indicate we couldn't create the output files
	 */
	static int TROUBLE_MAKING_FILES = 4;

	/**
	 * the scenario generator that does all the work
	 */
	private ScenarioGenerator _myGenny;

	/**
	 * the set of scenarios we're going to run through
	 */
	protected Vector<Document> _myScenarioDocuments;

	private Vector<IntraScenarioObserverType> _theIntraObservers;

	private Vector<ScenarioObserver> _thePlainObservers;

	private ResultsContainer _resultsStore;

	private Vector<CoreScenario> _theScenarios;

	/**
	 * ok, get things up and running. Load the data-files
	 * 
	 * @param scenario
	 *          the scenario file
	 * @param control
	 *          the control file
	 * @return null for success, message for failure
	 */
	private String setup(String scenario, String control)
	{
		// ok, create our genny
		_myGenny = new ScenarioGenerator();

		// now create somewhere for the scenarios to go
		_myScenarioDocuments = new Vector<Document>(0, 1);

		// and now create the list of scenarios
		String res = _myGenny.createScenarios(scenario, control, _myScenarioDocuments);

		return res;
	}

	/**
	 * write this set of scenarios to disk, for later examination
	 * 
	 * @param out
	 *          standard out
	 * @param err
	 *          error out
	 * @param in
	 *          input (to receive user input)
	 * @return success code (0) or failure codes
	 */
	private int writeToDisk(PrintStream out, PrintStream err, InputStream in)
	{
		int res = 0;
		// so,
		try
		{
			String failure = _myGenny.writeTheseToFile(_myScenarioDocuments, false);
			// just check for any other probs
			if (failure != null)
			{
				res = TROUBLE_MAKING_FILES;
			}
		}
		catch (Exception e)
		{
			res = TROUBLE_MAKING_FILES;
		}

		return res;
	}

	/**
	 * ok, let's get going...
	 * 
	 * @param out
	 * @param err
	 */
	private int runAll(OutputStream out, OutputStream err, InputStream in,
			Document controlFile)
	{
		int result = SUCCESS;

		// get the data we're after
		String controlStr = ScenarioGenerator.writeToString(_myGenny.getControlFile());
		InputStream controlStream = new ByteArrayInputStream(controlStr.getBytes());


		// ok, everything's loaded. Just have a pass through to
		// initialise any intra-scenario observers
		for (int thisObs = 0; thisObs < _theIntraObservers.size(); thisObs++)
		{
			ScenarioObserver scen = _theIntraObservers
					.elementAt(thisObs);
			if (scen.isActive())
			{
				IntraScenarioObserverType obs = (IntraScenarioObserverType) scen;
				// is it active?
				obs.initialise(_resultsStore.outputDirectory);
			}
		}

		final int scenarioLen = _myScenarioDocuments.size();

		// ok, we've got our scenarios up and running, might as well run through
		// them
		for (int i = 0; i < scenarioLen; i++)
		{
			// get the scenario
			Document document = _myScenarioDocuments.elementAt(i);

			// ok, put it into a stream
			String scenarioStr = ScenarioGenerator.writeToString(document);
			InputStream scenarioStream = new ByteArrayInputStream(scenarioStr
					.getBytes());

			File newOutputSubDirectory = new File(_resultsStore.outputDirectory, ""
					+ (i + 1) + "/");

			// and run through this one
			runThisOne(controlStream, scenarioStream, _resultsStore.observerList,
					newOutputSubDirectory, _resultsStore.randomSeed, i, scenarioLen);

			try
			{
				// and reset the control stream
				controlStream.reset();
			}
			catch (IOException e)
			{
				e.printStackTrace(); // To change body of catch statement use Options |
				// File Templates.
			}
		}

		// ok, everything's loaded. Just have a pass through to
		// close any intra-scenario observers
		for (int thisObs = 0; thisObs < _theIntraObservers.size(); thisObs++)
		{
			ScenarioObserver scen = _theIntraObservers
					.elementAt(thisObs);
			if (scen.isActive())
			{
				IntraScenarioObserverType obs = _theIntraObservers
						.elementAt(thisObs);
				obs.finish();
			}
		}

		return result;
	}

	/**
	 * run through a single scenario - using the ASSET command line runner
	 * 
	 * @param controlStream
	 *          the stream containing the control file
	 * @param scenarioStream
	 *          the stream containing the scenario
	 * @param theObservers
	 *          and observers to setup
	 * @param outputDirectory
	 *          the output directory to dump into
	 * @param theSeed
	 *          a seed for this scenario
	 * @param thisIndex
	 *          a counter running through the scenarios
	 * @param numScenarios
	 *          the total number of scenarios
	 */
	private void runThisOne(InputStream controlStream,
			InputStream scenarioStream, Vector<ScenarioObserver> theObservers,
			File outputDirectory, Integer theSeed, int thisIndex, int numScenarios)
	{
		// load the data
		CommandLine runner = new CommandLine();

		// load the scenario
		// ok, read in the scenario
		CoreScenario theScenario = runner.getScenario();
		ASSET.Util.XML.ASSETReaderWriter.importThis(theScenario, null,
				scenarioStream);

		// now set the seed
		theScenario.setSeed(theSeed);

		// ok, get the scenario, so we can set up our observers
		for (int i = 0; i < theObservers.size(); i++)
		{
			CoreObserver thisObs = (CoreObserver) theObservers.elementAt(i);

			// is it file-related?
			if (thisObs instanceof RecordToFileObserverType)
			{
				RecordToFileObserverType rec = (RecordToFileObserverType) thisObs;
				rec.setDirectory(outputDirectory);
			}

			// and set it up
			thisObs.setup(runner.getScenario());

			// and add to the runner
			runner.addObserver(thisObs);
		}

		System.out.print("Run " + (thisIndex + 1) + " of " + numScenarios + " ");

		// and get going....
		runner.run();

		// and remove the observers
		runner.clearObservers();
	}

	/**
	 * member method, effectively to handle "main" processing.
	 * 
	 * @param args
	 *          the arguments we received from the command line
	 * @param out
	 *          standard out
	 * @param err
	 *          error out
	 * @param in
	 *          input (to receive user input)
	 * @return success code (0) or failure codes
	 */

	public int prepareThis(String controlFile, String scenarioFile, PrintStream out,
			PrintStream err, InputStream in)
	{
		int resCode = 0;

		System.out.println("about to generate scenarios");

		// and set it up (including generating the scenarios)
		String res = setup(scenarioFile, controlFile);

		if (res != null)
		{
			// see what it was, file not found?
			if (res.indexOf("not found") >= 0)
			{
				err.println("Problem finding control file:" + res);
				resCode = FILE_NOT_FOUND;
			}
			else
			{
				err.println("Problem loading multi-scenario generator:" + res);
				resCode = PROBLEM_LOADING;
			}
		}
		else
		{
			out.println("about to write new scenarios to disk");

			// ok, now write the scenarios to disk
			resCode = writeToDisk(out, err, in);

			// and let our generator ditch some gash
		//	_myGenny = null;

			// there was lots of stuff read in by the scenario generator. Whilst
			// we've removed our only reference to
			// it on the previous line, the system won't necessarily do a GC just
			// yet - so we'll trigger an artificial one.
			System.gc();

			if (resCode != SUCCESS)
			{
				if (resCode == TROUBLE_MAKING_FILES)
				{
					err
							.println("Failed to write new scenarios to disk.  Is an old copy of an output file currently open?");
					err
							.println("  Alternately, is a file-browser currently looking at the output directory?");
				}
			}
		}
		
		// convert the control file to a stream
		String controlStr = ScenarioGenerator.writeToString(_myGenny.getControlFile());
		InputStream controlStream = new ByteArrayInputStream(controlStr.getBytes());

		System.out.println("about to import Control file");

		_resultsStore = ASSETReaderWriter
				.importThisControlFile(null, controlStream);

		// sort out observers (inter & intra)
		_theIntraObservers = new Vector<IntraScenarioObserverType>(
				0, 1);
		_thePlainObservers = new Vector<ScenarioObserver>();

		// start off by generating the time/state observers that we create for everybody
		_thePlainObservers.add(new ScenarioStatusObserver());
		_thePlainObservers.add(new TimeObserver());
		
		// also add those from the file
		Vector<ScenarioObserver> theObservers = _resultsStore.observerList;
		for (int i = 0; i < theObservers.size(); i++)
		{
			ScenarioObserver observer = theObservers.elementAt(i);
			if (observer instanceof IntraScenarioObserverType)
			{
				_theIntraObservers.add((IntraScenarioObserverType) observer);
			}
			else
				_thePlainObservers.add(observer);
		}

		// also read in the collection of scenarios
		 _theScenarios = new Vector<CoreScenario>(0,1);

		for (Iterator<Document> iterator = _myScenarioDocuments.iterator(); iterator.hasNext();)
		{
			Document thisD = iterator.next();
			String scenarioStr = ScenarioGenerator.writeToString(thisD);
			InputStream scenarioStream = new ByteArrayInputStream(scenarioStr
					.getBytes());
			CoreScenario newS = new CoreScenario();
			ASSETReaderWriter.importThis(newS, null, scenarioStream);
			_theScenarios.add(newS);
		}
		
		
		
		return resCode;
	}
	

	public int nowRun(PrintStream out, PrintStream err, InputStream in)
	{
		return runAll(out, err, in, _myGenny.getControlFile());
	}

	// //////////////////////////////////////////////////////////
	// testing stuff
	// //////////////////////////////////////////////////////////
	public static class MultiServerTest extends SupportTesting
	{
		public MultiServerTest(final String val)
		{
			super(val);
		}

		public void testValidStartup()
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ByteArrayOutputStream bes = new ByteArrayOutputStream();

			PrintStream out = new PrintStream(bos);
			PrintStream err = new PrintStream(bes);
			InputStream in = new ByteArrayInputStream(new byte[]
			{});

			bos.reset();
			bes.reset();
			String[] args = new String[2];
			args[1] = "src/ASSET/Util/MonteCarlo/test_variance_scenario.xml";
			args[0] = "src/ASSET/Util/MonteCarlo/test_variance1.xml";
			// args[1] =
			// "..\\src\\java\\ASSET_SRC\\ASSET\\Util\\MonteCarlo\\test_variance1.xml";
			MultiScenarioCore scen = new MultiScenarioCore();
			int res = scen.prepareThis(args[0], args[1], out, err, in);
			assertEquals("ran ok", SUCCESS, res);

			// check the contents of the error message
			assertEquals("no error reported", 0, bes.size());

			// check the scenarios got created
			Vector<Document> scenarios = scen._myScenarioDocuments;
			assertEquals("scenarios got created", 3, scenarios.size());
		}

		public void testCommandLineMainProcessing()
		{
			String[] args = new String[2];
			args[0] = "src/ASSET/Util/MonteCarlo/test_variance_scenario.xml";
			args[1] = "src/ASSET/Util/MonteCarlo/test_variance_realistic.xml";

			CommandLine.main(args);
		}
	}

	// //////////////////////////////////////////////////////////
	// and now the main method
	// //////////////////////////////////////////////////////////

	/**
	 * main method, of course - decides whether to handle this ourselves, or to
	 * pass it on to the command line
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		MultiServerTest tm = new MultiServerTest("me");
		SupportTesting.callTestMethods(tm);
	}

	@Override
	public Iterable<IAttribute> getAttributes()
	{
		// look at our observers, find any attributes
		Vector<IAttribute> attrs = new Vector<IAttribute>();
		
		for (Iterator<ScenarioObserver> iterator = _thePlainObservers.iterator(); iterator.hasNext();)
		{
			ScenarioObserver thisS = iterator.next();
			if(thisS instanceof IAttribute)
				attrs.add((IAttribute) thisS);
		}
		
		return attrs;
	}

	@Override
	public Vector<ISimulation> getSimulations()
	{
		Vector<ISimulation> res = new Vector<ISimulation>();
		res.addAll(_theScenarios);
		// return my list of simulations
		return res;
	}

	@Override
	public boolean isRunning()
	{
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void startQue()
	{
		// ok, go for it
		nowRun(System.out, System.err, System.in);
	}

	@Override
	public void stopQue()
	{
		// TODO Auto-generated method stub
		
	}


}