/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mimdevelopment.iot;

import com.mimdevelopment.iot.model.processors.UARTDataAvailableProcessor;
import com.mimdevelopment.iot.model.services.gateway.GatewayResource;
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.midlet.MIDlet;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.gpio.PinEvent;
import jdk.dio.gpio.PinListener;
import jdk.dio.uart.UART;
import jdk.dio.uart.UARTConfig;
import jdk.dio.uart.UARTEvent;

/**
 *
 * @author Luther Stanton
 */
public class HomeAutomate extends MIDlet {

    private static final int RAPBERRY_PI_UART_DEVICE_ID = 40;
    private static final int ZIGBEE_BAUD_RATE = 9600;

    private static final long GATEWAY_TRANSMISSION_INTERVAL_MILLIS = (1000 * 60 * 60);  // 1 hour

    private class GatewayTransmissionTask extends TimerTask {

        @Override
        public void run() {
            GatewayResource gatewayResource = GatewayResource.getInstance();
            gatewayResource.submitWeatherObservations();
        }
    }

    private class TransmitNowButtonManager implements PinListener {

        private static final int TRANSMIT_NOW_BUTTON_ID = 4;
        private final GPIOPin transmitNowButton;

        public TransmitNowButtonManager() throws IOException {
            transmitNowButton = 
                    (GPIOPin) DeviceManager.open(TRANSMIT_NOW_BUTTON_ID);
            transmitNowButton.setTrigger(GPIOPinConfig.TRIGGER_FALLING_EDGE);
            transmitNowButton.setInputListener(this);
        }

        public void close() throws IOException {
            if (transmitNowButton.isOpen()) {
                transmitNowButton.close();
            }
        }

        @Override
        public void valueChanged(PinEvent event) {
            System.out.println("["
                    + Util.formatDateForLogging(Calendar.getInstance())
                    + "] Processing 'Send Now' request.");
            
            HomeAutomate.this.gatewayTransmissionTimer.cancel();
            GatewayResource.getInstance().submitWeatherObservations();
            HomeAutomate.this.gatewayTransmissionTimer = new Timer();
            HomeAutomate.this.gatewayTransmissionTimer.scheduleAtFixedRate(
                    new GatewayTransmissionTask(),
                    GATEWAY_TRANSMISSION_INTERVAL_MILLIS, 
                    GATEWAY_TRANSMISSION_INTERVAL_MILLIS);
        }
    }

    private UART uart;
    private UARTDataAvailableProcessor uartDataAvailableProcessor;
    private Timer gatewayTransmissionTimer;

    private TransmitNowButtonManager transmitNowButtonManager;

    @Override
    public void startApp() {

        boolean initializedSucceeded = initialize();

        if (initializedSucceeded) {
            gatewayTransmissionTimer = new Timer();
            gatewayTransmissionTimer.scheduleAtFixedRate(
                    new GatewayTransmissionTask(), 
                    GATEWAY_TRANSMISSION_INTERVAL_MILLIS, 
                    GATEWAY_TRANSMISSION_INTERVAL_MILLIS);
        }
    }

    @Override
    public void destroyApp(boolean unconditional) {

        try {
            if (uart != null) {
                uart.close();
            }
        } catch (IOException e) {
            System.out.println("An IOException occurred reading the bytes from the UART device.  Exception says:[" + e.getMessage() + "].");
        }

        try {
            if (transmitNowButtonManager != null) {
                transmitNowButtonManager.close();
            }
        } catch (IOException e) {
            System.out.println("An IOException occurred closing the activityIndicatorLED.  Exception says:[" + e.getMessage() + "].");
        }
    }

    private boolean initialize() {

        boolean initializedSucceeded = true;

        // begin gateway registration
        GatewayResource gatewayResource = GatewayResource.getInstance();
        gatewayResource.register();

        // set up the required directory structure
        boolean dataDirectoryNeedsToBeCreated = false;
        try {
            FileConnection dataDirectory = (FileConnection) Connector.open("file://localhost/" + GatewayApplication.FILE_SYSTEM_PATH);
            if (!dataDirectory.exists()) {
                dataDirectoryNeedsToBeCreated = true;
                System.out.println(GatewayApplication.FILE_SYSTEM_PATH + " directory does not exist.");
            } else if (!dataDirectory.isDirectory()) {
                System.out.println(GatewayApplication.FILE_SYSTEM_PATH + "exists but is not a directory, deleting ...");
                dataDirectory.delete();
                dataDirectory.close();
                dataDirectory = (FileConnection) Connector.open(GatewayApplication.FILE_SYSTEM_PATH);
                dataDirectoryNeedsToBeCreated = true;
            }

            if (dataDirectoryNeedsToBeCreated) {
                dataDirectory.mkdir();
                System.out.println(GatewayApplication.FILE_SYSTEM_PATH + " created.");
            }

            dataDirectory.close();
        } catch (IOException e) {
            System.out.println("An IOException occurred trying to create the data directory structure"
                    + ".  Exception says:[" + e.getMessage() + "].");
            initializedSucceeded = false;
        } catch (Exception e) {
            System.out.println("An Exception occurred trying to create the data directory structure"
                    + ".  Exception says:[" + e.getMessage() + "].");
            initializedSucceeded = false;
        }

        if (initializedSucceeded) {

            // open and configure the UART
            uartDataAvailableProcessor = new UARTDataAvailableProcessor();
            try {
                uart = (UART) DeviceManager.open(RAPBERRY_PI_UART_DEVICE_ID);
                if (uart != null) {
                    uart.setBaudRate(ZIGBEE_BAUD_RATE);
                    uart.setDataBits(UARTConfig.DATABITS_8);
                    uart.setParity(UARTConfig.PARITY_NONE);
                    uart.setEventListener(
                            UARTEvent.INPUT_DATA_AVAILABLE, 
                            uartDataAvailableProcessor);
                     System.out.println("["
                    + Util.formatDateForLogging(Calendar.getInstance())
                    + "] UART opened successfully");
                } else {
                    // unable to initialze UART
                     System.out.println("["
                    + Util.formatDateForLogging(Calendar.getInstance())
                    + "] Unable to open UART.");
                    initializedSucceeded = false;
                }
            } catch (IOException e) {
                initializedSucceeded = false;
                System.out.println("["
                    + Util.formatDateForLogging(Calendar.getInstance())
                    + "] An IOException occurred opening UART device.  Exception says:[" + e.getMessage() + "].");
            }
        }

        if (initializedSucceeded) {
            try {
                transmitNowButtonManager = new TransmitNowButtonManager();
            } catch (IOException e) {
                initializedSucceeded = false;
                 System.out.println("["
                    + Util.formatDateForLogging(Calendar.getInstance())
                    + "]An IOException occurred initializing TransmitNowButtonManager.  Exception says:[" + e.getMessage() + "].");
            }
        }

        return initializedSucceeded;
    }
}