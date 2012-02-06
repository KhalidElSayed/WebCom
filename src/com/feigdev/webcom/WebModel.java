package com.feigdev.webcom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.NameValuePair;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;

import android.util.Log;

/**
 * This class builds a single web interaction. WebModel allows you to build an interaction, execute it and 
 * then receive the response asynchronously with a message handler.
 * 
 * Class members: url, requestType (get/put), contentType (text/html, text/json, etc.),
 * parameters (name - value hash), response (SimpleResponse), messageHandler (Handler to send messages to)
 *  
 * @author emil10001
 *
 */
public class WebModel {
	private String url;
	private String requestType;
	private boolean isSecure = false;
	private String contentType;
	private HashMap<String, Object> parameters;
	private SimpleResponse response;
	public static final int RESPONSE = 1001;
	private WebComListener listener;
	private BasicCookieStore cookies;
	
	/**
	 * Sets up the WebModel object
	 * 
	 * @param url full url to talk to
	 * @param handle Handler that will receive messages sent by this object
	 * 
	 * contentType is set to text/html and requestType is set
	 * to get by default
	 */
	public WebModel(String url, WebComListener listener){
		this.url = url;
		isSecure = false;
		requestType = "get";
		contentType = "text/html";
		cookies = null;
		response = new SimpleResponse();
		response.setUrl(url);
		response.setId(0);
		response.setContentType(contentType);
		response.setStatus(SimpleResponse.NOTEXECUTED);
		this.listener = listener;
		parameters = new HashMap<String,Object>();
	}
	
	
	/**
	 * Sets up the WebModel object
	 * 
	 * @param url full url to talk to
	 * @param handle Handler that will receive messages sent by this object
	 * 
	 * contentType is set to text/html and requestType is set
	 * to get by default
	 */
	public WebModel(String url, WebComListener listener, int id){
		this.url = url;
		isSecure = false;
		requestType = "get";
		contentType = "text/html";
		response = new SimpleResponse();
		response.setId(id);
		response.setUrl(url);
		response.setContentType(contentType);
		response.setStatus(SimpleResponse.NOTEXECUTED);
		this.listener = listener;
		parameters = new HashMap<String,Object>();
	}
	
	/**
	 * Run the interaction in the background
	 */
	public void interact(){
    	final HttpController httpRequest = new HttpController();    	
    	final String fixedUrl = this.getUrl();
    	final String contentType = this.getContentType();
    	final String requestType = this.getRequestType();
    	HashMap<String, Object> parameters = this.getParameters();
    	Iterator<String> it = parameters.keySet().iterator();
    	
    	String key = "";
    	
    	final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

    	while(it.hasNext()){
    		key = (String) it.next();
    		params.add(new BasicNameValuePair(key, (String)parameters.get(key)));
    	}
    	
    	if(Constants.VERBOSE){
    		Log.i(Constants.TAG,"interact("+ "" + ")");
    	}
    	
        Runnable runnable = new Runnable() {
            public void run() {
            	if (requestType.equals("get")){
            		response = httpRequest.get(fixedUrl,contentType,response.getId(),cookies);
            	}
            	else if (requestType.equals("post")){
            		response = httpRequest.post(fixedUrl,params,contentType,response.getId(),cookies);
            	}
            	if (listener != null){
            		listener.onResponse(response);
            	}
            }
        };
        
        // run on background thread.
        WebModel.performOnBackgroundThread(runnable);	
    }
	
	public void setCookies(BasicCookieStore cookies){
		this.cookies = cookies;
	}
	
	public BasicCookieStore getCookies(){
		return cookies;
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getRequestType() {
		return requestType;
	}
	/**
	 * 
	 * @param requestType either get or set
	 */
	public void setRequestType(String requestType) {
		this.requestType = requestType.toLowerCase();
	}
	public boolean isSecure() {
		return isSecure;
	}
	public void setSecure(boolean isSecure) {
		this.isSecure = isSecure;
	}
	public String getContentType() {
		return contentType;
	}
	/**
	 * 
	 * @param contentType a valid http content type like text/html or text/json
	 */
	public void setContentType(String contentType) {
		this.contentType = contentType.toLowerCase();
	}
	public HashMap<String, Object> getParameters() {
		return parameters;
	}
	
	public void addParameter(String key, Object value) {
		this.parameters.put(key,value);
	}


	public SimpleResponse getResponse() {
		return response;
	}

	public void setResponse(SimpleResponse response) {
		this.response = response;
	}
	
    /**
     * Executes the network requests on a separate thread.
     * 
     * @param runnable The runnable instance containing network mOperations to
     *        be executed.
     */
    private static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }

}
