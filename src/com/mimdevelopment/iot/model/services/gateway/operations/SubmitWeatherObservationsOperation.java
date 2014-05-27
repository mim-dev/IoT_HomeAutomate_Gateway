/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mimdevelopment.iot.model.services.gateway.operations;

import com.mimdevelopment.iot.GatewayApplication;
import com.mimdevelopment.iot.Globals;
import com.mimdevelopment.iot.Util;
import com.mimdevelopment.iot.model.services.definition.PostRestOperation;
import com.oracle.httpclient.HttpClientException;
import com.oracle.httpclient.HttpMessageHeaders;
import com.oracle.httpclient.HttpResponse;
import com.oracle.httpclient.HttpResponseListener;
import com.oracle.httpclient.StreamedMessageBody;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Calendar;
import java.util.List;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 *
 * @author Luther Stanton
 */
public class SubmitWeatherObservationsOperation extends PostRestOperation {

    private static final String ACTION_PREFIX = "gateway/";
    private static final String ACTION_POSTFIX = "/barometricData";

    private class SubmitWeatherObservationsOperationPayloadWriter implements StreamedMessageBody {

        @Override
        public void write(HttpMessageHeaders headers, OutputStream bodyStream) throws IOException {
            FileConnection srcFile;

            List<String> srcFilePaths = SubmitWeatherObservationsOperation.this.srcFilePaths;
            int fileCount = srcFilePaths == null ? 0 : srcFilePaths.size();
            int iteration = 1;

             System.out.println("[" + 
                    Util.formatDateForLogging(Calendar.getInstance())
                    + "] Processing ["
                     + (srcFilePaths == null ? 0 : srcFilePaths.size()) + "] files.");

            StringBuilder jsonPayload = new StringBuilder("{\"data\":[");

            for (String srcFilePath : srcFilePaths) {
                try {

                    srcFile = (FileConnection) Connector.open("file://localhost/" + GatewayApplication.FILE_SYSTEM_PATH + "/" + srcFilePath);
                    if (srcFile.exists()) {

                        char[] charBuffer = new char[1024];
                        StringBuilder stringBuffer = new StringBuilder();
                        Reader in = new BufferedReader(new InputStreamReader(srcFile.openDataInputStream()));
                        int charsRead;

                        while ((charsRead = in.read(charBuffer, 0, 1024)) > 0) {
                            jsonPayload.append(charBuffer, 0, charsRead);
                        }

                        srcFile.close();
                        if (iteration < fileCount) {
                            jsonPayload.append(",");
                        }

                        iteration += 1;
                    }
                } catch (IOException e) {
                    System.out.println("IOException trying to read JSON source file.  Exception says:[" + e.getMessage() + "]");
                }
            }
            jsonPayload.append("]}");

            Writer payloadWriter = new BufferedWriter(new OutputStreamWriter(bodyStream));
            payloadWriter.write(jsonPayload.toString());
            payloadWriter.close();
        }
    }

    private class SubmitWeatherObservationsResponseListener implements HttpResponseListener {

        private class FileCleanUp implements Runnable {

            private List<String> filesForCleanup;

            public FileCleanUp(List<String> filesPathsToCleanup) {
                this.filesForCleanup = filesPathsToCleanup;
            }

            @Override
            public void run() {
                for (String fileForCleanUp : filesForCleanup) {
                    FileConnection srcFile = null;
                    try {
                        srcFile = (FileConnection) Connector.open("file://localhost/" + GatewayApplication.FILE_SYSTEM_PATH + "/" + fileForCleanUp);
                        if (srcFile.exists()) {
                            System.out.println("["
                                    + Util.formatDateForLogging(Calendar.getInstance())
                                    + "] Deleting file:[" + fileForCleanUp + "].");
                            srcFile.delete();
                        }
                    } catch (IOException e) {
                        System.out.println("IO Exception deleting file.  Exception says[" + e.getMessage() + "].");
                        if (srcFile != null && srcFile.isOpen()) {
                            try {
                                srcFile.close();
                            } catch (IOException ex) {
                                ; // NO-OP
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Exception deleting file.  Exception says[" + e.getMessage() + "].");
                        if (srcFile != null && srcFile.isOpen()) {
                            try {
                                srcFile.close();
                            } catch (IOException ex) {
                                ; // NO-OP
                            }
                        }
                    }
                }
            }
        }

        private List<String> srcFilePath;

        public SubmitWeatherObservationsResponseListener(List<String> srcFilePath) {
            this.srcFilePath = srcFilePath;
        }

        @Override
        public void failed(HttpClientException cause) {
            System.out.println("["
                    + Util.formatDateForLogging(Calendar.getInstance())
                    + "]Weather observation submission failed.  Exception says:["
                    + cause.getMessage() + "]");
        }

        @Override
        public void handle(HttpResponse response) {
            int responseCode = response.getResponseCode();
            System.out.println("["
                    + Util.formatDateForLogging(Calendar.getInstance())
                    + "] Weather observation submission Succeeded.  Received HTTP Status:["
                    + responseCode + "].");

            if (responseCode == HttpResponse.NO_CONTENT) {
                System.out.println("["
                        + Util.formatDateForLogging(Calendar.getInstance())
                        + "] Deleting source fles ...");
                Thread cleanupThread = new Thread(new FileCleanUp(srcFilePaths));
                cleanupThread.start();
            } else {
                System.out.println("I do not understand how to process the response code.  Giving up....");
            }
        }
    }

    private final int gatewayId;
    private List<String> srcFilePaths;

    public SubmitWeatherObservationsOperation(int gatewayId) {
        this.gatewayId = gatewayId;
    }

    @Override
    protected String getUri() {

        StringBuilder uri = new StringBuilder(Globals.USE_SECURE ? "https://" : "http://");
        uri.append(Globals.SERVER);
        uri.append("/");
        uri.append(Globals.BASE_URI);
        uri.append("/");
        uri.append(ACTION_PREFIX);
        uri.append(gatewayId);
        uri.append(ACTION_POSTFIX);

        return uri.toString();
    }

    @Override
    protected StreamedMessageBody getRequestPayload() {
        return new SubmitWeatherObservationsOperation.SubmitWeatherObservationsOperationPayloadWriter();
    }

    public void invoke(List<String> srcFilePaths) {
        this.srcFilePaths = srcFilePaths;
        execute(new SubmitWeatherObservationsResponseListener(srcFilePaths));
    }
}
