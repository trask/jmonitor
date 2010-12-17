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

package org.jmonitor.installer.base.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO major copy paste here from org.jmonitor.installer.util.ResourceUtils, need to consolidate
public final class ResourceUtils {

    // utility class
    private ResourceUtils() {
    }

    public static File extractResource(String resourcePath, File tempDirectory) throws IOException {

        String resourceName = new File(resourcePath).getName();
        File extractedResourceFile = new File(tempDirectory, resourceName);
        
        InputStream classInputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        try {
            StreamUtils.copy(classInputStream, extractedResourceFile);
        } finally {
            classInputStream.close();
        }
        
        return extractedResourceFile;
    }
}
