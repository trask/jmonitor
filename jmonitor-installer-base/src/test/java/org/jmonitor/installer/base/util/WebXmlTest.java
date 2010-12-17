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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class WebXmlTest {

    @Test
    public void testWebXml() throws IOException {
        
        WebXml webXml = new WebXml(IOUtils.toString(WebXmlTest.class.getClassLoader().getResourceAsStream("petstore-web.xml")));

        webXml.addFilter("jmonitor", "org.jmonitor.ui.server.MonitorUiFilter",
                "the filter below was added by the jmonitor installer",
                "the filter above was added by the jmonitor installer");

        webXml.addFilterMapping("jmonitor", "/jmonitor/*",
                "the filter-mapping below was added by the jmonitor installer",
                "the filter-mapping above was added by the jmonitor installer");
        
        System.out.println(webXml.toString());
    }
}
