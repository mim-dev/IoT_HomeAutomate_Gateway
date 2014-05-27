/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.mimdevelopment.iot.model.services.definition;

import com.oracle.httpclient.HttpHeader;
import com.oracle.httpclient.HttpMethod;
import com.oracle.httpclient.HttpRequestBuilder;
import com.oracle.httpclient.StreamedMessageBody;

/**
 *
 * @author Luther Stanton
 */
public abstract class PostRestOperation extends BaseRestOperation {
    
    protected abstract StreamedMessageBody getRequestPayload();
    
    protected HttpRequestBuilder buildHttpRequestBuilder(){
        HttpRequestBuilder httpRequestBuilder = super.buildHttpRequestBuilder();
        httpRequestBuilder.setHeader(HttpHeader.CONTENT_TYPE, "application/json");
        httpRequestBuilder.setMethod(HttpMethod.POST);
        
        httpRequestBuilder.setBody(getRequestPayload());
        
        return httpRequestBuilder;
    } 
}
