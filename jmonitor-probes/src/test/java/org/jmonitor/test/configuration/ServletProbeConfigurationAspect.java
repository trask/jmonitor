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

package org.jmonitor.test.configuration;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jmonitor.configuration.impl.ConfigurationServiceImpl;
import org.jmonitor.configuration.service.model.ProbeConfiguration;
import org.jmonitor.configuration.shared.ConfigurationImplHelper;
import org.jmonitor.configuration.shared.model.ProbeConfigurationImpl;
import org.jmonitor.test.configuration.ConfigurationAspect;
import org.jmonitor.test.configuration.ConfigureServletProbeUsernameSessionAttribute;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO move to src/test/aspect?
@Aspect
public class ServletProbeConfigurationAspect {

    @Around("execution(@ConfigureProbeServletUsernameSessionAttribute void org.jmonitor..*Test.*())")
    public void aroundConfigureProbeServletUsernameSessionAttributePointcut(
            ProceedingJoinPoint joinPoint) throws Throwable {

        ConfigureServletProbeUsernameSessionAttribute annotation =
                ConfigurationAspect.getAnnotation(ConfigureServletProbeUsernameSessionAttribute.class, joinPoint);
        ProbeConfigurationImpl mutableConfiguration = getMutableProbeConfiguration();
        String previousValue =
                mutableConfiguration.getProperty("servlet", "usernameSessionAttribute");
        // set it to annotated value
        mutableConfiguration.setProperty("servlet", "usernameSessionAttribute", annotation.value());
        ConfigurationServiceImpl.getInstance().updateProbeConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setProperty("servlet", "usernameSessionAttribute", previousValue);
            ConfigurationServiceImpl.getInstance().updateProbeConfiguration(mutableConfiguration);
        }
    }

    private ProbeConfigurationImpl getMutableProbeConfiguration() {
        ProbeConfiguration configuration =
                ConfigurationServiceImpl.getInstance().getProbeConfiguration();
        return ConfigurationImplHelper.copyOf(configuration);
    }
}
