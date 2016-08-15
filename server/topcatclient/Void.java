
package org.icatproject.topcatdoiplugin.topcatclient;

import javax.ws.rs.core.Response;

/**
 *
 * @author elz24996
 */
public class Void implements ResponseProducer {
    
    public Response toResponse(){
        return Response.ok().build();
    }
    
}
