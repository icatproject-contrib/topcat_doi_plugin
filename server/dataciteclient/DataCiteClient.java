
package org.icatproject.topcatdoiplugin.dataciteclient;

import java.util.Map;
import java.util.HashMap;

import java.util.Base64;

import org.w3c.dom.Document;
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

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
		httpClient.post("metadata", createHeaders("application/xml;charset=UTF-8"), documentToString(document));
	}

	public void mintDoi(String doi, String landingPageUrl) throws DataCiteClientException {
		httpClient.post("metadata", createHeaders("text/plain;charset=UTF-8"), doi + "\n" + landingPageUrl);
	}

	private Map<String, String> createHeaders(String contentType){
		Map out = new HashMap();

		Properties properties = Properties.getInstance();
		String dataCiteUsername = properties.getProperty("dataCiteUsername");
		String dataCitePassword = properties.getProperty("dataCitePassword");

		String credentials = dataCiteUsername + ":" + dataCitePassword;
		String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
		out.put("Authorization", "Basic " + encodedCredentials);

		out.put("Content-Type", contentType);

		return out;
	}

	private String documentToString(Document document){
	    try {
	       DOMSource domSource = new DOMSource(document);
	       StringWriter writer = new StringWriter();
	       StreamResult result = new StreamResult(writer);
	       TransformerFactory transformerFactory = TransformerFactory.newInstance();
	       Transformer transformer = transformerFactory.newTransformer();
	       transformer.transform(domSource, result);
	       return writer.toString();
	    } catch(TransformerException ex) {
	       return null;
	    }
	}

}
