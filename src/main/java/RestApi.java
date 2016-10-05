
package org.icatproject.topcatdoiplugin;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;


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
import javax.ws.rs.PathParam;
import javax.json.Json;
import javax.json.JsonObjectBuilder;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.icatproject.topcatdoiplugin.Properties;
import org.icatproject.topcatdoiplugin.topcatclient.TopcatClient;
import org.icatproject.topcatdoiplugin.topcatclient.TopcatClientException;
import org.icatproject.topcatdoiplugin.dataciteclient.DataCiteClient;
import org.icatproject.topcatdoiplugin.dataciteclient.DataCiteClientException;
import org.icatproject.topcatdoiplugin.DoiDownload;

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
import org.icatproject.DataCollectionParameter;
import org.icatproject.ParameterType;
import org.icatproject.Datafile;
import org.icatproject.Dataset;
import org.icatproject.Investigation;
import org.icatproject.DataCollectionDatafile;
import org.icatproject.DataCollectionDataset;
import org.icatproject.Login.Credentials;
import org.icatproject.Login.Credentials.Entry;

import org.icatproject.ids.client.DataSelection;
import org.icatproject.ids.client.IdsClient;
import org.icatproject.ids.client.IdsException;
import org.icatproject.ids.client.IdsClient.Flag;
import org.icatproject.ids.client.IdsClient.Status;

/**
 *
 * @author elz24996
 */
@Stateless
@LocalBean
@Path("")
public class RestApi {
    
    private static final Logger logger = LoggerFactory.getLogger(RestApi.class);

    @PersistenceContext(unitName = "topcat")
    EntityManager em;
    
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
    @Path("/makePublicDataCollection")
    @Produces({MediaType.APPLICATION_JSON})
    public Response makePublicDataCollection(
            @FormParam("icatUrl") String icatUrl,
            @FormParam("sessionId") String sessionId,
            @FormParam("datasetIds") String datasetIds,
            @FormParam("datafileIds") String datafileIds,
            @FormParam("title") String title,
            @FormParam("releaseDate") String releaseDate) {

        try {
            List<Long> datasetIdList = parseIds(datasetIds);
            List<Long> datafileIdList = parseIds(datafileIds);

            DataCollection dataCollection = createDataCollection(icatUrl, sessionId, title, new Date(), datasetIdList, datafileIdList);
            String doi = generateEntityDoi("DataCollection", dataCollection.getId());
            setEntityDoi(icatUrl, sessionId, "DataCollection", dataCollection.getId(), doi);

            return Response.ok().entity(Json.createObjectBuilder().add("id", dataCollection.getId()).add("doi", doi).build().toString()).build();
        } catch(Exception e){
            return Response.status(400).entity(Json.createObjectBuilder().add("message", e.toString()).build().toString()).build();
        }
    }

    @GET
    @Path("/landingPageInfo/{dataCollectionId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getLandingPageInfo(
        @PathParam("dataCollectionId") Long dataCollectionId)  throws Exception {

        try {
            Properties properties = Properties.getInstance();
            String readerIcatUrl = properties.getProperty("readerIcatUrl");
            String readerSessionId = readerSessionId();
            ICAT icat = createIcat(readerIcatUrl);
            DataCollection dataCollection = (DataCollection) icat.get(readerSessionId, "DataCollection", dataCollectionId);
            String title = ((DataCollectionParameter) icat.search(readerSessionId, "select dataCollectionParameter from DataCollectionParameter dataCollectionParameter where dataCollectionParameter.type.name = 'title' and dataCollectionParameter.dataCollection.id = '" + dataCollectionId + "'").get(0)).getStringValue();
            Date releaseDate = ((DataCollectionParameter) icat.search(readerSessionId, "select dataCollectionParameter from DataCollectionParameter dataCollectionParameter where dataCollectionParameter.type.name = 'releaseDate' and dataCollectionParameter.dataCollection.id = '" + dataCollectionId + "'").get(0)).getDateTimeValue().toGregorianCalendar().getTime();

            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("title", title);
            jsonObjectBuilder.add("releaseDate", releaseDate.toString());
            
            return Response.status(200).entity(jsonObjectBuilder.build().toString()).build();
        } catch(Exception e){
            return Response.status(400).entity(Json.createObjectBuilder().add("message", e.toString()).build().toString()).build();
        }
    }

    @GET
    @Path("/redirectToLandingPage/{dataCollectionId}")
    @Produces({MediaType.TEXT_HTML})
    public Response redirectToLandingPage(
        @PathParam("dataCollectionId") Long dataCollectionId)  throws Exception {

        String facilityName = dataCollectionToFacilityName(getDataCollection(dataCollectionId));

        StringBuilder html = new StringBuilder();

        html.append("<script>");
        html.append("window.location = '/#/doi-landing-page/" + facilityName + "/DataCollection/" + dataCollectionId + "';");
        html.append("</script>");

        return Response.status(200).entity(html.toString()).build();
    }

    @GET
    @Path("/status/{dataCollectionId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getStatus(
        @PathParam("dataCollectionId") Long dataCollectionId) throws Exception {

        try {
            DataSelection dataSelection = dataCollectionToDataSelection(getDataCollection(dataCollectionId));
            Properties properties = Properties.getInstance();
            URL readerIdsUrl = new URL(properties.getProperty("readerIdsUrl"));
            IdsClient idsClient = new IdsClient(readerIdsUrl);
            Status status = null;
            String readerSessionId = readerSessionId();

            for(DataSelection currentDataSelection : chunkDataSelection(dataSelection)){
                Status currentStatus = idsClient.getStatus(readerSessionId, currentDataSelection);
                if(currentStatus == Status.ARCHIVED){
                    status = Status.ARCHIVED;
                    break;
                } else if(currentStatus == Status.RESTORING){
                    status = Status.RESTORING;
                } else if(status != Status.RESTORING){
                    status = Status.ONLINE;
                }
            }
            
            return Response.status(200).entity("\"" + status.name() + "\"").build();
        } catch(Exception e){
            return Response.status(400).entity(Json.createObjectBuilder().add("message", e.toString()).build().toString()).build();
        }
    }

    @POST
    @Path("/prepareData/{dataCollectionId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response prepareData(
        @PathParam("dataCollectionId") Long dataCollectionId,
        @FormParam("fileName") String fileName,
        @FormParam("email") String email) throws Exception {

        try {
            DataSelection dataSelection = dataCollectionToDataSelection(getDataCollection(dataCollectionId));
            Properties properties = Properties.getInstance();
            URL readerIdsUrl = new URL(properties.getProperty("readerIdsUrl"));
            IdsClient idsClient = new IdsClient(readerIdsUrl);
            String preparedId = idsClient.prepareData(readerSessionId(), dataSelection, Flag.ZIP);

            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("preparedId", preparedId);

            String downloadUrl = readerIdsUrl.toString();
            downloadUrl += "/ids/getData?preparedId=" + preparedId;
            downloadUrl += "&outname=" + fileName;
            jsonObjectBuilder.add("downloadUrl", downloadUrl);

            if(email != null && email.length() > 0){
                DoiDownload doiDownload = new DoiDownload();
                doiDownload.setTransportUrl(readerIdsUrl.toString());
                doiDownload.setPreparedId(preparedId);
                doiDownload.setFileName(fileName);
                doiDownload.setEmail(email);
                em.persist(doiDownload);
                em.flush();
            }

            return Response.status(200).entity(jsonObjectBuilder.build().toString()).build();
        } catch(Exception e){
            return Response.status(400).entity(Json.createObjectBuilder().add("message", e.toString()).build().toString()).build();
        }
    }

    private DataSelection dataCollectionToDataSelection(DataCollection dataCollection) {
        DataSelection dataSelection = new DataSelection();

        for(DataCollectionDataset dataCollectionDataset : dataCollection.getDataCollectionDatasets()){
            dataSelection.addDataset(dataCollectionDataset.getDataset().getId());
        }

        for(DataCollectionDatafile dataCollectionDatafile : dataCollection.getDataCollectionDatafiles()){
            dataSelection.addDatafile(dataCollectionDatafile.getDatafile().getId());
        }

        return dataSelection;
    }

    private String dataCollectionToFacilityName(DataCollection dataCollection) throws Exception {
        if(dataCollection.getDataCollectionDatafiles().size() > 0){
            return dataCollection.getDataCollectionDatafiles().get(0).getDatafile().getDataset().getInvestigation().getFacility().getName();
        } else {
            return dataCollection.getDataCollectionDatasets().get(0).getDataset().getInvestigation().getFacility().getName();
        } 
    }

    private DataCollection getDataCollection(Long id) throws Exception {
        Properties properties = Properties.getInstance();
        String readerIcatUrl = properties.getProperty("readerIcatUrl");
        String readerSessionId = readerSessionId();
        ICAT icat = createIcat(readerIcatUrl);
        return (DataCollection) icat.get(readerSessionId, "DataCollection dataCollection include dataCollection.dataCollectionDatafiles.datafile.dataset.investigation.facility, dataCollection.dataCollectionDatasets.dataset.investigation.facility", id);
    }

    private List<DataSelection> chunkDataSelection(DataSelection dataSelection){
        List<DataSelection> out = new ArrayList<DataSelection>();
        Map<String, String> parameters = dataSelection.getParameters();
        List<Long> datasetIds = new ArrayList<Long>();
        List<Long> datafileIds = new ArrayList<Long>();

        if(parameters.get("datasetIds") != null){
            for(String id : parameters.get("datasetIds").split(",")){
                datasetIds.add(Long.valueOf(id));
            }
        }
        if(parameters.get("datafileIds") != null){
            for(String id : parameters.get("datafileIds").split(",")){
                datafileIds.add(Long.valueOf(id));
            }
        }

        while(!datasetIds.isEmpty() || !datafileIds.isEmpty()){
            DataSelection currentDataSelection = new DataSelection();

            while(!datasetIds.isEmpty()){
                if(dataSelectionSize(currentDataSelection) >= 900){
                    break;
                }
                currentDataSelection.addDataset(datasetIds.remove(0));
            }

            while(!datafileIds.isEmpty()){
                if(dataSelectionSize(currentDataSelection) >= 900){
                    break;
                }
                currentDataSelection.addDatafile(datafileIds.remove(0));
            }

            out.add(currentDataSelection);
        }

        return out;
    }

    private int dataSelectionSize(DataSelection dataSelection){
        Map<String, String> parameters = dataSelection.getParameters();
        int out = 0;
        if(parameters.get("datasetIds") != null){
            out += parameters.get("datasetIds").length();
        }
        if(parameters.get("datafileIds") != null){
            out += parameters.get("datafileIds").length();
        }
        return out; 
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

    private void setEntityDoi(String icatUrl, String sessionId, String entityType, Long entityId, String doi) throws Exception {
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

    private DataCollection createDataCollection(String icatUrl, String sessionId, String title, Date releaseDate,  List<Long> datasetIds, List<Long> datafileIds) throws Exception {
        ICAT icat = createIcat(icatUrl);

        DataCollection dataCollection = new DataCollection();
        dataCollection.setId(icat.create(sessionId, dataCollection));

        ParameterType titleParameterType = getParameterType(icatUrl, sessionId, "title");
        ParameterType releaseDateParameterType = getParameterType(icatUrl, sessionId, "releaseDate");

        DataCollectionParameter titleDataCollectionParmeter = new DataCollectionParameter();
        titleDataCollectionParmeter.setDataCollection(dataCollection);
        titleDataCollectionParmeter.setType(titleParameterType);
        titleDataCollectionParmeter.setStringValue(title);
        icat.create(sessionId, titleDataCollectionParmeter);

        DataCollectionParameter releaseDateDataCollectionParmeter = new DataCollectionParameter();
        releaseDateDataCollectionParmeter.setDataCollection(dataCollection);
        releaseDateDataCollectionParmeter.setType(releaseDateParameterType);
        releaseDateDataCollectionParmeter.setDateTimeValue(dateToXMLGregorianCalendar(releaseDate));
        icat.create(sessionId, releaseDateDataCollectionParmeter);

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

    private ParameterType getParameterType(String icatUrl, String sessionId, String name) throws Exception {
        return (ParameterType) createIcat(icatUrl).search(sessionId, "select parameterType from ParameterType parameterType where parameterType.name = '" + name + "'").get(0);
    }

    private XMLGregorianCalendar dateToXMLGregorianCalendar(Date date) throws Exception {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
    }

    private ICAT createIcat(String icatUrl) throws Exception {
        URL icatServiceUrl = new URL(icatUrl + "/ICATService/ICAT?wsdl");
        ICATService icatService = new ICATService(icatServiceUrl, new QName("http://icatproject.org", "ICATService"));
        return icatService.getICATPort();
    }

    private String readerSessionId() throws Exception {
        Properties properties = Properties.getInstance();
        String readerIcatUrl = properties.getProperty("readerIcatUrl");
        String readerAuthenticationPlugin = properties.getProperty("readerAuthenticationPlugin");
        String readerUsername = properties.getProperty("readerUsername");
        String readerPassword = properties.getProperty("readerPassword");

        ICAT icat = createIcat(readerIcatUrl);

        Credentials credentials = new Credentials();
        List<Entry> entries = credentials.getEntry();
        Entry entry;

        entry = new Entry();
        entry.setKey("username");
        entry.setValue(readerUsername);
        entries.add(entry);
        entry = new Entry();
        entry.setKey("password");
        entry.setValue(readerPassword);
        entries.add(entry);

        return icat.login(readerAuthenticationPlugin, credentials);
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
        resourceTypeElement.setAttribute("resourceTypeGeneral", resourceTypeGeneral);
        resourceTypeElement.appendChild(document.createTextNode(resourceType));
        rootElement.appendChild(resourceTypeElement);

        DataCiteClient dataCiteClient = new DataCiteClient();

        dataCiteClient.setDoiMetadata(document);

        dataCiteClient.mintDoi(doi, landingPageUrl);

    }
    
}
