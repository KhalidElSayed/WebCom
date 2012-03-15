package com.feigdev.webcom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.NameValuePair;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
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
	private int requestType;
	private boolean isSecure = false;
	private String contentType;
	private HashMap<String, Object> parameters;
	private MultipartEntity params2;
	private SimpleResponse response;
	public static final int RESPONSE = 1001;
	private WebComListener listener;
	private String username;
	private ArrayList<NameValuePair> headParams;
	public static final int GET = 12313;
	public static final int POST = 12314;
	public static final int POST_AUTH = 12315;
	public static final int POST_FILE = 12316;
	
	public String getUsername() {
		return username;
	}


	public void setUsername(String username) {
		this.username = username;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}

	private String password;
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
		requestType = GET;
		contentType = "text/html";
		cookies = null;
		setParams2(new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE));
		response = new SimpleResponse();
		response.setUrl(url);
		response.setId(0);
		response.setContentType(contentType);
		response.setStatus(SimpleResponse.NOTEXECUTED);
		this.listener = listener;
		parameters = new HashMap<String,Object>();
		headParams = new ArrayList<NameValuePair>();
		
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
		requestType = GET;
		contentType = "text/html";
		cookies = null;
		response = new SimpleResponse();
		response.setId(id);
		response.setUrl(url);
		response.setContentType(contentType);
		response.setStatus(SimpleResponse.NOTEXECUTED);
		this.listener = listener;
		parameters = new HashMap<String,Object>();
		headParams = new ArrayList<NameValuePair>();
		setParams2(new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE));
	}
	
	/**
	 * Run the interaction in the background
	 */
	public void interact(){
    	final HttpController httpRequest = new HttpController(); 
    	HashMap<String, Object> parameters = this.getParameters();
    	Iterator<String> it = parameters.keySet().iterator();
    	
    	String key = "";
    	
    	final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();

    	while(it.hasNext()){
    		key = (String) it.next();
    		params.add(new BasicNameValuePair(key, (String)parameters.get(key)));
    	}
    	
    	if(Constants.VERBOSE){ Log.i(Constants.TAG,"interact("+ "" + ")");	}
    	
        Runnable runnable = new Runnable() {
            public void run() {
            	switch (requestType){
            	case GET:
            		response = httpRequest.get(getUrl(),contentType,response.getId(),cookies,headParams);
            		break;
            	case POST:
            		response = httpRequest.post(getUrl(),params,contentType,response.getId(),cookies,headParams);
            		break;
            	case POST_AUTH:
            		response = httpRequest.postAuth(getUrl(),params,username,password,contentType,response.getId(),cookies,headParams);
            		break;
            	case POST_FILE:
            		response = httpRequest.postFile(getUrl(),params2,contentType,response.getId(),cookies,headParams);
            		break;
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
	public int getRequestType() {
		return requestType;
	}
	/**
	 * 
	 * @param requestType either get or set
	 */
	public void setRequestType(int requestType) {
		this.requestType = requestType;
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

    public void addHeadParam(String name, String value){
    	headParams.add(new BasicNameValuePair(name,value));
    }

	public ArrayList<NameValuePair> getHeadParams() {
		return headParams;
	}


	public void setHeadParams(ArrayList<NameValuePair> headParams) {
		this.headParams = headParams;
	}


	public MultipartEntity getParams2() {
		return params2;
	}


	public void setParams2(MultipartEntity params2) {
		this.params2 = params2;
	}
	
	public void addMParam(String name, ContentBody value){
		this.params2.addPart(name, value);
	}


}
