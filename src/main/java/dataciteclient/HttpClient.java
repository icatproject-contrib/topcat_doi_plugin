package org.icatproject.topcatdoiplugin.dataciteclient;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonValue;

import java.net.URL;
import java.net.URLEncoder;
import javax.net.ssl.HttpsURLConnection;

import java.io.*;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HttpClient {

	private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

	private String url;

	public HttpClient(String url){
		this.url = url;
	}

	public String get(String offset, Map<String, String> headers) throws DataCiteClientException {
		return send("GET", offset, headers);
	}

	public String post(String offset, Map<String, String> headers, String data) throws DataCiteClientException {
		return send("POST", offset, headers, data);
	}

	public String delete(String offset, Map<String, String> headers) throws DataCiteClientException {
		return send("DELETE", offset, headers);
	}


	private String send(String method, String offset, Map<String, String> headers, String body) throws DataCiteClientException {
		StringBuilder url = new StringBuilder(this.url + "/" + offset);

		HttpsURLConnection connection = null;

		try {
		    //Create connection
		    connection = (HttpsURLConnection) (new URL(url.toString())).openConnection();
		    connection.setRequestMethod(method);
    		connection.setUseCaches(false);
    		connection.setDoInput(true);

    		for(Map.Entry<String, String> entry : headers.entrySet()) {
				connection.setRequestProperty(entry.getKey(), entry.getValue());
			}

    		if(body != null && (method.equals("POST") || method.equals("PUT"))){
    			connection.setDoOutput(true);
    			connection.setRequestProperty("Content-Length", Integer.toString(body.toString().getBytes().length));

	    		DataOutputStream request = new DataOutputStream(connection.getOutputStream());
	    		request.writeBytes(body.toString());
	    		request.close();
	    	}

		    return inputStreamToString(connection.getInputStream());
    	} catch (Exception e) {
    		if(connection != null){
	    		try {
	    			throw new DataCiteClientException(inputStreamToString(connection.getErrorStream()));
	    		} catch(Exception e2){
	    			throw new DataCiteClientException(e2.getMessage());
	    		}
	    	} else {
	    		throw new DataCiteClientException(e.getMessage());
	    	}
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private String send(String method, String offset, Map<String, String> headers) throws DataCiteClientException {
		return send(method, offset, headers, null);
	}

	private String inputStreamToString(InputStream inputStream) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	    StringBuilder out = new StringBuilder();
	    int currentChar;
	    while ((currentChar = bufferedReader.read()) > -1) {
	    	out.append(Character.toChars(currentChar));
	    }
	    bufferedReader.close();
	    return out.toString();
	}


}