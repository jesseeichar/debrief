package org.mwc.asset.comms.restlet.data;

import org.restlet.resource.Get;
import org.restlet.resource.Put;

import ASSET.Participants.DemandedStatus;

/**
 * The resource associated to a contact.
 */
public interface DemandedStatusResource {

    @Get
    public DemandedStatus retrieve();

    @Put
    public void store(DemandedStatus newState);
}
