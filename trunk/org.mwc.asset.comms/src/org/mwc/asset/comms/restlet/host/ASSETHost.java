package org.mwc.asset.comms.restlet.host;

import java.net.URL;
import java.util.List;
import java.util.Vector;

import org.mwc.asset.comms.restlet.data.Participant;
import org.mwc.asset.comms.restlet.data.Scenario;

import ASSET.ScenarioType;
import ASSET.Participants.DemandedStatus;

/** methods exposed by object capable of acting as ASSET Host in networked simulation
 * 
 * @author ianmayo
 *
 */
public interface ASSETHost
{
	/** how to get at the host object
	 * 
	 * @author ianmayo
	 *
	 */
	public interface HostProvider
	{
		/** get the host object
		 * 
		 * @return
		 */
		public ASSETHost getHost();
	}
	
	
	/** get hold of the specified scenario
	 * 
	 * @param scenarioId
	 * @return
	 */
	public ScenarioType getScenario(int scenarioId);

	/** somebody new wants to listen to us
	 * 
	 * @param scenario
	 * @param url
	 * @return
	 */
	public int newScenarioListener(int scenario, URL url);
	
	/** somebody wants to stop listening to us
	 * @param scenario subject scenario
	 * @param listenerId
	 */
	public void deleteScenarioListener(int scenario, int listenerId);

	/** get a list of scenarios we know about
	 * 
	 * @return
	 */
	public Vector<Scenario> getScenarios();
	
	public List<Participant> getParticipantsFor(int scenarioId);
	
	/** somebody new wants to listen to us
	 * 
	 * @param scenario
	 * @param url
	 * @return
	 */
	public int newParticipantListener(int scenarioId, int participantId, URL url);
	
	/** somebody wants to stop listening to us
	 * @param scenarioId TODO
	 * @param listenerId
	 */
	public void deleteParticipantListener(int scenarioId, int participantId, int listenerId);


	/** find out the current status of this participant
	 * 
	 * @param parseInt
	 * @param parseInt2
	 * @return
	 */
	public DemandedStatus getDemandedStatus(int scenario, int participant);

	/** record a new demanded status for the supplied participant
	 * 
	 * @param scenario
	 * @param participant
	 * @param demState
	 */
	public void setDemandedStatus(int scenario, int participant,
			DemandedStatus demState);
}
