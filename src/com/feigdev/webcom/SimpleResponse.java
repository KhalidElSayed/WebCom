package com.feigdev.webcom;

/**
 * This is the object that will be used to send the calling class information
 * about the results of the http interaction.  
 * 
 * @author emil10001
 *
 */
public class SimpleResponse {
	private int status;
	private String message;
	private String contentType;
	private String url;
	private int id;
	public static final int FAIL = 1;
	public static final int PASS = 0;
	public static final int NOTEXECUTED = -1;
	
	public SimpleResponse(){
		status = NOTEXECUTED;
		url = "";
		message = "";
		contentType = "";
	}
	
	/**
	 * @return either SimpleResponse.FAIL, SimpleResponse.PASS or SimpleResponse.NOTEXECUTED
	 */
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	/**
	 * 
	 * @return the content of the results. If doing a GET on a webpage, this will contain all 
	 * of the html of that page 
	 */
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	
}
