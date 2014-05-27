/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mimdevelopment.iot.model.processors;

import com.mimdevelopment.iot.Util;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Calendar;
import jdk.dio.uart.UARTEvent;
import jdk.dio.uart.UARTEventListener;

/**
 *
 * @author Luther Stanton
 */
public class UARTDataAvailableProcessor implements UARTEventListener {

    private static final int BASE_BUFFER_ALLOCATION_SIZE = 200;
    private static final short MESSAGE_SIZE_OFFSET = 0;

    private byte[] buffer;
    private int currentBufferPopulation;
    private int maxBufferPopulation;
    private int expectedMessageSize;

    public UARTDataAvailableProcessor() {
        // initialize to an empty buffer
        currentBufferPopulation = 0;
        expectedMessageSize = 0;
        maxBufferPopulation = 0;
    }

    @Override
    public void eventDispatched(UARTEvent event) {

        if (event.getID() == UARTEvent.INPUT_DATA_AVAILABLE) {
            ByteBuffer byteBuffer;
            try {
                byteBuffer = ByteBuffer.allocateDirect(BASE_BUFFER_ALLOCATION_SIZE);
                int numberOfBytesRead = event.getDevice().read(byteBuffer);

                if (numberOfBytesRead > 0) {

                    byte[] readBuffer = new byte[numberOfBytesRead];
                    byteBuffer.get(readBuffer);

                    if (currentBufferPopulation == 0) {
                        resetMessageBuffer(readBuffer[MESSAGE_SIZE_OFFSET]);
                        processMessageBuffer(readBuffer, 0);
                    } else {
                        // existing message
                        if (expectedMessageSize <= (numberOfBytesRead + currentBufferPopulation)) {
                            // there are enough bytes available to form a complete message - move the existing buffer into the fullMessage
                            byte[] fullMessage = Arrays.copyOf(buffer, expectedMessageSize);
                            int readBufferIndex = 0;

                            // copy the remaining bytes needed to complete the fullMessage from the buffer of bytes just read
                            for (int fullMessageOffset = currentBufferPopulation; fullMessageOffset < expectedMessageSize; fullMessageOffset++) {
                                fullMessage[fullMessageOffset] = readBuffer[readBufferIndex++];
                            }
                            Thread contentProcessorThread = new Thread(new ContentProcessor(fullMessage));
                            contentProcessorThread.start();

                            if (readBufferIndex < numberOfBytesRead) {
                                resetMessageBuffer(readBuffer[readBufferIndex]);
                                processMessageBuffer(readBuffer, readBufferIndex);
                            } else {
                                clearMessageBuffer();
                            }
                        } else {
                            // reallocate buffer to hold the expanded content
                            if (maxBufferPopulation < currentBufferPopulation + numberOfBytesRead) {
                                maxBufferPopulation = currentBufferPopulation + numberOfBytesRead + BASE_BUFFER_ALLOCATION_SIZE;
                                buffer = Arrays.copyOf(buffer, maxBufferPopulation);

                                for (int bufferOffset = currentBufferPopulation, readBufferIndex = 0; readBufferIndex < numberOfBytesRead; bufferOffset++, readBufferIndex++) {
                                    buffer[bufferOffset] = readBuffer[readBufferIndex];
                                }

                                currentBufferPopulation += numberOfBytesRead;
                            } else {
                                for (int bufferOffset = currentBufferPopulation, readBufferIndex = 0; readBufferIndex < numberOfBytesRead; bufferOffset++, readBufferIndex++) {
                                    buffer[bufferOffset] = readBuffer[readBufferIndex];
                                }
                                currentBufferPopulation += numberOfBytesRead;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                 System.out.println("["
                    + Util.formatDateForLogging(Calendar.getInstance())
                    + "]An IOException occurred reading the bytes from the UART device.  Exception says:[" + e.getMessage() + "].");
                // cancel reading as something has happened               
            } catch (Exception e) {
                 System.out.println("["
                    + Util.formatDateForLogging(Calendar.getInstance())
                    + "]An Exception occurred reading the bytes from the UART device.  Exception says:[" + e.getMessage() + "].");
            }
        }
    }

    void processMessageBuffer(byte[] readBuffer, int readBufferIndex) {

        int readBufferSize = readBuffer.length;

        if (expectedMessageSize <= readBufferSize - readBufferIndex) {
            byte[] fullMessage = new byte[expectedMessageSize];
            for (int fullMessageIndex = 0; fullMessageIndex < expectedMessageSize; fullMessageIndex++, readBufferIndex++) {
                fullMessage[fullMessageIndex] = readBuffer[readBufferIndex];
            }
            Thread contentProcessorThread = new Thread(new ContentProcessor(fullMessage));
            contentProcessorThread.start();

            if (readBufferIndex < readBufferSize) {
                resetMessageBuffer(readBuffer[readBufferIndex]);
                processMessageBuffer(readBuffer, readBufferIndex);
            } else {
                clearMessageBuffer();
            }
        } else if (readBufferIndex == readBufferSize) {
            // reached the end of the read buffer
            buffer = null;
            currentBufferPopulation = 0;
            expectedMessageSize = 0;
            maxBufferPopulation = 0;
        } else {
            // remaining buffer only has a partial message
            buffer = new byte[BASE_BUFFER_ALLOCATION_SIZE];
            int bufferIndex;
            for (bufferIndex = 0; readBufferIndex < readBufferSize; readBufferIndex++, bufferIndex++) {
                buffer[bufferIndex] = readBuffer[readBufferIndex];
            }

            expectedMessageSize = buffer[MESSAGE_SIZE_OFFSET];
            maxBufferPopulation = BASE_BUFFER_ALLOCATION_SIZE;
            currentBufferPopulation = bufferIndex;
        }
    }

    void resetMessageBuffer(int expectedMessageSize) {
        this.expectedMessageSize = expectedMessageSize;
        buffer = new byte[BASE_BUFFER_ALLOCATION_SIZE];
        currentBufferPopulation = 0;
        maxBufferPopulation = BASE_BUFFER_ALLOCATION_SIZE;
    }

    void clearMessageBuffer() {
        expectedMessageSize = 0;
        buffer = null;
        currentBufferPopulation = 0;
        maxBufferPopulation = 0;
    }
}
