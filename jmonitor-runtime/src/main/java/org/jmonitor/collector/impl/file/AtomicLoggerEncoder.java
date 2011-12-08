/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jmonitor.collector.impl.file;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.jmonitor.collector.impl.file.AtomicLogger.LoggerCallback;

import ch.qos.logback.core.encoder.EncoderBase;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
public class AtomicLoggerEncoder extends EncoderBase<LoggerCallback> {

    // TODO implement header or footer?
    private String fileHeader;
    private String fileFooter;

    public void doEncode(LoggerCallback event) throws IOException {

        // we have exclusive access to outputStream here, see lock in
        // OutputStreamAppender.subAppend()

        // protect the underlying out so the call back cannot store it and reuse it later
        ProtectedOutputStream protectedOutputStream = new ProtectedOutputStream(
                new BufferedOutputStream(outputStream));
        PrintWriter protectedOut = new PrintWriter(protectedOutputStream);
        event.doWithLogger(protectedOut);
        protectedOut.flush();
        // deactive the protected out
        protectedOutputStream.setActive(false);
    }

    public void close() throws IOException {
        if (fileFooter == null) {
            return;
        }
        outputStream.write(fileFooter.getBytes());
    }

    @Override
    public void init(OutputStream os) throws IOException {
        super.init(os);
        if (fileHeader == null) {
            return;
        }
        outputStream.write(fileHeader.getBytes());
    }
}
