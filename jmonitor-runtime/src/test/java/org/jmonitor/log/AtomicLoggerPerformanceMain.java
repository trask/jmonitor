/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jmonitor.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;

import org.apache.commons.io.FileUtils;
import org.jmonitor.collector.impl.file.AtomicLogger;
import org.jmonitor.collector.impl.file.FileDataDestination;
import org.jmonitor.collector.impl.file.AtomicLogger.LoggerCallback;

/**
 * Performance test of {@link AtomicLogger}.
 * 
 * Since the {@link FileDataDestination} can log lots and lots of data (depending on the agent
 * settings), it is important that the logging be very fast even though it uses <a
 * href="http://logback.qos.ch/">logback</a> instead of raw file logging.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class AtomicLoggerPerformanceMain {

    private static final int TOTAL_MEGABYTES = 100;
    private static final int BYTES_PER_PRINT_SMALL = 1;
    private static final int BYTES_PER_PRINT_MEDIUM = 10;
    private static final int BYTES_PER_PRINT_LARGE = 100;

    private static final int BYTES_PER_MEGABYTE = 1024 * 1024;
    private static final String TEMP_FILE_PREFIX = "jmonitor-logger-performance-test-";
    private static final String TEMP_FILE_SUFFIX = ".%d{yyyy-MM-dd}.log";

    private static final int MILLISECONDS_PER_SECOND = 1000;

    // utility class
    private AtomicLoggerPerformanceMain() {
    }

    public static void main(String[] args) throws IOException {

        AtomicLoggerPerformanceMain test = new AtomicLoggerPerformanceMain();
        test.speedTest(TOTAL_MEGABYTES, BYTES_PER_PRINT_LARGE);
        test.speedTest(TOTAL_MEGABYTES, BYTES_PER_PRINT_MEDIUM);
        test.speedTest(TOTAL_MEGABYTES, BYTES_PER_PRINT_SMALL);
    }

    private void speedTest(int totalMegabytes, int bytesPerPrintCall) throws IOException {

        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < bytesPerPrintCall; i++) {
            buffer.append('x');
        }
        String line = buffer.toString();

        int numberOfPrintCalls = totalMegabytes * BYTES_PER_MEGABYTE / bytesPerPrintCall;

        double logbackSpeed = runSpeedTestWithLogback(numberOfPrintCalls, line);
        double rawStreamSpeed = runSpeedTestWithRawStream(numberOfPrintCalls, line);

        System.out.println(totalMegabytes + " megabytes at " // NOPMD for stdout output
                + bytesPerPrintCall + " bytes per print call");
        System.out.println("  logback speed: " + logbackSpeed + " seconds"); // NOPMD for stdout output
        System.out.println("  raw stream speed:   " + rawStreamSpeed + " seconds"); // NOPMD for stdout output
        NumberFormat format = NumberFormat.getPercentInstance();
        format.setMaximumFractionDigits(1);
        format.setMinimumFractionDigits(1);
        String percentageText = format.format((logbackSpeed - rawStreamSpeed) / logbackSpeed);
        System.out.println("  raw is " + percentageText + " faster"); // NOPMD for stdout output
    }

    private double runSpeedTestWithRawStream(int numberOfPrintCalls, String printText)
            throws IOException {

        File file = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);

        // use raw file writer
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));

        try {
            // warm-up
            runSpeedTest(out, numberOfPrintCalls, printText);

            // real run
            long startMillis = System.currentTimeMillis();
            runSpeedTest(out, numberOfPrintCalls, printText);

            return (System.currentTimeMillis() - startMillis) / (double) MILLISECONDS_PER_SECOND;

        } finally {
            // clean up
            out.close();
            FileUtils.forceDelete(file);
        }
    }

    private double runSpeedTestWithLogback(final int numberOfPrintCalls, final String printText)
            throws IOException {

        File file = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);

        // no rollover
        AtomicLogger logger = new AtomicLogger(file.getPath(), null, 0, "", "");

        try {
            // warm-up
            logger.execute(new LoggerCallback() {
                public void doWithLogger(PrintWriter logger) {
                    runSpeedTest(logger, numberOfPrintCalls, printText);
                }
            });

            // real run
            long startMillis = System.currentTimeMillis();
            logger.execute(new LoggerCallback() {
                public void doWithLogger(PrintWriter out) {
                    runSpeedTest(out, numberOfPrintCalls, printText);
                }
            });

            return (System.currentTimeMillis() - startMillis) / (double) MILLISECONDS_PER_SECOND;

        } finally {
            // clean up
            logger.stop();
            FileUtils.forceDelete(file);
        }
    }

    private void runSpeedTest(PrintWriter out, int numberOfPrintCalls, String printText) {
        for (int i = 0; i < numberOfPrintCalls; i++) {
            out.print(printText);
        }
    }
}
