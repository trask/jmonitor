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

import java.util.concurrent.TimeUnit;

/**
 * Basic formatting utility.
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public final class FormatUtils {

    private static final int SECONDS_PER_MINUTE = 60;

    private FormatUtils() {}

    public static String formatAsMinutesAndSeconds(long milliseconds) {
        int seconds = (int) TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        int minutes = seconds / SECONDS_PER_MINUTE;
        int leftoverSeconds = seconds % SECONDS_PER_MINUTE;
        if (minutes > 0) {
            return minutes + " minutes and " + leftoverSeconds + " seconds";
        } else {
            return seconds + " seconds";
        }
    }
}
