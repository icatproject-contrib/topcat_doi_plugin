package org.icatproject.topcatdoiplugin.topcatclient;

import java.util.Map;

import javax.json.Json;
import javax.json.JsonValue;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;

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

	private JsonValue send(String method, String offset, Map<String, String> params) throws TopcatClientException {
		StringBuilder url = new StringBuilder(this.url + "/" + offset);
		StringBuilder data = new StringBuilder();

		if(params != null){
			for(Map.Entry<String, String> entry : params.entrySet()) {
				if(data.length() > 0){
					data.append("&");
				}
				data.append(URLEncoder.encode(entry.getKey()) + "=" + URLEncoder.encode(entry.getValue()));
			}
		}

		if((method.equals("GET") || method.equals("DELETE")) && data.length() > 0){
			url.append("?");
			url.append(data);
		}

		logger.info(method + ": " + url.toString());

		HttpURLConnection connection = null;

		try {
		    //Create connection
		    connection = (HttpURLConnection) (new URL(url.toString())).openConnection();
		    connection.setRequestMethod(method);

		    if(method.equals("POST") || method.equals("PUT")){
    			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
    			connection.setRequestProperty("Content-Length", Integer.toString(data.toString().getBytes().length));
    		}

    		connection.setUseCaches(false);
    		connection.setDoOutput(true);

    		DataOutputStream request = new DataOutputStream(connection.getOutputStream());
    		if(method.equals("POST") || method.equals("PUT")){
    			request.writeBytes(data.toString());
    		}
    		request.close();

		    BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		    StringBuilder out = new StringBuilder();
		    int currentChar;
		    while ((currentChar = response.read()) > -1) {
		    	out.append(Character.toChars(currentChar));
		    }
		    response.close();

		    return parseJson(out.toString());
    	} catch (Exception e) {
			throw new TopcatClientException(e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private JsonValue parseJson(String value){
		return (JsonValue) Json.createReader(new ByteArrayInputStream(("{value:"+value+"}").getBytes(StandardCharsets.UTF_8))).readObject().get("value");
	}

}