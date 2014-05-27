/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mimdevelopment.iot.model.processors;

import com.mimdevelopment.iot.GatewayApplication;
import com.mimdevelopment.iot.Util;
import com.oracle.json.Json;
import com.oracle.json.JsonObject;
import com.oracle.json.JsonObjectBuilder;
import com.oracle.json.JsonWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 *
 * @author Luther Stanton
 */
public class ContentProcessor implements Runnable {

    private static final long WEATHER_STATE_MESSAGE = 1048739;

    private static final short LATITUDE_AVAILABLE_MASK = 0b10000000;
    private static final short LONGITUDE_AVAILABLE_MASK = 0b01000000;
    private static final short TEMPERATURE_AVAILABLE_MASK = 0b00100000;
    private static final short BAROMETER_AVAILABLE_MASK = 0b00010000;
    private static final short HUMIDITY_AVAILABLE_MASK = 0b00001000;
    private static final short WIND_AVAILABLE_MASK = 0b00000100;

    // common message element indicies
    private static final byte MESSAGE_LENGTH_INDEX = 0;
    private static final byte PRIVATE_SEGMENT_START_OFFSET_INDEX = 1;
    private static final byte MESSAGE_DIRECTION_INDEX = 2;
    private static final byte MESSAGE_CATEGORY_START_INDEX = 3;

    // common message element lengths   
    private static final byte MESSAGE_CATEGORY_BYTE_COUNT = 4;

    // WEATHER STATE private payload element lengths
    private static final byte MANUFACTURER_IDENTIFIER_BYTE_COUNT = 4;
    private static final byte SENSOR_SERIAL_NUMBER_BYTE_COUNT = 4;

    // other constants
    private static final int SIZE_OF_FLOAT = 4;
    private static final int MESSAGE_DIRECTION_INBOUND = 1;

    private class WeatherStatePayloadContent {

        public boolean hasLatitude = false;
        public boolean hasLongitude = false;
        public boolean hasTemperature = false;
        public boolean hasPressure = false;
        public boolean hasHumidity = false;
        public boolean hasWind = false;
    }

    private byte[] message;

    public ContentProcessor(byte[] message) {
        this.message = message;
    }

    @Override
    public void run() {

        // TODO: add sfatey net to ensure message array is never overrun
        // ensure that we have a complete message
        byte messageLength = message[MESSAGE_LENGTH_INDEX];
        if (messageLength != message.length) {
            // drop the processing of the message
            return;
        }

        // read addressing information
        if (message[MESSAGE_DIRECTION_INDEX] != MESSAGE_DIRECTION_INBOUND) {
            // NO-OP for the POC
            return;
        }

        byte privateSegmentStartOffset = (byte) (message[PRIVATE_SEGMENT_START_OFFSET_INDEX] - 1);
        long messageCategory = 0;

        for (int i = 0; i < MESSAGE_CATEGORY_BYTE_COUNT; i++) {
            messageCategory += Long.rotateLeft(message[MESSAGE_CATEGORY_START_INDEX + i] & 0xFF, (8 * i));
        }

        // ensure this is a message of interest
        if (messageCategory == WEATHER_STATE_MESSAGE) {
            // determine what payload elements are available
            short payloadContentFlags = (short) (message[privateSegmentStartOffset] & 0xFF);
            WeatherStatePayloadContent payloadContent = parsePayloadContentFlags(payloadContentFlags);

            double latitude = 0;
            double longitude = 0;
            Float temperature = null;
            Float pressure = null;
            long manufacturerIdentifier = 0;
            long sensorSerialNumber = 0;
            byte bufferIndex = (byte) (privateSegmentStartOffset + 1);

            if (payloadContent.hasLatitude) {
                byte latitudeDegrees = message[bufferIndex++];

                byte[] latitudeMinutesSecondsRawBytes = new byte[SIZE_OF_FLOAT];
                for (int x = 0; x < SIZE_OF_FLOAT; x++) {
                    latitudeMinutesSecondsRawBytes[x] = message[bufferIndex + x];
                }
                float latitudeMinutesSeconds = ByteBuffer.wrap(latitudeMinutesSecondsRawBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                if (latitudeDegrees < 0) {
                    latitude = (double) ((int) latitudeDegrees - (latitudeMinutesSeconds / 60.0));
                } else {
                    latitude = (double) ((int) latitudeDegrees + (latitudeMinutesSeconds / 60.0));
                }

                bufferIndex += SIZE_OF_FLOAT;
            }

            if (payloadContent.hasLongitude) {
                byte longitudeDegrees = message[bufferIndex++];

                byte[] longitudeMinutesSecondsRawBytes = new byte[SIZE_OF_FLOAT];
                for (int x = 0; x < SIZE_OF_FLOAT; x++) {
                    longitudeMinutesSecondsRawBytes[x] = message[bufferIndex + x];
                }
                float longitudeMinutesSeconds = ByteBuffer.wrap(longitudeMinutesSecondsRawBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                if (longitudeDegrees < 0) {
                    longitude = (double) ((int) longitudeDegrees - (longitudeMinutesSeconds / 60.0));
                } else {
                    longitude = (double) ((int) longitudeDegrees + (longitudeMinutesSeconds / 60.0));
                }

                bufferIndex += SIZE_OF_FLOAT;
            }

            if (payloadContent.hasTemperature) {
                // TODO: implement - no op is OK for now as it will not be sent in POC
            }

            if (payloadContent.hasPressure) {
                byte[] pressureRawBytes = new byte[SIZE_OF_FLOAT];
                for (int x = 0; x < SIZE_OF_FLOAT; x++) {
                    pressureRawBytes[x] = message[bufferIndex + x];
                }
                pressure = new Float(ByteBuffer.wrap(pressureRawBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat());

                bufferIndex += SIZE_OF_FLOAT;
            }

            if (payloadContent.hasHumidity) {
                // TODO: implement - no op is OK for now as it will not be sent in POC
            }

            if (payloadContent.hasWind) {
                // TODO: implement - no op is OK for now as it will not be sent in POC
            }

            for (int i = 0; i < MANUFACTURER_IDENTIFIER_BYTE_COUNT; i++) {
                manufacturerIdentifier += Long.rotateLeft(message[bufferIndex + i] & 0xFF, (8 * i));
            }

            bufferIndex += MANUFACTURER_IDENTIFIER_BYTE_COUNT;

            for (int i = 0; i < SENSOR_SERIAL_NUMBER_BYTE_COUNT; i++) {
                sensorSerialNumber += Long.rotateLeft(message[bufferIndex + i] & 0xFF, (8 * i));
            }

            System.out.println("["
                    + Util.formatDateForLogging(Calendar.getInstance())
                    + "] Message Parsing Completed.  Found the following values ....");
            System.out.println("Latitude:[" + latitude + "]");
            System.out.println("Longitude:[" + longitude + "]");
            System.out.println("Pressure:[" + pressure.toString() + "]");
            System.out.println("Manufacturer Id:[" + manufacturerIdentifier + "]");
            System.out.println("Sensor Serial Number:[" + sensorSerialNumber + "]");

            writeFile(buildJsonContent(
                    latitude, 
                    longitude, 
                    temperature, 
                    pressure.doubleValue(), 
                    new Long(manufacturerIdentifier), 
                    new Long(sensorSerialNumber)));
        }
    }

    protected JsonObject buildJsonContent(double latitude, double longitude, Float temperature, double pressure, Long manufacturerId, Long sensorSerialNumber) {

        JsonObjectBuilder value = Json.createObjectBuilder();

        value.add("latitude", latitude);

        value.add("longitude", longitude);

        if (temperature != null) {
            value.add("temperature", temperature.doubleValue());
        }

        value.add("pressure", pressure);

        if (sensorSerialNumber != null) {
            value.add("sensorIdentifier", sensorSerialNumber.longValue());
        }

        if (manufacturerId != null) {
            value.add("sensorManufacturer", manufacturerId.longValue());
        }

        value.add("submissionDate", Util.formatUTCDate(Calendar.getInstance()));

        return value.build();
    }

    protected void writeFile(JsonObject jsonContent) {

        Long fileNameSeed = new Long(new Date().getTime());

        try {

            // TODO: handle file name duplicates
            FileConnection targetFile = (FileConnection) Connector.open("file://localhost/" + GatewayApplication.FILE_SYSTEM_PATH + "/" + fileNameSeed.toString());
            targetFile.create();
            try (JsonWriter targetFileWriter = Json.createWriter(new BufferedWriter(new OutputStreamWriter(targetFile.openOutputStream())))) {
                targetFileWriter.writeObject(jsonContent);
            }

            if (targetFile.isOpen()) {
                targetFile.close();
            }
        } catch (IOException e) {
            System.out.println("Failed to write file!");
        }
    }

    private WeatherStatePayloadContent parsePayloadContentFlags(short payloadContentFlags) {

        WeatherStatePayloadContent availablePayloadElements = new WeatherStatePayloadContent();

        if ((LATITUDE_AVAILABLE_MASK & payloadContentFlags) == LATITUDE_AVAILABLE_MASK) {
            availablePayloadElements.hasLatitude = true;
        }
        if ((LONGITUDE_AVAILABLE_MASK & payloadContentFlags) == LONGITUDE_AVAILABLE_MASK) {
            availablePayloadElements.hasLongitude = true;
        }
        if ((TEMPERATURE_AVAILABLE_MASK & payloadContentFlags) == TEMPERATURE_AVAILABLE_MASK) {
            availablePayloadElements.hasTemperature = true;
        }
        if ((BAROMETER_AVAILABLE_MASK & payloadContentFlags) == BAROMETER_AVAILABLE_MASK) {
            availablePayloadElements.hasPressure = true;
        }
        if ((HUMIDITY_AVAILABLE_MASK & payloadContentFlags) == HUMIDITY_AVAILABLE_MASK) {
            availablePayloadElements.hasHumidity = true;
        }
        if ((WIND_AVAILABLE_MASK & payloadContentFlags) == WIND_AVAILABLE_MASK) {
            availablePayloadElements.hasWind = true;
        }
        return availablePayloadElements;
    }
}
