
package org.icatproject.topcatdoiplugin.topcatclient;

import javax.json.JsonObject;

public class TopcatClient {

	private RestClient restClient;
	private String icatUrl;
	private String sessionId;

	public TopcatClient(String topcatUrl, String icatUrl, String sessionId){
        this.restClient = new RestClient(topcatUrl + "/topcat");
        this.icatUrl = icatUrl;
        this.sessionId = sessionId;
	}

	public TopcatClient(String topcatUrl){
		this(topcatUrl, null, null);
	}

	public String getVersion() throws TopcatClientException {
		return ((JsonObject) restClient.get("version")).getString("value");
	}

	public JsonObject getCart(String facilityName) throws TopcatClientException {
		return (JsonObject) restClient.get("user/cart/" + facilityName);
	}

	public JsonObject deleteCartItems(String facilityName)  throws TopcatClientException {
		return (JsonObject) restClient.delete("cart/" + facilityName + "/cartItems");
	}

}
