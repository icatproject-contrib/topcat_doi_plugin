
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

import org.icatproject.topcatdoiplugin.dataciteclient.DataCiteClient;
import org.icatproject.topcatdoiplugin.dataciteclient.DataCiteClientException;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;


import org.w3c.dom.Document;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

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

    // <?xml version="1.0" encoding="UTF-8"?>
    // <resource xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://datacite.org/schema/kernel-4" xsi:schemaLocation="http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd">
    //     <identifier identifierType="DOI">#{doi}</identifier>
    //     <creators>
    //         <creator>
    //             <creatorName>Salt, Jody</creatorName>
    //         </creator>
    //     </creators>
    //     <titles>
    //         <title xml:lang="en-gb">Harwell Rounders</title>
    //     </titles>
    //     <publisher>Harwell RecSoc</publisher>
    //     <publicationYear>2016</publicationYear>
    //     <resourceType resourceTypeGeneral="Dataset">Fixtures and Results</resourceType>
    // </resource>

    @GET
    @Path("/makeDataPublic")
    @Produces({MediaType.APPLICATION_JSON})
    public Response makeDataPublic(){
        String seedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resource xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://datacite.org/schema/kernel-4\" xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd\"></resource>";
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
        
        
            Document document = builder.parse(new ByteArrayInputStream(seedXml.getBytes("UTF-8")));
            
            Element root = document.getDocumentElement();
            
            Element identifier = document.createElement("identifier");
            identifier.setAttribute("identifierType", "DOI");
            identifier.appendChild(document.createTextNode("10.5286/topcat/sciency-stuff"));
            root.appendChild(identifier);

            Element creators = document.createElement("creators");
            root.appendChild(creators);

            Element creator = document.createElement("creator");
            creators.appendChild(creator);

            Element creatorName = document.createElement("creatorName");
            creatorName.appendChild(document.createTextNode("Dr Bob"));
            creator.appendChild(creatorName);

            Element titles = document.createElement("titles");
            root.appendChild(titles);

            Element title = document.createElement("title");
            title.setAttribute("xml:lang", "en-gb");
            title.appendChild(document.createTextNode("Hello World!"));
            titles.appendChild(title);

            Element publisher = document.createElement("publisher");
            publisher.appendChild(document.createTextNode("The Foo Bar Institute"));
            root.appendChild(publisher);

            Element publicationYear = document.createElement("publicationYear");
            publicationYear.appendChild(document.createTextNode("2016"));
            root.appendChild(publicationYear);

            Element resourceType = document.createElement("resourceType");
            resourceType.setAttribute("resourceTypeGeneral", "Dataset");
            resourceType.appendChild(document.createTextNode("Serious Research"));
            root.appendChild(resourceType);

            DataCiteClient dataCiteClient = new DataCiteClient();

            dataCiteClient.setDoiMetadata(document);

            return Response.ok().entity("\"ok\"").build();
        } catch(Exception e){
            return Response.ok().entity(e.toString()).build();
        }
    }
    
}
