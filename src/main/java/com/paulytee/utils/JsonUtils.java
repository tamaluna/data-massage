package com.paulytee.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * @author Paul Tamalunas
 * @version $Id: JSONUtils.java 254 2013-11-09 22:07:12Z lium $
 */
public final class JsonUtils {
//	public static final String DEFAULT_ERROR_CODE = "0999";
//	public static final String SOCKET_ERROR_CODE = "0081";
//	public static final String BLANK_FILE_ERROR_CODE = "0119";
//	public static final String BAD_JSON_ERROR_CODE = "0197";
//	public static final String SAVE_RESPONSE_ERROR_CODE = "0113";
//	private static final Logger LOGGER = Logger.getLogger(JSONUtils.class);

	private JsonUtils() { }


	public static JsonNode asJsonNode(String s) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readTree(s);
	}

	public static JsonNode objectAsJsonNode(Object o) throws IOException {
		return asJsonNode(asJsonString(o));
	}

	public static String asJsonString(Object o) throws IOException {
		return new ObjectMapper()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.writeValueAsString(o);
	}

	/*
	 * convert <code >jsonString</code> into a {@link JSONObject} object.  This method will convert exception
	 * condition into a {@link JSONObject} using {@link #newExceptionJSONObject(Exception)}.
	 */
	//public static JSONObject toJSONObject(String jsonString) {
	//	try {
	//		return new JSONObject(jsonString);
	//	} catch (JSONException e) {
	//		//LOGGER.error("Error converting string to JSON object: " + e.getMessage(), e);
	//		//LOGGER.error("JSON string in error is as follows:\n" + jsonString);
	//		return newExceptionJSONObject(e);
	//	}
	//}

	//public static JSONObject newExceptionJSONObject(Exception e) {
	//	try {
	//		JSONObject json = new JSONObject();
	//		json.put("errorCode", "TPA000");
	//		json.put("errorMessage", e.getMessage());
	//		json.put("responseTimestamp", System.currentTimeMillis());
	//		return json;
	//	} catch (JSONException e1) {
	//		// very unlikely since we control all the JSON properties here.
	//		return null;
	//	}
	//}

	//public static <T> T jsonNodeToPojo(JsonNode jsonNode, Class<T> clas) {
	//	try {
	//		return new ObjectMapper().treeToValue(jsonNode, clas);
	//	} catch (JsonProcessingException e) {
	//		e.printStackTrace();
	//	}
	//	return null;
	//}

}
