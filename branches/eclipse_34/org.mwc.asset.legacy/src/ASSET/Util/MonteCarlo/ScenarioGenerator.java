/*
 * Created by IntelliJ IDEA.
 * User: administrator
 * Date: Oct 31, 2001
 * Time: 1:22:06 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package ASSET.Util.MonteCarlo;

import ASSET.Util.RandomGenerator;
import ASSET.Util.SupportTesting;
import ASSET.Util.XML.ScenarioHandler;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Vector;

/**
 * list of items in a particular file which may be changed
 */

public final class ScenarioGenerator
{
  /**
   * the multi-scenario generator
   */
  private MultiScenarioGenerator _scenarioGenny = null;

  /**
   * the multi-participant generator
   */
  private MultiParticipantGenerator _participantGenny = null;

  /**
   * our document
   */
  private Document _targetScenario;

  /**
   * our control file
   */
  private Document _controlDocument;

  /**
   * the directory to dump the scenarios into
   */
  private static final String OUTPUT_DIRECTORY = "OutputDirectory";

  /**
   * the filename to use for the output file(s)
   */
  private static final String FILENAME = "Filename";

  /**
   * the attribute name for the random seed
   */
  private static final String RANDOM_SEED = "RandomSeed";


  /**
   * the directory to place the new files
   */
  private String _myDirectory;

  /**
   * the seed to use for the random number generator
   */
  private Integer _theSeed;


  // phrases to indicate where error may have occured
  public static final String CONTROL_FILE_ERROR = "Control file";
  public static final String TEMPLATE_FILE_ERROR = "Template file";

  /**
   * the name of the file we copy the control file to when doing multi-scenario
   */
  public static final String CONTROL_FILENAME = "control_file.xml";

  /***************************************************************
   *  constructor
   ***************************************************************/
  /**
   * constructor, received a stream containing the list
   * of variances we are going to manage
   */
  public ScenarioGenerator()
  {
  }


  /**
   * ************************************************************
   * member methods
   * ************************************************************
   */

  public String createScenarios(String templatePath, String controlPath, Vector results)
  {
    Document var = null;
    String res = null;

    try
    {
      var = ScenarioGenerator.readDocumentFrom(new FileInputStream(controlPath));
    }
    catch (SAXException e)
    {
      res = "Problem parsing " + CONTROL_FILE_ERROR + e.getMessage();
      e.printStackTrace();
    }
    catch (FileNotFoundException e)
    {
      res = CONTROL_FILE_ERROR + " not found:" + e.getMessage();
    }

    // did it work?
    if (res == null)
    {

      // now try to load the scenario
      Document doc = null;
      try
      {
        doc = ScenarioGenerator.readDocumentFrom(new FileInputStream(templatePath));
      }
      catch (SAXException e)
      {
        res = "Problem parsing " + TEMPLATE_FILE_ERROR + e.getMessage();
      }
      catch (FileNotFoundException e)
      {
        res = TEMPLATE_FILE_ERROR + " file not found:" + e.getMessage();
      }

      // did that work?
      if (res == null)
      {
        // yup, go for it.
        res = doScenarioGeneration(doc, var, results);
      }

    }

    return res;
  }


  /**
   * main loader/creator method. Takes input params and results holder, returns
   * populated holder together with status message
   *
   * @param template    scenario template
   * @param controlFile scenario control file (with builder information)
   * @param results     vector containing the new scenarios
   * @return error message on failure, or null for success
   */
  protected String doScenarioGeneration(Document template, Document controlFile, Vector results)
  {
    String res = null;

    // load the files
    setVariances(controlFile);

    // create the scenario(s)
    setTemplate(template);

    // did we find a random seed?
    if (_theSeed != null)
    {
      RandomGenerator.seed(_theSeed.intValue());
    }

    // and now the permutations
    res = createNewRandomisedPermutations(results);

    return res;
  }

  /**
   * method to write a list of scenarios to file, each in their own directory
   *
   * @param scenarios the list of scenarios to write to file
   * @return String indicating any problems, or void for success
   */
  public String writeTheseToFile(Vector scenarios, boolean deleteExisting)
  {
    String res = null;

    // do we want to delete the existing files?
    if (deleteExisting)
    {
      deleteThisDirectory(new File(_myDirectory));
    }

    // firstly output the control file to disk in the parent directory
    // === which will also ensure the parent directory is present
    String controlFileAsString = ScenarioGenerator.writeToString(_controlDocument);

    File controlOutput = new File(_myDirectory + "/" + CONTROL_FILENAME);
    controlOutput.getAbsoluteFile().getParentFile().mkdirs();
    try
    {
      FileWriter fw = new FileWriter(controlOutput);
      fw.write(controlFileAsString);
      fw.close();

      if (!controlOutput.exists())
      {
        System.err.println("failed to create control file");
      }
    }
    catch (IOException e)
    {
      res = e.getMessage();
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }

    // did it work?
    if (res == null)
    {

      // ok, now loop through the scenarios
      for (int counter = 0; counter < scenarios.size(); counter++)
      {
        Document thisDoc = (Document) scenarios.elementAt(counter);

        String asString = ScenarioGenerator.writeToString(thisDoc);

        NodeList list = thisDoc.getElementsByTagName(ScenarioHandler.SCENARIO_NAME);
        Element scen = (Element) list.item(0);
        String scen_name = scen.getAttribute(MultiScenarioGenerator.SCENARIO_NAME_ATTRIBUTE);


        // and output this string to file
        int thisId = counter + 1;

        // create the path to the new file
        String theDir = _myDirectory + "/" + thisId + "/";

        // declare it as a file
        File outFile = new File(theDir);

        // create any parent directories we need
        outFile.mkdirs();

        String theFile = theDir + scen_name + ".xml";

        try
        {
          // put it into a writer
          FileWriter fw = new FileWriter(theFile);

          // write it out
          fw.write(asString);

          // and close it
          fw.close();
        }
        catch (IOException e)
        {
          if (e instanceof FileNotFoundException)
          {
            res = "Is output file already open?";
          }

          // take a copy of the problem
          res = res + e.getMessage();

          // and print a stack trace
          e.printStackTrace();

          // and cut short the run
          return res;

        }

      }
    }

    return res;
  }


  /**
   * convenience method to get a new document builder
   *
   * @return a new document builder
   */
  public static DocumentBuilder createNewBuilder()
  {
    DocumentBuilder res = null;

    try
    {

      DocumentBuilderFactory _factory = DocumentBuilderFactory.newInstance();
      _factory.setNamespaceAware(true);    // set to false by default in DocumentBuilderFactory
      _factory.setValidating(true);


      _factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                            "http://www.w3.org/2001/XMLSchema");

      //      _factory.setAttribute(
      //         "http://java.sun.com/xml/jaxp/properties/schemaSource",
      //         "file:///:E:/dev/Asset/src/schemas/ASSET.xsd");

      res = _factory.newDocumentBuilder();

      res.setErrorHandler(new ErrorHandler()
      {
        public void warning(SAXParseException ex)
          throws SAXException
        {
          System.err.println("Throwing SAX Warner:" + ex.getMessage());
          throw ex;
        }

        public void error(SAXParseException ex)
          throws SAXException
        {
          System.err.println("Throwing SAX Error" + ex.getMessage());
          throw ex;
        }

        public void fatalError(SAXParseException ex)
          throws SAXException
        {
          System.err.println("Throwing new fatal error:" + ex.getMessage());
          throw ex;
        }
      });
    }
    catch (ParserConfigurationException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }
    return res;
  }

  /**
   * convenience method to load a document from a stream
   */
  public static Document readDocumentFrom(InputStream stream) throws SAXException, SAXParseException
  {
    DocumentBuilder builder = createNewBuilder();
    Document document = null;
    try
    {
      document = builder.parse(stream);
    }
    catch (IOException e)
    {
      System.out.println("IO Exception occurred!");
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }
    return document;
  }

  /**
   * method to validate XML document (normally before saving a computer-generated one)
   *
   * @param doc the document to validate
   * @return a string containing errors, or null if document is valid
   */
  public static String validateThisDocument(InputStream doc)
  {
    Document getIt;
    String res = null;

    try
    {
      getIt = ScenarioGenerator.readDocumentFrom(doc);

      // just do some dummy rubbish to prevent compiler warning
      // that getIt isn't used....
      short scrapNodeType = getIt.getNodeType();
      scrapNodeType = scrapNodeType++;
    }
    catch (SAXParseException e)
    {
      res = e.getMessage();
    }
    catch (SAXException e)
    {
      res = e.getMessage();
    }

    return res;
  }


  /**
   * convenience method to load a document from a stream
   */
  public static Document readDocumentFrom(InputSource stream)
  {
    DocumentBuilder builder = createNewBuilder();
    Document document = null;
    try
    {
      document = builder.parse(stream);
    }
    catch (SAXException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }
    catch (IOException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }
    return document;
  }

  //  /**
  //   * Simple search and replace:
  //   * @param string the String to search
  //   * @param find the target
  //   * @param replace replace target (find) with
  //   * @return java.lang.String the resulting string.
  //   */
  //  public static String searchAndReplace(String string, String find, String replace)
  //  {
  //    // Does the target string have the string we're replacing?
  //    if (string.indexOf(find) >= 0)
  //    {
  //      StringBuffer Result = new StringBuffer();
  //
  //      int pos = -1, lastPos = 0, findLength = find.length();
  //      while ((pos = string.indexOf(find, pos)) >= 0)
  //      {
  //        Result.append(string.substring(lastPos, pos));
  //        Result.append(replace);
  //        pos += findLength;
  //        lastPos = pos;
  //      }
  //      Result.append(string.substring(lastPos));
  //
  //      return Result.toString();
  //    }
  //    else
  //      return string;
  //  }


  /**
   * read in the list of variances, and collate them
   * into our list
   */
  private void setVariances(final Document document)
  {

    try
    {
      // can we find a scenario generator?
      DOMXPath xpath = new DOMXPath("//MultiScenarioGenerator");
      Element el = (Element) xpath.selectSingleNode(document);
      if (el != null)
      {
     		this._scenarioGenny = new MultiScenarioGenerator(document);
      }

      // can we find a scenario generator?
      xpath = new DOMXPath("//MultiParticipantGenerator");
      el = (Element) xpath.selectSingleNode(document);
      if (el != null)
      {
        this._participantGenny = new MultiParticipantGenerator(document);
      }

      xpath = new DOMXPath("//ScenarioGenerator");
      el = (Element) xpath.selectSingleNode(document);

      // retrieve our working values
      xpath = new DOMXPath("//ScenarioController");
      el = (Element) xpath.selectSingleNode(document);

      _myDirectory = el.getAttribute(OUTPUT_DIRECTORY);

      String theSeedStr = el.getAttribute(RANDOM_SEED);
      if (theSeedStr != null)
        if (theSeedStr.length() > 0)
          _theSeed = Integer.valueOf(theSeedStr);

    }
    catch (JaxenException e)
    {
      e.printStackTrace();
    }

    _controlDocument = document;

  }

  /**
   * retrieve the control file
   */
  public Document getControlFile()
  {
    return _controlDocument;
  }

  /**
   * set the document we are going to be changing
   */
  public final void setTemplate(final Document rawDoc)
  {
    _targetScenario = rawDoc;
  }


  /**
   * set the document which we are going to be changing
   */
  public final void setTemplate(final InputStream istream)
  {
    // get a document from the istream
    Document thisDocument = null;
    try
    {
      thisDocument = ScenarioGenerator.readDocumentFrom(istream);
    }
    catch (SAXException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }

    // and store it
    setTemplate(thisDocument);
  }

  //  public final Document getRawDocument()
  //  {
  //    return _targetScenario;
  //  }

  public final String createNewRandomisedPermutations(Vector resultsContainer)
  {

    String res = null;

    // do we have a scenario generator?
    if (_scenarioGenny != null)
    {
      System.out.println("Generating scenarios");
      System.out.println("====================");

      _scenarioGenny.setDocument(_targetScenario);

      // yup, get it to create it's list
      Document[] list = new Document[0];
      try
      {
        list = _scenarioGenny.createNewRandomisedPermutations();
      }
      catch (XMLVariance.IllegalExpressionException e)
      {
        res = e.getMessage();
        return res;
      }
      catch (XMLVariance.MatchingException e)
      {
        res = e.getMessage();
        return res;
      }

      for (int i = 0; i < list.length; i++)
      {
        Document document = list[i];
        resultsContainer.add(document);
      }
    }
    else
    {
      resultsContainer.add(_targetScenario);
    }

    // do we have a participant generator?
    if (_participantGenny != null)
    {

      System.out.println("Mutating participants");
      System.out.println("=====================");

      Vector newResults = new Vector(0, 1);

      // start off by creating our long list of scenario duplicates

      for (int i = 0; i < resultsContainer.size(); i++)
      {
        Document thisScenario = (Document) resultsContainer.elementAt(i);

        // tell the participant about the scenario
        _participantGenny.setDocument(thisScenario);

        //        try
        //        {
        //          System.out.println("Enter to create next permutation:");
        //          int var = System.in.read();
        //          System.out.println(".");
        //        }
        //        catch (IOException e)
        //        {
        //          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        //        }

        // ok, create a new permutation
        try
        {
          thisScenario = _participantGenny.createNewRandomisedPermutation();
        }
        catch (XMLVariance.IllegalExpressionException e)
        {
          res = e.getMessage();
          return res;
        }
        catch (XMLVariance.MatchingException e)
        {
          res = e.getMessage();
          return res;
        }

        newResults.add(thisScenario);

        // and indicate our progress
        outputProgress(i);

      }

      // make sure the command line's on a new line
      System.out.println("");


      // clear our old list
      resultsContainer.removeAllElements();

      // and insert our new items
      resultsContainer.addAll(newResults);
    }

    return res;
  }

  //  /** validate a single document - convering the document
  //   * to a string and getting the string validated
  //   *
  //   * @param scenario the scenario to validate
  //   * @return null for success, message for error
  //   */
  //  private static String validateThisScenario(Document scenario)
  //  {
  //    String res = null;
  //    String thisScena = writeToString(scenario);
  //    InputStream str = new ByteArrayInputStream(thisScena.getBytes());
  //    String test = validateThisDocument(str);
  //
  //    if (test != null)
  //    {
  //      res = test;
  //    }
  //    return res;
  //  }

  //
  //  public OutputStream createOutput(String title, String directory)
  //  {
  //    OutputStream res = null;
  //
  //    try
  //    {
  //      res = new FileOutputStream(directory + "\\" + title + ".xml");
  //    }
  //    catch (FileNotFoundException e)
  //    {
  //      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
  //    }
  //
  //    return res;
  //  }


  final public static String writeToString(Element newElement)
  {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    final TransformerFactory factory
      = TransformerFactory.newInstance();

    try
    {
      final javax.xml.transform.Transformer transformer
        = factory.newTransformer();

      final StreamResult sr = new StreamResult(bos);

      transformer.transform(new DOMSource(newElement), sr);

      bos.close();
    }
    catch (TransformerException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }
    catch (IOException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }

    String res = bos.toString();
    return res;
  }


  final public static String writeToString(Document newDoc)
  {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();

    final TransformerFactory factory
      = TransformerFactory.newInstance();

    try
    {
      final javax.xml.transform.Transformer transformer
        = factory.newTransformer();

      final StreamResult sr = new StreamResult(bos);

      transformer.transform(new DOMSource(newDoc), sr);

      bos.close();
    }
    catch (TransformerException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }
    catch (IOException e)
    {
      e.printStackTrace();  //To change body of catch statement use Options | File Templates.
    }

    String res = bos.toString();
    return res;
  }


  /**
   * Recursively deletes this file and if it is a directory all it contained
   * directories.
   */
  public static boolean deleteThisDirectory(File aFile)
  {
    boolean deleted = true;
    File[] files = aFile.listFiles();
    if (null != files)
    {
      for (int i = 0; i < files.length; i++)
      {
        deleted &= deleteThisDirectory(files[i]);
      }
    }
    deleted &= aFile.delete();
    return deleted;
  }


  //////////////////////////////////////////////////////////////////////////////////////////////////
  // testing for this class
  //////////////////////////////////////////////////////////////////////////////////////////////////
  public static final class ScenarioGennyTest extends SupportTesting
  {
    public static final String VARIANCE_FILE = "test_variance1.xml";
    public static final String SCENARIO_FILE = "test_variance_scenario.xml";

    public ScenarioGennyTest(final String val)
    {
      super(val);
    }

    public final void testLoadVariances()
    {
      String code_root = System.getProperty("CODE_ROOT");
      if (code_root == null)
        code_root = "..\\src\\java";

      final String docPath = code_root + "\\ASSET_SRC\\ASSET\\Util\\MonteCarlo\\";

      Document doc = null;
      try
      {
        File var = new File(docPath + VARIANCE_FILE);
        assertTrue("can find data-file", var.exists());
        doc = ScenarioGenerator.readDocumentFrom(new FileInputStream(var));
      }
      catch (SAXException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }
      catch (FileNotFoundException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }
      ScenarioGenerator genny = new ScenarioGenerator();
      genny.setVariances(doc);

      // check they got loaded
      assertEquals("loaded template name", "c:/temp/asset_test_output/test_variance1", genny._myDirectory);
    }

    public final void testPerformVariances()
    {
      String code_root = System.getProperty("CODE_ROOT");
      if (code_root == null)
        code_root = "..\\src\\java";

      final String docPath = code_root + "\\ASSET_SRC\\ASSET\\Util\\MonteCarlo\\";

      Vector list = new Vector(0, 1);

      ScenarioGenerator genny = new ScenarioGenerator();

      Document var = null;
      try
      {
        var = ScenarioGenerator.readDocumentFrom(new FileInputStream(docPath + VARIANCE_FILE));
      }
      catch (SAXException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }
      catch (FileNotFoundException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }

      Document doc = null;
      try
      {
        doc = ScenarioGenerator.readDocumentFrom(new FileInputStream(docPath + SCENARIO_FILE));
      }
      catch (SAXException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }
      catch (FileNotFoundException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }


      String res = genny.doScenarioGeneration(doc, var, list);

      assertNull("success - no error", res);

      // check there's stuff in the lst
      assertEquals("got some scenarios", 3, list.size());
    }

    public final void testPerformVariancesFromFilenames()
    {
      String code_root = System.getProperty("CODE_ROOT");
      if (code_root == null)
        code_root = "..\\src\\java";

      final String docPath = code_root + "\\ASSET_SRC\\ASSET\\Util\\MonteCarlo\\";

      Vector list = new Vector(0, 1);

      ScenarioGenerator genny = new ScenarioGenerator();

      String res = genny.createScenarios(docPath + SCENARIO_FILE, docPath + VARIANCE_FILE, list);

      assertNull("success - no error", res);

      // check there's stuff in the lst
      assertEquals("got some scenarios", 3, list.size());
    }

    public final void testPerformVariancesFromInvalidFilenames()
    {
      String code_root = System.getProperty("CODE_ROOT");
      if (code_root == null)
        code_root = "..\\src\\java";

      final String docPath = code_root + "\\ASSET_SRC\\ASSET\\Util\\MonteCarlo\\";

      Vector list = new Vector(0, 1);

      ScenarioGenerator genny = new ScenarioGenerator();

      String res = genny.createScenarios(docPath + "test_variance_scnario.xml", docPath + SCENARIO_FILE, list);

      assertNotNull("success - error returned", res);
      assertTrue("correct error returned", res.indexOf(TEMPLATE_FILE_ERROR) > -1);

      // check there's stuff in the lst
      assertEquals("got no scenarios", 0, list.size());

      res = genny.createScenarios(docPath + SCENARIO_FILE, docPath + "test_varince1.xml", list);

      assertNotNull("success - error returned", res);
      assertTrue("correct error returned", res.indexOf(CONTROL_FILE_ERROR) > -1);

      // check there's stuff in the lst
      assertEquals("got no scenarios", 0, list.size());

    }


    public void testWriteScenariosToFile()
    {
      String outputDir = "c:\\temp\\asset_test_output";

      // check the output directory is clear
      File outputDirectory = new File(outputDir);

      deleteThisDirectory(outputDirectory);

      assertFalse("we should have deleted the output directory", outputDirectory.exists());
      // ok, put dummy seed into the output directory to check that it's been deleted


      String code_root = System.getProperty("CODE_ROOT");
      if (code_root == null)
        code_root = "..\\src\\java";

      final String docPath = code_root + "\\ASSET_SRC\\ASSET\\Util\\MonteCarlo\\";

      Vector list = new Vector(0, 1);

      ScenarioGenerator genny = new ScenarioGenerator();

      Document var = null;
      try
      {
        var = ScenarioGenerator.readDocumentFrom(new FileInputStream(docPath + VARIANCE_FILE));
      }
      catch (SAXException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }
      catch (FileNotFoundException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }

      Document doc = null;
      try
      {
        doc = ScenarioGenerator.readDocumentFrom(new FileInputStream(docPath + SCENARIO_FILE));
      }
      catch (SAXException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }
      catch (FileNotFoundException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }


      String res = genny.doScenarioGeneration(doc, var, list);

      assertNull("success - no error", res);

      // check there's stuff in the lst
      assertEquals("got some scenarios", 3, list.size());

      // ok, we've read in the data - find out where the output directory is, so
      // that we can seed it with an old file (to check that they get deleted)
      String theDir = genny._myDirectory;
      File touchFile = new File(theDir + "/" + "seed_file.txt");
      try
      {
        createNewFile(touchFile);
      }
      catch (IOException e)
      {
        e.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }

      assertTrue("check we created the touch file", touchFile.exists());

      // and now write them out
      genny.writeTheseToFile(list, true);

      // and check that the touch file has been deleted
      assertFalse("check that the touch file has been deleted", touchFile.exists());
    }

    /**
     * Emulation of File.createNewFile for JDK 1.1.
     * <p/>
     * <p>This method does <strong>not</strong> guarantee that the
     * operation is atomic.</p>
     *
     * @since 1.21, Ant 1.5
     */
    private boolean createNewFile(File f) throws IOException
    {
      if (f != null)
      {
        if (f.exists())
        {
          return false;
        }

        FileOutputStream fos = null;
        try
        {
          // first create the parent directory(s) if needed
          File parentFile = f.getParentFile();
          boolean res = parentFile.mkdirs();

          // and now the file itself
          fos = new FileOutputStream(f);
          fos.write(new byte[0]);
        }
        finally
        {
          if (fos != null)
          {
            fos.close();
          }
        }

        return true;
      }
      return false;
    }

    public void testValidation()
    {
      //      String str = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
      //      str += "<Scenario xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"file:///E:/dev/Asset/src/schemas/ASSET.xsd/\" Created=\"2003-12-12T12:00:00\" Name=\"the name\" StartTime=\"2003-12-12T12:00:00\">";
      //      str += "<StepTime Units=\"seconds\" Value=\"45\"/>";
      //      str += "<Participants/>";
      //      str += "</Scenario>";

      String res = null;

      try
      {
        final FileInputStream doc = new FileInputStream("..\\src\\java\\ASSET_SRC\\ASSET\\Util\\MonteCarlo\\small_test_scenario.xml");
        assertNotNull("we found test document", doc);
        res = ScenarioGenerator.validateThisDocument(doc);
      }
      catch (FileNotFoundException e1)
      {
        e1.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }

      // check if/why validation isn't working
      assertNull("valid file didn't throw any errors", res);

      try
      {
        final FileInputStream doc = new FileInputStream("..\\src\\java\\ASSET_SRC\\ASSET\\Util\\MonteCarlo\\small_test_scenario_invalid.xml");
        assertNotNull("we found test document", doc);
        res = ScenarioGenerator.validateThisDocument(doc);
      }
      catch (FileNotFoundException e1)
      {
        e1.printStackTrace();  //To change body of catch statement use Options | File Templates.
      }
      catch (Exception se)
      {
        System.out.println("OTHER EXCEPTION THROWN");
        se.printStackTrace();
      }

      // check if/why validation isn't working
      assertNotNull("valid file didn't throw any errors", res);

      // and check it's contents
      assertEquals("correct error message", "cvc-elt.1: Cannot find the declaration of element 'Scenaio'.", res);

    }

  }

  /**
   * helper method to show progress to the command line
   *
   * @param counter how far we've got
   */
  public static final void outputProgress(int counter)
  {
    if (counter % 100 == 0)
    {
      System.out.println("-");
    }
    else if (counter % 10 == 0)
    {
      System.out.print("%");
    }
  }

}
