
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
            String doi = generateEntityDoi("DataCollection", dataCollection.getId());
            setEntityDoi(icatUrl, sessionId, "DataCollection", dataCollection.getId(), doi);

            
            
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

    private String generateEntityDoi(String entityType, Long entityId){
        Properties properties = new Properties();
        String doiNamespace = properties.getProperty("doiNamespace");
        return doiNamespace + "/" + entityType + "/" + entityId;

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

    private void createDoi(
        String doi,
        List<String> creatorNames,
        List<String> titles,
        String publisher,
        int publicationYear,
        String resourceTypeGeneral,
        String resourceType,
        String landingPageUrl) throws Exception {

        String seedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><resource xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://datacite.org/schema/kernel-4\" xsi:schemaLocation=\"http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd\"></resource>";

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
    
        Document document = builder.parse(new ByteArrayInputStream(seedXml.getBytes("UTF-8")));
        
        Element rootElement = document.getDocumentElement();
        
        Element identifierElement = document.createElement("identifier");
        identifierElement.setAttribute("identifierType", "DOI");
        identifierElement.appendChild(document.createTextNode(doi));
        rootElement.appendChild(identifierElement);

        Element creatorsElement = document.createElement("creators");
        rootElement.appendChild(creatorsElement);

        for(String creatorName : creatorNames){
            Element creatorElement = document.createElement("creator");
            creatorsElement.appendChild(creatorElement);

            Element creatorNameElement = document.createElement("creatorName");
            creatorNameElement.appendChild(document.createTextNode(creatorName));
            creatorElement.appendChild(creatorNameElement);
        }

        for(String title : titles){
            Element titlesElement = document.createElement("titles");
            rootElement.appendChild(titlesElement);

            Element titleElement = document.createElement("title");
            titleElement.setAttribute("xml:lang", "en-gb");
            titleElement.appendChild(document.createTextNode(title));
            titleElement.appendChild(titleElement);
        }

        Element publisherElement = document.createElement("publisher");
        publisherElement.appendChild(document.createTextNode(publisher));
        rootElement.appendChild(publisherElement);

        Element publicationYearElement = document.createElement("publicationYear");
        publicationYearElement.appendChild(document.createTextNode(Integer.toString(publicationYear)));
        rootElement.appendChild(publicationYearElement);

        Element resourceTypeElement = document.createElement("resourceType");
        resourceTypeElement.setAttribute("resourceTypeGeneral", "Dataset");
        resourceTypeElement.appendChild(document.createTextNode("Serious Research"));
        rootElement.appendChild(resourceTypeElement);

        DataCiteClient dataCiteClient = new DataCiteClient();

        dataCiteClient.setDoiMetadata(document);

        dataCiteClient.mintDoi(doi, landingPageUrl);

    }
    
}
