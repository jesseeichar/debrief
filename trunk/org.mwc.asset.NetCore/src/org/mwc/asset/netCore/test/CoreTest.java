package org.mwc.asset.netCore.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.Vector;

import org.mwc.asset.netCore.common.Network.AHandler;
import org.mwc.asset.netCore.common.Network.LightParticipant;
import org.mwc.asset.netCore.common.Network.LightScenario;
import org.mwc.asset.netCore.core.AServer;
import org.mwc.asset.netCore.core.MClient;

import ASSET.ScenarioType;
import ASSET.Models.DecisionType;
import ASSET.Models.Decision.Movement.Wander;
import ASSET.Models.Detection.DetectionList;
import ASSET.Models.Movement.SurfaceMovementCharacteristics;
import ASSET.Models.Sensor.Cookie.TypedCookieSensor;
import ASSET.Models.Sensor.Cookie.TypedCookieSensor.TypedRangeDoublet;
import ASSET.Models.Vessels.Surface;
import ASSET.Participants.Category;
import ASSET.Participants.CoreParticipant;
import ASSET.Participants.DemandedStatus;
import ASSET.Participants.ParticipantDecidedListener;
import ASSET.Participants.ParticipantDetectedListener;
import ASSET.Participants.ParticipantMovedListener;
import ASSET.Participants.Status;
import ASSET.Scenario.CoreScenario;
import ASSET.Scenario.MultiScenarioLister;
import ASSET.Scenario.ScenarioSteppedListener;
import MWC.GenericData.Duration;
import MWC.GenericData.WorldDistance;
import MWC.GenericData.WorldLocation;
import MWC.GenericData.WorldSpeed;

import com.esotericsoftware.minlog.Log;
import com.esotericsoftware.minlog.Log.Logger;

public class CoreTest extends junit.framework.TestCase
{

	public static MyPart testParticipant;

	private Vector<String> _events;
	protected Vector<LightScenario> _myList;
	private static Vector<ScenarioType> _myScenarios;

	@Override
	protected void setUp() throws Exception
	{
		// TODO Auto-generated method stub
		super.setUp();

		_events = new Vector<String>();
		Logger logger = new Logger()
		{

			@Override
			public void log(int level, String category, String message, Throwable ex)
			{
				_events.add(message);
			}
		};
		Log.setLogger(logger);
	}

	public void testZConnect() throws InterruptedException, IOException
	{
		// check events empty
		assertEquals("events empty", 0, _events.size());
		AServer server = new AServer();
		server.start();
		assertEquals("events recorded", 1, _events.size());
		assertEquals("correct start event", "Server opened.", _events.elementAt(0));

		// and now the client
		MClient client = new MClient();
		assertEquals("still no more events", 1, _events.size());

		// try with dodgy target
		boolean errorThrown = false;
		try
		{
			client.connect("128.3.3.3");
		}
		catch (IOException e)
		{
			errorThrown = true;
		}
		assertTrue("Exception thrown with dodgy target", errorThrown);
		assertEquals("events recorded", 2, _events.size());
		System.err.println("event is:" + _events.elementAt(1));
		assertTrue("correct client event",
				_events.elementAt(1).contains("Connecting: /128.3.3.3:54555/54778"));

		// now real connection
		client.connect(null);
		Thread.sleep(100);

		client.stop();
		server.stop();
	}

	public void testStartup() throws InterruptedException, IOException
	{
		// check events empty
		assertEquals("events empty", 0, _events.size());
		AServer server = new AServer();
		server.start();
		assertEquals("events recorded", 1, _events.size());
		assertEquals("correct start event", "Server opened.", _events.elementAt(0));

		// and now the client
		MClient client = new MClient();
		assertEquals("still no more events", 1, _events.size());

		// now real connection
		client.connect(null);
		Thread.sleep(100);

		assertEquals("events recorded", 5, _events.size());
		assertTrue("correct client event",
				_events.elementAt(1).contains("Discovered server"));
		assertTrue("correct client event",
				_events.elementAt(2).contains("Connecting"));
		assertTrue("correct client event",
				_events.elementAt(3).contains("connected"));
		assertTrue("correct client event",
				_events.elementAt(4).contains("connected"));

		// ok, give the server some data
		MultiScenarioLister lister = new MultiScenarioLister()
		{

			@Override
			public Vector<ScenarioType> getScenarios()
			{
				return getScenarioList();
			}
		};
		server.setDataProvider(lister);

		assertNull("scen list should be empty", _myList);
		AHandler<Vector<LightScenario>> sHandler = new AHandler<Vector<LightScenario>>()
		{
			@Override
			public void onSuccess(Vector<LightScenario> result)
			{
				_myList = result;
			}
		};
		// fire in request for scenarios
		System.err.println("about to request scenarios");
		client.getScenarioList(sHandler);
		Thread.sleep(300);

		showEvents(_events);

		assertNotNull("scen list should have data", _myList);
		assertEquals("scen has correct number", 3, _myList.size());
		LightScenario scen = _myList.elementAt(0);
		assertEquals("scen has correct name", "aaa", scen.name);
		assertEquals("has correct parts", 3, scen.listOfParticipants.size());
		LightParticipant firstPart = scen.listOfParticipants.firstElement();
		assertEquals("has correct first part", "p12", firstPart.name);
		assertEquals("has correct first id", 12, firstPart.id);

		// check we have no lsitenser....
		assertEquals("no listeners", 0, server.getPartListeners().size());

		// ok, check scenario listen/release
		assertEquals("no scen lsiteners", 0, server.getScenListeners().size());
		Vector<String> stepLog = new Vector<String>();
		ScenarioSteppedListener sListener = new MySListener(stepLog);
		client.listenScen(scen.name, sListener);
		Thread.sleep(100);

		assertEquals("our scen lsitener got registered", 1, server
				.getScenListeners().size());
		assertEquals("step evnts empty", 0, stepLog.size());

		// ok, are we rx step events?
		server.step(scen.name);
		Thread.sleep(100);

		assertEquals("step evnts populated", 1, stepLog.size());

		// ok, stop listening
		client.stopListenScen(scen.name);
		Thread.sleep(100);

		// ok, are we rx step events?
		server.step(scen.name);
		Thread.sleep(100);
		assertEquals("no new step evnts populated", 1, stepLog.size());

		// ok, start listening again
		client.listenScen(scen.name, sListener);
		Thread.sleep(100);

		// ok, are we rx step events?
		server.step(scen.name);
		Thread.sleep(100);

		assertEquals("rx new step event", 2, stepLog.size());

		// next, we want to listen to a participant
		Vector<String> moveLog = new Vector<String>();
		Vector<String> detectLog = new Vector<String>();
		CombinedListener combi = new CombinedListener(moveLog, detectLog);
		client.listenPart(scen.name, firstPart.id, combi, combi, combi);
		Thread.sleep(600);

		assertEquals("a listener", 1, server.getPartListeners().size());

		// ok, now try to release
		client.stopListenPart(scen.name, firstPart.id);
		Thread.sleep(600);

		// System.in.read();
		assertEquals("no listener", 0, server.getPartListeners().size());

		// connect again
		// ok, now try to control the participant
		client.listenPart(scen.name, firstPart.id, combi, combi, combi);
		Thread.sleep(600);

		assertEquals("a listener", 1, server.getPartListeners().size());

		// move scenario & see if movement occurs...
		client.step(scen.name);
		Thread.sleep(200);
		client.step(scen.name);
		Thread.sleep(200);
		client.step(scen.name);
		Thread.sleep(200);

		// check we've seen some mvoement
		assertEquals("movement detected", 3, moveLog.size());
		assertTrue("has movement", moveLog.firstElement().startsWith("move"));
		
		// and some dtections
		assertEquals("detections performed", 3, moveLog.size());

		// check the course
		assertEquals("has correct course", 12d, combi.lastStat.getCourse(), 0.001);

		// ok, stop listening
		// ok, now try to release
		client.stopListenPart(scen.name, firstPart.id);
		Thread.sleep(600);

		assertEquals("no listener", 0, server.getPartListeners().size());

		// do another step anyway
		server.step(scen.name);
		Thread.sleep(200);

		// check we've got the same movement
		assertEquals("movement detected", 3, moveLog.size());

		// ok, reconnect, and try driving it...
		// ok, now try to control the participant
		client.listenPart(scen.name, firstPart.id, combi, combi, combi);
		Thread.sleep(600);
		assertEquals("a listener", 1, server.getPartListeners().size());

		// pl. try driving
		assertEquals("original dec model", "DefaultWander", testParticipant
				.getDecisionModel().getName());
		client.controlPart(scen.name, firstPart.id, 55d, 4d, 0d);

		// move forward & look for change in course/speed
		server.step(scen.name);
		Thread.sleep(200);
		server.step(scen.name);
		Thread.sleep(200);
		server.step(scen.name);
		Thread.sleep(200);

		// check we've seen some mvoement
		assertEquals("movement detected", 6, moveLog.size());
		assertTrue("has movement", moveLog.firstElement().startsWith("move"));

		// check the model is user control
		assertEquals("user control dec model", AServer.NETWORK_CONTROL,
				testParticipant.getDecisionModel().getName());

		// check the course
		assertEquals("has correct course", 55d, combi.lastStat.getCourse(), 0.001);
		assertEquals("has correct speed", 4d,
				combi.lastStat.getSpeed().getValueIn(WorldSpeed.Kts), 0.001);

		// ok, release control
		client.releasePart(scen.name, firstPart.id);
		Thread.sleep(200);
		assertEquals("original dec model restored", "DefaultWander",
				testParticipant.getDecisionModel().getName());

		// check we stop receiving updates
		client.stopListenPart(scen.name, firstPart.id);
		Thread.sleep(50);
		server.step(scen.name);
		Thread.sleep(200);
		server.step(scen.name);
		Thread.sleep(200);
		assertEquals("no more steps received", 6, moveLog.size());

		// ///////////////////////////
		// checking down more than one level
		// ///////////////////////////
		client.listenPart(scen.name, firstPart.id, combi, combi, combi);
		assertEquals("original dec model", "DefaultWander", testParticipant
				.getDecisionModel().getName());
		client.controlPart(scen.name, firstPart.id, 55d, 4d, 0d);
		Thread.sleep(200);

		// check the model is under user control
		assertEquals("user control dec model", AServer.NETWORK_CONTROL,
				testParticipant.getDecisionModel().getName());

		// ok, now step down two levels
		client.stopListenPart(scen.name, firstPart.id);

		// check the behaviour got restored
		Thread.sleep(200);
		assertEquals("original dec model restored", "DefaultWander",
				testParticipant.getDecisionModel().getName());

		// HOW ABOUT TWO LEVELS!
		client.listenPart(scen.name, firstPart.id, combi, combi, combi);
		assertEquals("original dec model", "DefaultWander", testParticipant
				.getDecisionModel().getName());
		client.controlPart(scen.name, firstPart.id, 55d, 4d, 0d);
		Thread.sleep(200);

		// check the model is under user control
		assertEquals("user control dec model", AServer.NETWORK_CONTROL,
				testParticipant.getDecisionModel().getName());
		assertEquals("only one listener", 1, server.getPartListeners().size());

		// ok, collapse it!
		client.stopListenScen(scen.name);

		Thread.sleep(200);
		assertEquals("original dec model restored", "DefaultWander",
				testParticipant.getDecisionModel().getName());
		assertEquals("zero listeners", 0, server.getPartListeners().size());

		showEvents(_events);
		System.out.println("pausing");
		// System.in.read();
		client.stop();
		server.stop();
	}

	protected static class MyScen extends CoreScenario
	{
		public MyScen()
		{
			this.setScenarioStepTime(new Duration(1, Duration.MINUTES));
			this.setStepTime(new Duration(1, Duration.SECONDS));
		}
	}

	protected static class MyPart extends Surface
	{
		/**
			 * 
			 */
		private static final long serialVersionUID = 1L;

		public MyPart(int id, String force)
		{
			super(id);
			setName("p" + id);
			this.setCategory(new Category(force, Category.Environment.SURFACE,
					Category.Type.FRIGATE));
			WorldLocation centre = new WorldLocation(12, 12, 2);
			WorldDistance area = new WorldDistance(12, WorldDistance.NM);
			DecisionType wander = new Wander(centre, area);
			wander.setName("DefaultWander");
			this.setDecisionModel(wander);
			Status newStat = new Status(12, 0);
			newStat.setLocation(new WorldLocation(11 + id /100d,
					11 - id/100d, 11));
			newStat.setCourse(12);
			newStat.setSpeed(new WorldSpeed(12, WorldSpeed.Kts));
			setMovementChars(SurfaceMovementCharacteristics.getSampleChars());
			this.setStatus(newStat);

			Vector<TypedRangeDoublet> rangeDoublets = new Vector<TypedRangeDoublet>();
			rangeDoublets.add(new TypedRangeDoublet(null, new WorldDistance(10,
					WorldDistance.NM)));
			// and a sensor
			TypedCookieSensor sensor = new TypedCookieSensor(id * 30, rangeDoublets);
			this.addSensor(sensor);

		}
	}

	public class MySListener implements ScenarioSteppedListener
	{
		private Vector<String> _items;

		public MySListener(Vector<String> items)
		{
			_items = items;
		}

		public void step(ScenarioType scenario, long newTime)
		{
			_items.add("step to:" + newTime);
		}

		@Override
		public void restart(ScenarioType scenario)
		{
			_items.add("restart");
		}
	};

	private static class CombinedListener implements ParticipantMovedListener,
			ParticipantDetectedListener, ParticipantDecidedListener
	{
		private Vector<String> moves;
		private Vector<String> detects;
		protected Status lastStat;

		public CombinedListener(Vector<String> movements, Vector<String> detections)
		{
			moves = movements;
			detects = detections;
		}

		@Override
		public void newDecision(String description, DemandedStatus dem_status)
		{
			moves.add("dec:" + description);
		}

		@Override
		public void newDetections(DetectionList detections)
		{
			detects.add("det:" + detections.size());
		}

		@Override
		public void moved(Status newStatus)
		{
			moves.add("move:" + newStatus.getTime());
			lastStat = newStatus;
			System.err
					.println(newStatus.getTime() + " at:" + newStatus.getLocation());
		}

		@Override
		public void restart(ScenarioType scenario)
		{
		}

	}

	public static Vector<ScenarioType> getScenarioList()
	{
		if (_myScenarios == null)
		{
			_myScenarios = new Vector<ScenarioType>();

			CoreScenario scen = new MyScen();
			scen.setName("aaa");
			testParticipant = new MyPart(12, Category.Force.RED);
			CoreParticipant cp2 = new MyPart(13, Category.Force.BLUE);
			CoreParticipant cp3 = new MyPart(14, Category.Force.BLUE);
			CoreParticipant cp4 = new MyPart(22, Category.Force.RED);
			CoreParticipant cp5 = new MyPart(23, Category.Force.BLUE);
			CoreParticipant cp6 = new MyPart(24, Category.Force.BLUE);
			CoreParticipant cp7 = new MyPart(32, Category.Force.BLUE);
			CoreParticipant cp8 = new MyPart(33, Category.Force.RED);
			CoreParticipant cp9 = new MyPart(34, Category.Force.BLUE);
			scen.addParticipant(testParticipant.getId(), testParticipant);
			scen.addParticipant(cp2.getId(), cp2);
			scen.addParticipant(cp3.getId(), cp3);
			CoreScenario scen2 = new MyScen();
			scen2.addParticipant(cp4.getId(), cp4);
			scen2.addParticipant(cp5.getId(), cp5);
			scen2.addParticipant(cp6.getId(), cp6);
			scen2.setName("aab");
			CoreScenario scen3 = new MyScen();
			scen3.addParticipant(cp7.getId(), cp7);
			scen3.addParticipant(cp8.getId(), cp8);
			scen3.addParticipant(cp9.getId(), cp9);
			scen3.setName("aac");

			_myScenarios.add(scen);
			_myScenarios.add(scen2);
			_myScenarios.add(scen3);
		}

		return _myScenarios;
	}

	protected static void showEvents(Vector<String> events)
	{
		System.out.println("=================");
		Iterator<String> iter = events.iterator();
		int ctr = 0;
		while (iter.hasNext())
		{
			String string = (String) iter.next();
			System.out.println(++ctr + " - " + string);
		}
	}

}
