package com.feigdev.webcom;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.util.Log;

import org.apache.http.impl.client.AbstractHttpClient;

/***
 * This class does the actual work of the HttpController.
 * It creates http clients and executes http requests.
 * 
 * @author emil10001
 *
 */
public class HttpController {
	public static final String TAG = "HttpController";
    private AbstractHttpClient mHttpClient;
    private ResponseHandler<String> responseHandler;
	public static final int TIMEOUT = 10000;

    
    public HttpController(){
    	if(Constants.VERBOSE){
    		Log.i(TAG,"Network Handler initialized");
    	}
    }
	
    /**
     * Configures the httpClient to connect to the URL provided.
     */
    public void maybeCreateHttpClient() {
        if (mHttpClient == null) {
        	if(Constants.VERBOSE){
        		Log.i(TAG,"maybeCreateHttpClient()");
        	}
            mHttpClient = new DefaultHttpClient();
            
            mHttpClient.getCookieStore().addCookie(null);
            responseHandler = new BasicResponseHandler();
            final HttpParams params = mHttpClient.getParams();
            HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, TIMEOUT);
            ConnManagerParams.setTimeout(params, TIMEOUT);
        }
    }
   
    /**
     * Connects to the url, gets the results
     * 
     * @param url Full web address to get from
     * @param contentType what you expect to get back, text/html or text/json
     * @return SimpleResponse a simple object that describes the response. 
     * SimpleResponse.message contains the actual content that was returned
     * or the error message
     */
    public SimpleResponse get(String url, String contentType) { 
    	SimpleResponse response = new SimpleResponse();
    	response.setUrl(url);
    	response.setContentType(contentType);
    	if (url.equals("")){
    		response.setStatus(SimpleResponse.FAIL);
    		response.setMessage("null url");
        	return response;
        }
        
        final HttpGet httpRequest = new HttpGet(url);
        maybeCreateHttpClient();

        try {
        	response.setMessage(mHttpClient.execute(httpRequest, responseHandler));
        	response.setStatus(SimpleResponse.PASS);
        	return response;
        } catch (final HttpResponseException e) {
        	if(Constants.VERBOSE){
        		e.printStackTrace();
        	}
        	response.setStatus(SimpleResponse.FAIL);
        	response.setMessage("Site not found");
        	return response;
        } catch (ClientProtocolException e) {
        	if(Constants.VERBOSE){
        		e.printStackTrace();
        	}
        	response.setStatus(SimpleResponse.FAIL);
        	response.setMessage("Client Protocol Exception");
        	return response;
        } catch (IOException e) {
			if(Constants.VERBOSE){
        		e.printStackTrace();
        	}
        	response.setStatus(SimpleResponse.FAIL);
        	response.setMessage("IO Exception");
        	return response;
		} finally {
        	if(Constants.VERBOSE){
        		Log.i(TAG, "get finished");
        	}
        }
        
    }
    
	  /**
	  * Connects to the server, performs a post and returns the results 
	  * in a SimpleResponse object. 
	  * 
	  * @param url Full web address to get from
	  * @param params paramaters built in WebModel
      * @param contentType what you expect to get back, text/html or text/json
      * @return SimpleResponse a simple object that describes the response. 
      * SimpleResponse.message contains the actual content that was returned
      * or the error message
      */
	public SimpleResponse post(String url, ArrayList<NameValuePair> params, String contentType) { 
		SimpleResponse response = new SimpleResponse();
	    response.setUrl(url);
	    response.setContentType(contentType);
	    if (url.equals("")){
	    	response.setStatus(SimpleResponse.FAIL);
	    	response.setMessage("null url");
	       	return response;
	    }
	 		     
	    HttpEntity entity = null;
	    try {
	        entity = new UrlEncodedFormEntity(params);
	    } catch (final UnsupportedEncodingException e) {
	        // this should never happen.
	        throw new AssertionError(e);
	    }
	    if(Constants.VERBOSE){
	    	Log.i(TAG,"Posting to: " + url + params);
	    }
	     
	    final HttpPost post = new HttpPost(url);
	    post.addHeader(entity.getContentType());
	    post.setEntity(entity);
	    maybeCreateHttpClient();
	
	    try {
	    	response.setMessage(mHttpClient.execute(post, responseHandler));
    		response.setStatus(SimpleResponse.PASS);
    		return response;
	    } catch (final HttpResponseException e) {
	    	if(Constants.VERBOSE){
	       		e.printStackTrace();
	       	}
	       	response.setStatus(SimpleResponse.FAIL);
	       	response.setMessage("Site not found");
	       	return response;
	    } catch (ClientProtocolException e) {
	       	if(Constants.VERBOSE){
	       		e.printStackTrace();
	      	}
	       	response.setStatus(SimpleResponse.FAIL);
	       	response.setMessage("Client Protocol Exception");
	       	return response;
	    } catch (IOException e) {
			if(Constants.VERBOSE){
	       		e.printStackTrace();
	       	}
	       	response.setStatus(SimpleResponse.FAIL);
	       	response.setMessage("IO Exception");
	       	return response;
	    } finally {
	    	if(Constants.VERBOSE){
	    		Log.i(TAG, "post completing");
	    	}
	    }
	}

    
}
