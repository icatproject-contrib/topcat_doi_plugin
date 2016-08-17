package org.icatproject.topcatdoiplugin.dataciteclient;

import javax.ws.rs.core.Response;

import javax.json.Json;

public class DataCiteClientException extends Exception {
    
    private String message;
    protected int status;
    
    public DataCiteClientException(String message){
        this.status = 400;
        this.message = message;
    }
    
    public String getMessage(){
        return this.message;
    }
    
    public String toString(){
        return Json.createObjectBuilder().add("message", (String) getMessage()).build().toString();
    }
    
    public Response toResponse(){
        return Response.status(status).entity(toString()).build();
    }
    
}