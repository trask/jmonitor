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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for working with temporary directories.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO major copy paste here from org.jmonitor.installer.util.TempDirectoryUtils, need to consolidate
public final class TempDirectoryUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(TempDirectoryUtils.class);

    // utility class
    private TempDirectoryUtils() {
    }

    private static File createTempDirectory(String prefix) throws IOException {
        File tempDirectory = File.createTempFile(prefix, "");
        boolean deleteSuccess = tempDirectory.delete();
        if (!deleteSuccess) {
            throw new IOException("Could not delete file '" + tempDirectory.getPath() + "'");
        }
        boolean mkdirSuccess = tempDirectory.mkdir();
        if (!mkdirSuccess) {
            throw new IOException("Could not create directory '" + tempDirectory.getPath() + "'");
        }
        return tempDirectory;
    }

    public static void execute(String prefix, boolean deleteDirectory,
            TempDirectoryCallback callback) throws IOException {

        File tempDirectory = createTempDirectory(prefix);
        LOGGER.debug("using temp directory: " + tempDirectory.getPath());
        try {
            callback.doWithTempDirectory(tempDirectory);
        } finally {
            if (deleteDirectory) {
                LOGGER.debug("deleting temp directory: " + tempDirectory.getPath());
                deleteDirectory(tempDirectory);
            }
        }
    }

    private static void deleteDirectory(File tempDirectory) {
        try {
            FileUtils.deleteDirectory(tempDirectory);
        } catch (IOException e) {

            // try gc to see if an unclosed stream needs garbage collecting

            // TODO submit aspectj patch to close temporary ZipFile inside
            // org.aspectj.util.FileUtil.isZipFile()

            System.gc(); // NOPMD for explicit gc, see comment above
            System.gc(); // NOPMD for explicit gc, see comment above

            // try one more time after gc
            try {
                FileUtils.deleteDirectory(tempDirectory);
            } catch (IOException e2) {
                LOGGER.warn("could not delete temp directory: " + tempDirectory.getPath());
                // only output stack trace if debug is enabled
                LOGGER.debug("could not delete temp directory: " + tempDirectory.getPath(), e);
            }
        }
    }

    /**
     * Callback interface used by {@link TempDirectoryUtils#execute(String, TempDirectoryCallback)}.
     */
    public interface TempDirectoryCallback {
        void doWithTempDirectory(File tempDirectory) throws IOException;
    }
}
