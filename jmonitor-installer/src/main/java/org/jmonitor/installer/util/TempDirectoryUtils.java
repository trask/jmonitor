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
package org.jmonitor.installer.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class for working with temporary directories.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class TempDirectoryUtils {

    private static final Log LOG = LogFactory.getLog(TempDirectoryUtils.class);

    // utility class
    private TempDirectoryUtils() {}

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

    public static void execute(String prefix, TempDirectoryCallback callback) throws IOException {

        File tempDirectory = createTempDirectory(prefix);
        LOG.debug("using temp directory: " + tempDirectory.getPath());
        try {
            callback.doWithTempDirectory(tempDirectory);
        } finally {
            LOG.debug("deleting temp directory: " + tempDirectory.getPath());
            try {
                FileUtils.deleteDirectory(tempDirectory);
            } catch (IOException e) {

                // try gc to see if an unclosed stream needs garbage collecting

                // TODO submit aspectj patch to close temporary ZipFile inside
                // org.aspectj.util.FileUtil.isZipFile()

                System.gc();
                System.gc();

                // try one more time after gc
                try {
                    FileUtils.deleteDirectory(tempDirectory);
                } catch (IOException e2) {
                    LOG.warn("could not delete temp directory: " + tempDirectory.getPath());
                    LOG.debug(e, e);
                }
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
