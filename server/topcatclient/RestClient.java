package org.icatproject.topcatdoiplugin.topcatclient;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonValue;

import java.net.URL;
import java.net.URLEncoder;
import javax.net.ssl.HttpsURLConnection;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class RestClient {

	private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

	private String url;

	public RestClient(String url){
		this.url = url;
	}

	public JsonValue get(String offset, Map<String, String> params) throws TopcatClientException {
		return send("GET", offset, params);
	}

	public JsonValue get(String offset) throws TopcatClientException {
		return send("GET", offset, null);
	}

	public JsonValue post(String offset, Map<String, String> params) throws TopcatClientException {
		return send("POST", offset, params);
	}

	public JsonValue post(String offset) throws TopcatClientException {
		return send("POST", offset, null);
	}

	public JsonValue put(String offset, Map<String, String> params) throws TopcatClientException {
		return send("PUT", offset, params);
	}

	public JsonValue put(String offset) throws TopcatClientException {
		return send("PUT", offset, null);
	}

	public JsonValue delete(String offset, Map<String, String> params) throws TopcatClientException {
		return send("DELETE", offset, params);
	}

	public JsonValue delete(String offset) throws TopcatClientException {
		return send("DELETE", offset, null);
	}

	private JsonValue send(String method, String offset, Map<String, String> params) throws TopcatClientException {
		StringBuilder url = new StringBuilder(this.url + "/" + offset);
		StringBuilder encodedParams = new StringBuilder();

		if(params != null){
			for(Map.Entry<String, String> entry : params.entrySet()) {
				if(encodedParams.length() > 0){
					encodedParams.append("&");
				}
				encodedParams.append(URLEncoder.encode(entry.getKey()) + "=" + URLEncoder.encode(entry.getValue()));
			}
		}

		if((method.equals("GET") || method.equals("DELETE")) && encodedParams.length() > 0){
			url.append("?");
			url.append(encodedParams);
		}

		HttpsURLConnection connection = null;

		try {
		    //Create connection
		    connection = (HttpsURLConnection) (new URL(url.toString())).openConnection();
		    connection.setRequestMethod(method);
    		connection.setUseCaches(false);
    		connection.setDoInput(true);

    		if(method.equals("POST") || method.equals("PUT")){
    			connection.setDoOutput(true);
    			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    			connection.setRequestProperty("Content-Length", Integer.toString(encodedParams.toString().getBytes().length));

	    		DataOutputStream request = new DataOutputStream(connection.getOutputStream());
	    		request.writeBytes(encodedParams.toString());
	    		request.close();
	    	}

		    BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		    StringBuilder out = new StringBuilder();
		    int currentChar;
		    while ((currentChar = response.read()) > -1) {
		    	out.append(Character.toChars(currentChar));
		    }
		    response.close();

		    return parseJson(out.toString());
    	} catch (Exception e) {
			throw new TopcatClientException(e.toString());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private JsonValue parseJson(String value){
		return (JsonValue) Json.createReader(new ByteArrayInputStream(("{\"value\":"+value+"}").getBytes(StandardCharsets.UTF_8))).readObject().get("value");
	}

}