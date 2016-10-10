
package org.icatproject.topcatdoiplugin;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.text.SimpleDateFormat;


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
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathConstants;

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
import org.icatproject.User;
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
    public Response makePublicDataCollection(@FormParam("json") String json) {

        try {

            InputStream jsonInputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            JsonReader jsonReader = Json.createReader(jsonInputStream);
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();

            String icatUrl = jsonObject.getString("icatUrl");

            String sessionId = jsonObject.getString("sessionId");

            String title = jsonObject.getString("title");

            String description = jsonObject.getString("description");

            List<String> creators = new ArrayList<String>();
            for(JsonValue creator : jsonObject.getJsonArray("creators")){
                creators.add(((JsonString) creator).toString());
            }

            List<Long> datasetIds = new ArrayList<Long>();
            for(JsonValue datasetId : jsonObject.getJsonArray("datasetIds")){
                datasetIds.add(((JsonNumber) datasetId).longValue());
            }

            List<Long> datafileIds = new ArrayList<Long>();
            for(JsonValue datafileId : jsonObject.getJsonArray("datafileIds")){
                datafileIds.add(((JsonNumber) datafileId).longValue());
            }

            Date releaseDate = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(jsonObject.getString("releaseDate"));

            String publisher = "Lorum Ipsum Light Source";

            int publicationYear = 2016;

            String resourceTypeGeneral = "Dataset";

            String resourceType = "Experiment Data";

            DataCollection dataCollection = createDataCollection(icatUrl, sessionId, title, releaseDate, datasetIds, datafileIds);
            String doi = generateEntityDoi("DataCollection", dataCollection.getId());
            setEntityDoi(icatUrl, sessionId, "DataCollection", dataCollection.getId(), doi);

            Properties properties = Properties.getInstance();
            String landingPageUrl = properties.getProperty("topcatUrl") + "/topcat_doi_plugin/api/redirectToLandingPage/" + dataCollection.getId();

            createDoi(doi, title, description, creators, releaseDate, publisher, publicationYear, resourceTypeGeneral, resourceType, landingPageUrl);

            return Response.ok().entity(Json.createObjectBuilder().add("id", dataCollection.getId()).add("doi", doi).build().toString()).build();
        } catch(DataCiteClientException e){
            return e.toResponse();
        } catch(Exception e){
            return Response.status(400).entity(Json.createObjectBuilder().add("message", e.toString()).build().toString()).build();
        }
    }

    @GET
    @Path("/metadata/{dataCollectionId}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getMetaData(
        @PathParam("dataCollectionId") Long dataCollectionId)  throws Exception {

        try {
            Properties properties = Properties.getInstance();
            String readerIcatUrl = properties.getProperty("readerIcatUrl");
            String readerSessionId = readerSessionId();
            ICAT icat = createIcat(readerIcatUrl);
            DataCollection dataCollection = (DataCollection) icat.get(readerSessionId, "DataCollection", dataCollectionId);
            DataCiteClient dataCiteClient = new DataCiteClient();
            Document document = dataCiteClient.getDoiMetadata(dataCollection.getDoi());
            XPath xPath =  XPathFactory.newInstance().newXPath();
            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

            jsonObjectBuilder.add("doi",  xPath.compile("resource/identifier").evaluate(document));

            jsonObjectBuilder.add("title",  xPath.compile("resource/titles/title").evaluate(document));

            jsonObjectBuilder.add("description",  xPath.compile("resource/descriptions/description").evaluate(document));
            
            jsonObjectBuilder.add("releaseDate",  xPath.compile("resource/dates/date").evaluate(document));

            jsonObjectBuilder.add("publisher",  xPath.compile("resource/publisher").evaluate(document));

            jsonObjectBuilder.add("publicationYear",  xPath.compile("resource/publicationYear").evaluate(document));
            
            JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
            NodeList creators = (NodeList) xPath.compile("resource/creators/creator").evaluate(document, XPathConstants.NODESET);
            for(int i = 0; i < creators.getLength(); i++){
                Node creator = creators.item(i);
                jsonArrayBuilder.add(xPath.compile("creatorName").evaluate(creator));
            }
            jsonObjectBuilder.add("creators",  jsonArrayBuilder.build());

            return Response.status(200).entity(jsonObjectBuilder.build().toString()).build();
        } catch(DataCiteClientException e){
            return e.toResponse();
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
            DataCollection dataCollection = getDataCollection(dataCollectionId);
            for(DataCollectionParameter parameter : dataCollection.getParameters()){
                Date now = new Date();
                if(parameter.getType().getName().equals("releaseDate") && now.before(parameter.getDateTimeValue().toGregorianCalendar().getTime())){
                    return Response.status(401).entity(Json.createObjectBuilder().add("message", "can't access data before release date").build().toString()).build();
                }
            }

            DataSelection dataSelection = dataCollectionToDataSelection(dataCollection);
            Properties properties = Properties.getInstance();
            URL readerIdsUrl = new URL(properties.getProperty("readerIdsUrl"));
            IdsClient idsClient = new IdsClient(readerIdsUrl);
            String preparedId = idsClient.prepareData(readerSessionId(), dataSelection, Flag.ZIP);

            JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
            jsonObjectBuilder.add("preparedId", preparedId);

            if(email != null && email.length() > 0){
                DoiDownload doiDownload = new DoiDownload();
                doiDownload.setTransportUrl(readerIdsUrl.toString());
                doiDownload.setPreparedId(preparedId);
                doiDownload.setFileName(fileName);
                doiDownload.setEmail(email);
                em.persist(doiDownload);
                em.flush();
            } else {
                String downloadUrl = readerIdsUrl.toString();
                downloadUrl += "/ids/getData?preparedId=" + preparedId;
                downloadUrl += "&outname=" + fileName;
                jsonObjectBuilder.add("downloadUrl", downloadUrl);
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
        return (DataCollection) icat.get(readerSessionId, "DataCollection dataCollection include dataCollection.dataCollectionDatafiles.datafile.dataset.investigation.facility, dataCollection.dataCollectionDatasets.dataset.investigation.facility, dataCollection.parameters.type", id);
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
        String title,
        String description,
        List<String> creators,
        Date releaseDate,
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


        Element titlesElement = document.createElement("titles");
        rootElement.appendChild(titlesElement);
        Element titleElement = document.createElement("title");
        titleElement.setAttribute("xml:lang", "en-gb");
        titleElement.appendChild(document.createTextNode(title));
        titlesElement.appendChild(titleElement);

        Element descriptionsElement = document.createElement("descriptions");
        rootElement.appendChild(descriptionsElement);
        Element descriptionElement = document.createElement("description");
        descriptionElement.setAttribute("xml:lang", "en-gb");
        descriptionElement.setAttribute("descriptionType", "Abstract");
        descriptionElement.appendChild(document.createTextNode(description));
        descriptionsElement.appendChild(descriptionElement);

        Element creatorsElement = document.createElement("creators");
        rootElement.appendChild(creatorsElement);

        for(String creator : creators){
            Element creatorElement = document.createElement("creator");
            creatorsElement.appendChild(creatorElement);

            Element creatorNameElement = document.createElement("creatorName");
            creatorNameElement.appendChild(document.createTextNode(creator));
            creatorElement.appendChild(creatorNameElement);
        }

        Element datesElement = document.createElement("dates");
        rootElement.appendChild(datesElement);
        Element dateElement = document.createElement("date");
        dateElement.setAttribute("dateType", "Available");
        dateElement.appendChild(document.createTextNode((new SimpleDateFormat("yyyy-MM-dd")).format(releaseDate)));
        datesElement.appendChild(dateElement);

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
