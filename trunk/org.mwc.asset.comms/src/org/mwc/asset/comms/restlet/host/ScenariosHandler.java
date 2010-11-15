package org.mwc.asset.comms.restlet.host;

import java.util.List;
import java.util.Vector;

import org.mwc.asset.comms.restlet.data.Scenario;
import org.mwc.asset.comms.restlet.data.ScenariosResource;
import org.mwc.asset.comms.restlet.host.ASSETHost.HostProvider;
import org.restlet.resource.Get;

public class ScenariosHandler extends ASSETResource implements
		ScenariosResource
{
	
	@Get
	public List<Scenario> retrieve()
	{
		ASSETHost.HostProvider hostP = (HostProvider) getApplication();
		ASSETHost host = hostP.getHost();
		
		Vector<Scenario> res = host.getScenarios();
		
		return res;
	}
}