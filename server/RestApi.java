/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.icatproject.topcatdoiplugin;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.icatproject.topcatdoiplugin.topcatclient.TopcatClient;
import org.icatproject.topcatdoiplugin.topcatclient.TopcatClientException;

/**
 *
 * @author elz24996
 */
@Stateless
@LocalBean
@Path("")
public class RestApi {
    
    private static final Logger logger = LoggerFactory.getLogger(RestApi.class);
    
    /**
     * Used to detect whether app is running or not.
     *
     * @summary ping
     *
     * @return a string "ok" if all is well
    */
    @GET
    @Path("/ping")
    @Produces({MediaType.APPLICATION_JSON})
    public Response ping() {
        logger.info("ping() called");
        return Response.ok().entity("\"ok\"").build();
    }

    @GET
    @Path("/version")
    @Produces({MediaType.APPLICATION_JSON})
    public Response version() {
        TopcatClient topcatClient = new TopcatClient("https://localhost:8181");
        try {
            return Response.ok().entity("\"" + topcatClient.getVersion() + "\"").build();
        } catch(TopcatClientException e){
            return e.toResponse();
        }
    }

    // @POST
    // @Path("/makeDataPublic")
    // @Produces({MediaType.APPLICATION_JSON})
    // public Response makeDataPublic(){

    // }
    
}
