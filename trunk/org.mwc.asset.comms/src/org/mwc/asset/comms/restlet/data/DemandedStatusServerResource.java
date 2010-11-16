package org.mwc.asset.comms.restlet.data;

import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

import ASSET.Participants.DemandedStatus;

/**
 * The server side implementation of the Restlet resource.
 */
public class DemandedStatusServerResource extends ServerResource implements
		DemandedStatusResource
{

	private static DemandedStatus _thisD;

	@Get
	public DemandedStatus retrieve()
	{
		return _thisD;
	}

	@Put
	public void store(final DemandedStatus demState)
	{
		final Map<String, Object> attrs = this.getRequestAttributes();
		final Object thisS = attrs.get("scenario");
		final Object thisP = attrs.get("participant");
		_thisD = demState;
		System.out.println("redirect " + thisS + ", " + thisP + " to:" + demState);
	}
}