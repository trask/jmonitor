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

package org.jmonitor.ui.client.configuration.model;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.pietschy.gwt.pectin.client.format.Format;
import com.pietschy.gwt.pectin.client.format.FormatException;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO genericize (may need to modify pectin library)
public class CommaSeparatedListFormatter implements Format<List> {

    public List parse(String text) throws FormatException {
        return Arrays.asList(text.split(","));
    }

    public String format(List value) {
        if (value == null) {
            return "";
        }
        StringBuffer buffer = new StringBuffer();
        Iterator<String> iterator = value.iterator();
        while (iterator.hasNext()) {
            buffer.append(iterator.next());
            if (iterator.hasNext()) {
                buffer.append(',');
            }
        }
        return buffer.toString();
    }
}
