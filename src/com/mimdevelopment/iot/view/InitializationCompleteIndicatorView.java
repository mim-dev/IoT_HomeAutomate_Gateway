/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mimdevelopment.iot.view;

import com.mimdevelopment.iot.Globals;
import java.io.IOException;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;

/**
 *
 * @author Luther Stanton
 */
public class InitializationCompleteIndicatorView implements Runnable {

    @Override
    public void run() {

        GPIOPin activityIndicatorLED = null;

        try {
            activityIndicatorLED = (GPIOPin) DeviceManager.open(Globals.ACTIVITY_INDICATOR_LED_ID);

            activityIndicatorLED.setValue(true);
            Thread.sleep(250);
            activityIndicatorLED.setValue(false);
            Thread.sleep(250);
            activityIndicatorLED.setValue(true);
            Thread.sleep(250);
            activityIndicatorLED.setValue(false);
            Thread.sleep(250);
            activityIndicatorLED.setValue(true);
            Thread.sleep(250);
            activityIndicatorLED.setValue(false);

            activityIndicatorLED.close();
        } catch (InterruptedException | IOException e) {
            System.out.println("Encounter exception of type[" + e.getClass().toString() + "] from InitializationCompleteIndicatorView.  Exception says:[" + e.getMessage() + "].");
            if (activityIndicatorLED != null && activityIndicatorLED.isOpen()) {
                try {
                    activityIndicatorLED.close();
                } catch (IOException ex) {
                    // NO-OP - we tried to clean up!
                }
            }
        }
    }
}
