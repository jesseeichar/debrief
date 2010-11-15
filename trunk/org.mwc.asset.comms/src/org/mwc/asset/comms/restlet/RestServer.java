package org.mwc.asset.comms.restlet;

import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

public class RestServer extends   ServerResource
{
  public static void main(String[] args) throws Exception {  
    // Create the HTTP server and listen on port 8182  
    new Server(Protocol.HTTP, 8182, RestServer.class).start();  
 }

 @Get 
 public String toString() {  
    return "hello, world";  
 }

}
