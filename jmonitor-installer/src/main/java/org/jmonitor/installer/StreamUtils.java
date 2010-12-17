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

package org.jmonitor.installer;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

/**
 * Utility class to encapsulate proper handling of streams.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO major copy paste here from org.jmonitor.installer.util.StreamUtils, need to consolidate
public final class StreamUtils {

    // utility class
    private StreamUtils() {
    }

    public static void copy(File file, OutputStream outputStream) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            IOUtils.copy(fileInputStream, outputStream);
        } finally {
            fileInputStream.close();
        }
    }

    public static void copy(InputStream inputStream, File file) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            IOUtils.copy(inputStream, fileOutputStream);
        } finally {
            fileOutputStream.close();
        }
    }

    public static void execute(File jarInputFile, File jarOutputFile, JarStreamCallback callback)
            throws IOException {

        JarInputStream jarInputStream = null;
        JarOutputStream jarOutputStream = null;
        try {
            jarInputStream = new JarInputStream(new FileInputStream(jarInputFile));
            jarOutputStream = new JarOutputStream(new FileOutputStream(jarOutputFile));
            callback.doWithJarStreams(jarInputStream, jarOutputStream);
        } finally {
            if (jarInputStream != null) {
                jarInputStream.close();
            }
            if (jarOutputStream != null) {
                jarOutputStream.close();
            }
        }
    }

    /**
     * Callback interface used by {@link StreamUtils#execute(File, File, JarStreamCallback)}.
     */
    public interface JarStreamCallback {
        void doWithJarStreams(JarInputStream jarInputStream, JarOutputStream jarOutputStream)
                throws IOException;
    }
}
