/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mimdevelopment.iot.model.services.gateway.operations;

import com.mimdevelopment.iot.Globals;
import com.mimdevelopment.iot.model.services.definition.PutRestOperation;
import com.oracle.httpclient.HttpMessageHeaders;
import com.oracle.httpclient.HttpResponseListener;
import com.oracle.httpclient.StreamedMessageBody;
import com.oracle.json.Json;
import com.oracle.json.JsonBuilderFactory;
import com.oracle.json.JsonObject;
import com.oracle.json.JsonObjectBuilder;
import com.oracle.json.JsonWriter;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Luther Stanton
 */
public class RegisterOperation extends PutRestOperation {

    private static final String ACTION = "gateway/register";

    private class RegisterOperationPayloadWriter implements StreamedMessageBody {

        private Double latitude;
        private Double longitude;

        public RegisterOperationPayloadWriter(
                Double latitude, Double longitude) {

            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        public void write(HttpMessageHeaders headers, OutputStream bodyStream) throws IOException {
            JsonObjectBuilder value = Json.createObjectBuilder();
            
            if(latitude != null){
                value.add("latitude", latitude.doubleValue());
            }
            
            if(longitude != null){
                value.add("longitude", longitude.doubleValue());
            }
                            
            JsonWriter jsonWriter = Json.createWriter(bodyStream);
            jsonWriter.writeObject(value.build());
            jsonWriter.close();
        }
    }

    private String gatewayIdentifier;
    private Double latitude;
    private Double longitude;

    public RegisterOperation(String gatewayIdentifier) {
        this.gatewayIdentifier = gatewayIdentifier;
    }

    @Override
    protected String getUri() {

        StringBuilder uri = new StringBuilder(Globals.USE_SECURE ? "https://" : "http://");
        uri.append(Globals.SERVER);
        uri.append("/");
        uri.append(Globals.BASE_URI);
        uri.append("/");
        uri.append(ACTION);
        uri.append("/");
        uri.append(gatewayIdentifier);

        return uri.toString();
    }

    @Override
    protected StreamedMessageBody getRequestPayload() {
        return new RegisterOperationPayloadWriter(latitude, longitude);
    }

    public void invoke(
            Double latitude, 
            Double longitude, 
            HttpResponseListener responseListener) {
        
        this.latitude = latitude;
        this.longitude = longitude;
        
        execute(responseListener);
        
    }
}
