
package org.icatproject.topcatdoiplugin.dataciteclient;

import java.util.Map;
import java.util.HashMap;

import java.util.Base64;

import org.w3c.dom.Document;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.parsers.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.icatproject.topcatdoiplugin.Properties;

public class DataCiteClient {

	private static final Logger logger = LoggerFactory.getLogger(DataCiteClient.class);

	private HttpClient httpClient;

	public DataCiteClient(){
		Properties properties = Properties.getInstance();
		String dataCiteUrl = properties.getProperty("dataCiteUrl");
        this.httpClient = new HttpClient(dataCiteUrl);
	}

	public void setDoiMetadata(Document document) throws DataCiteClientException {
		Map headers = createAuthorizationHeaders();
		headers.put("Content-Type", "application/xml;charset=UTF-8");
		httpClient.post("metadata", headers, documentToString(document));
	}

	public Document getDoiMetadata(String doi) throws DataCiteClientException {
		try {
			Map headers = createAuthorizationHeaders();
			headers.put("Accept", "application/xml");
			String xml = httpClient.get("metadata/" + doi, headers);
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        return builder.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
	    } catch(DataCiteClientException e){
	    	throw e;
	    } catch(Exception e){
	    	throw new DataCiteClientException(e.getMessage());
	    }
	}

	public void mintDoi(String doi, String landingPageUrl) throws DataCiteClientException {
		Map headers = createAuthorizationHeaders();
		headers.put("Content-Type", "text/plain;charset=UTF-8");
		httpClient.post("doi", headers, "doi=" + doi + "\nurl=" + landingPageUrl);
	}

	private Map<String, String> createAuthorizationHeaders(){
		Map out = new HashMap();

		Properties properties = Properties.getInstance();
		String dataCiteUsername = properties.getProperty("dataCiteUsername");
		String dataCitePassword = properties.getProperty("dataCitePassword");

		String credentials = dataCiteUsername + ":" + dataCitePassword;
		String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
		out.put("Authorization", "Basic " + encodedCredentials);

		return out;
	}

	private String documentToString(Document document) throws DataCiteClientException {
	    try {
	       DOMSource domSource = new DOMSource(document);
	       StringWriter writer = new StringWriter();
	       StreamResult result = new StreamResult(writer);
	       TransformerFactory transformerFactory = TransformerFactory.newInstance();
	       Transformer transformer = transformerFactory.newTransformer();
	       transformer.transform(domSource, result);
	       return writer.toString();
	    } catch(TransformerException ex) {
	       throw new DataCiteClientException(ex.getMessage());
	    }
	}

}
