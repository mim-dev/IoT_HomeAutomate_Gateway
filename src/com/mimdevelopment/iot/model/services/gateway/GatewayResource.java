/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mimdevelopment.iot.model.services.gateway;

import com.mimdevelopment.iot.GatewayApplication;
import com.mimdevelopment.iot.Util;
import com.mimdevelopment.iot.model.services.gateway.operations.RegisterOperation;
import com.mimdevelopment.iot.model.services.gateway.operations.SubmitWeatherObservationsOperation;
import com.mimdevelopment.iot.view.InitializationCompleteIndicatorView;
import com.mimdevelopment.iot.view.TransmissionStartingView;
import com.oracle.httpclient.HttpClientException;
import com.oracle.httpclient.HttpResponse;
import com.oracle.httpclient.HttpResponseListener;
import com.oracle.json.Json;
import com.oracle.json.stream.JsonParser;
import com.oracle.json.stream.JsonParser.Event;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 *
 * @author Luther Stanton
 */
public class GatewayResource {

    private class RegisterResponseListener implements HttpResponseListener {

        @Override
        public void failed(HttpClientException cause) {
            System.out.println(Util.formatDateForLogging(Calendar.getInstance())
                    + " Registration failed.  Exception says:["
                    + cause.getMessage() + "]");
        }

        @Override
        public void handle(HttpResponse response) {

            Thread activityIndictorDisplayThread = new Thread(new InitializationCompleteIndicatorView());
            activityIndictorDisplayThread.start();

            int responseCode = response.getResponseCode();

            System.out.println(
                    "[" + Util.formatDateForLogging(Calendar.getInstance())
                    + "] Registration Succeeded.  Received HTTP Status:["
                    + responseCode + "].");

            if (responseCode == HttpResponse.OK) {

                JsonParser responseParser = null;
                Integer gatewayId = null;
                try {
                    responseParser = Json.createParser(response.getBodyStream());
                    Event jsonParsingEvent;
                    boolean parsingGatewayId = false;

                    while (responseParser.hasNext()) {
                        jsonParsingEvent = responseParser.next();
                        if (jsonParsingEvent == Event.START_OBJECT) {
                            // move to the object; 
                        } else if (jsonParsingEvent == Event.KEY_NAME) {
                            String keyName = responseParser.getString();
                            if (keyName.equalsIgnoreCase("gatewayId")) {
                                parsingGatewayId = true;
                            }
                        } else if (jsonParsingEvent == Event.VALUE_NUMBER && parsingGatewayId) {
                            gatewayId = new Integer(responseParser.getInt());
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exception while parsing the Json response.  Exception says:[" + e.getMessage() + "].");
                } finally {
                    if (responseParser != null) {
                        responseParser.close();
                    }
                }

                if (gatewayId != null) {
                    GatewayResource.this.identifier = gatewayId;
                    Logger.getGlobal().log(Level.INFO, "Set gatewayId:[{0}].", gatewayId);
                }
            } else {
                System.out.println("I do not understand how to process the response code.  Giving up....");
            }
        }
    }

    private static GatewayResource instance = null;
    private Integer identifier = null;

    public Integer getIdentifier() {
        return identifier;
    }

    public boolean isRegistered() {
        return identifier != null;
    }

    protected GatewayResource() {
    }

    public static GatewayResource getInstance() {
        if (instance == null) {
            instance = new GatewayResource();
        }
        return instance;
    }

    public void register() {
        RegisterOperation registerOp = new RegisterOperation(GatewayApplication.IDENTIFIER);
        registerOp.invoke(GatewayApplication.LATITUDE, GatewayApplication.LONGITUDE, new RegisterResponseListener());
    }

    public void submitWeatherObservations() {

        try {
            if (isRegistered()) {

                // build the file list
                List<String> srcFilePaths = new ArrayList<>(5);
                FileConnection dataDir = (FileConnection) Connector.open("file://localhost/" + GatewayApplication.FILE_SYSTEM_PATH);

                if (dataDir.isDirectory()) {
                    Enumeration e = dataDir.list();
                    while (e.hasMoreElements()) {
                        srcFilePaths.add(e.nextElement().toString());
                    }
                }

                if (srcFilePaths.size() > 0) {
                    Thread transmissionStartingDisplayThread = new Thread(new TransmissionStartingView());
                    transmissionStartingDisplayThread.start();

                    SubmitWeatherObservationsOperation submitWeatherObservationsOp = new SubmitWeatherObservationsOperation(identifier);
                    submitWeatherObservationsOp.invoke(srcFilePaths);
                } else {
                    System.out.println("There are currently no sensor reports to process.");
                }
            } else {
                System.out.println("Gateway is not registered.  Unable to submit data....");
            }
        } catch (IOException e) {
            System.out.println("IOException trying to read JSON source file.  Exception says:[" + e.getMessage() + "]");
        }
    }
}
