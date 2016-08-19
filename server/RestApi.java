
package org.icatproject.topcatdoiplugin;

import java.util.List;
import java.util.ArrayList;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.json.Json;

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

import java.net.URL;

import javax.xml.namespace.QName;

import org.icatproject.ICAT;
import org.icatproject.ICATService;
import org.icatproject.IcatExceptionType;
import org.icatproject.DataCollection;
import org.icatproject.Datafile;
import org.icatproject.Dataset;
import org.icatproject.Investigation;
import org.icatproject.DataCollectionDatafile;
import org.icatproject.DataCollectionDataset;

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

    @POST
    @Path("/makeDataPublic")
    @Produces({MediaType.APPLICATION_JSON})
    public Response makeDataPublic(
            @FormParam("icatUrl") String icatUrl,
            @FormParam("sessionId") String sessionId,
            @FormParam("investigationIds") String investigationIds,
            @FormParam("datasetIds") String datasetIds,
            @FormParam("datafileIds") String datafileIds) {

        try {
            List<Long> investigationIdList = parseIds(investigationIds);
            List<Long> datasetIdList = parseIds(datasetIds);
            List<Long> datafileIdList = parseIds(datafileIds);

            DataCollection dataCollection = createDataCollection(icatUrl, sessionId, datasetIdList, datafileIdList);
            setEntityDoi(icatUrl, sessionId, "DataCollection", dataCollection.getId(), "10.5286/topcat/TEST/DataCollection/" + dataCollection.getId());

            return Response.ok().entity("\"ok\"").build();
        } catch(Exception e){
            return Response.status(400).entity(Json.createObjectBuilder().add("message", e.toString()).build().toString()).build();
        }
    }

    private List<Long> parseIds(String ids){
        List<Long> out = new ArrayList<Long>();

        if(ids != null && !ids.equals("")){
            for (String id : ids.split("\\s*,\\s*")) {
                out.add(Long.valueOf(id));
            }
        }

        return out;
    }

    private void setEntityDoi(String icatUrl, String sessionId, String entityType, Long entityId, String doi) throws Exception{
        ICAT icat = createIcat(icatUrl);

        if(entityType.equals("Investigation")){
            Investigation investigation = (Investigation) icat.get(sessionId, "Investigation", entityId);
            investigation.setDoi(doi);
            icat.update(sessionId, investigation);
        } else if(entityType.equals("Dataset")){
            Dataset dataset = (Dataset) icat.get(sessionId, "Dataset", entityId);
            dataset.setDoi(doi);
            icat.update(sessionId, dataset);
        } else if(entityType.equals("Datafile")){
            Datafile datafile = (Datafile) icat.get(sessionId, "Datafile", entityId);
            datafile.setDoi(doi);
            icat.update(sessionId, datafile);
        } else if(entityType.equals("DataCollection")){
            DataCollection dataCollection = (DataCollection) icat.get(sessionId, "DataCollection", entityId);
            dataCollection.setDoi(doi);
            icat.update(sessionId, dataCollection);
        }
    }

    private DataCollection createDataCollection(String icatUrl, String sessionId, List<Long> datasetIds, List<Long> datafileIds) throws Exception {
        ICAT icat = createIcat(icatUrl);

        DataCollection dataCollection = new DataCollection();
        dataCollection.setId(icat.create(sessionId, dataCollection));

        for(Long datasetId : datasetIds){
            Dataset dataset = (Dataset) icat.get(sessionId, "Dataset", datasetId);
            DataCollectionDataset dataCollectionDataset = new DataCollectionDataset();
            dataCollectionDataset.setDataCollection(dataCollection);
            dataCollectionDataset.setDataset(dataset);
            icat.create(sessionId, dataCollectionDataset);
        }

        for(Long datafileId : datafileIds){
            Datafile datafile = (Datafile) icat.get(sessionId, "Datafile", datafileId);
            DataCollectionDatafile dataCollectionDatafile = new DataCollectionDatafile();
            dataCollectionDatafile.setDataCollection(dataCollection);
            dataCollectionDatafile.setDatafile(datafile);
            icat.create(sessionId, dataCollectionDatafile);
        }

        return dataCollection;
    }

    private ICAT createIcat(String icatUrl) throws Exception {
        URL icatServiceUrl = new URL(icatUrl + "/ICATService/ICAT?wsdl");
        ICATService icatService = new ICATService(icatServiceUrl, new QName("http://icatproject.org", "ICATService"));
        return icatService.getICATPort();
    }

    private void createDoi() throws Exception {
        String seedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resource xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://datacite.org/schema/kernel-4\" xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd\"></resource>";

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

        dataCiteClient.mintDoi("10.5286/topcat/sciency-stuff", "http://www.scd.stfc.ac.uk/SCD/organisation/42436.aspx");

    }
    
}
