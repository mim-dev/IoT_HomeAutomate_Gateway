/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mimdevelopment.iot.model.services.definition;

import com.oracle.httpclient.HttpClient;
import com.oracle.httpclient.HttpClientBuilder;
import com.oracle.httpclient.HttpHeader;
import com.oracle.httpclient.HttpRequest;
import com.oracle.httpclient.HttpRequestBuilder;
import com.oracle.httpclient.HttpResponseListener;
import javax.microedition.io.ConnectionOption;

/**
 *
 * @author Luther Stanton
 */
public abstract class BaseRestOperation {
    
    private static final Integer DEFAULT_SOCKET_TIMEOUT_MS = new Integer(5000);
  
    protected abstract String getUri();
    
    protected BaseRestOperation(){ 
    }
    
    protected Integer getSocketTimeoutMS(){
        return DEFAULT_SOCKET_TIMEOUT_MS; 
    }
    
    protected HttpClientBuilder buildHttpClientBuilder(){
        HttpClientBuilder clientBuilder = HttpClientBuilder.getInstance();
        ConnectionOption<Integer> timeoutOption = new ConnectionOption<>("Timeout", getSocketTimeoutMS());
        clientBuilder.addConnectionOptions(timeoutOption);
        return clientBuilder;
    }
    
    protected HttpClient buildHttpClient(){
        return buildHttpClientBuilder().build();
    }
    
    protected HttpRequestBuilder buildHttpRequestBuilder(){
        HttpRequestBuilder httpRequestBuilder 
                = buildHttpClient().build(getUri());
        
        httpRequestBuilder.setHeader(HttpHeader.ACCEPT, "application/json");
        httpRequestBuilder.setHeader(HttpHeader.CONTENT_TYPE, "application/json");
        
        return httpRequestBuilder;
    } 
    
    protected void execute(HttpResponseListener responseListener){
        
        // TODO: work in template values
        
        HttpRequest httpRequest = buildHttpRequestBuilder().build((String)null);
        httpRequest.invoke(responseListener);
         
    }
}
