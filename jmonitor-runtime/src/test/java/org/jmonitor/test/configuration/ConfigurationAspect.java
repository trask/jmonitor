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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jmonitor.configuration.impl.ConfigurationServiceImpl;
import org.jmonitor.configuration.service.model.AgentConfiguration;
import org.jmonitor.configuration.service.model.CollectorConfiguration;
import org.jmonitor.configuration.shared.ConfigurationImplHelper;
import org.jmonitor.configuration.shared.model.AgentConfigurationImpl;
import org.jmonitor.configuration.shared.model.CollectorConfigurationImpl;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO move to src/test/aspect?
// this is kind of a lot of work, but it sure makes the tests look nicer
@Aspect
public class ConfigurationAspect {

    @Around("execution(@ConfigureAgentEnabled void org.jmonitor..*Test.*())")
    public void aroundConfigureAgentEnabledPointcut(ProceedingJoinPoint joinPoint) throws Throwable {

        ConfigureAgentEnabled annotation = getAnnotation(ConfigureAgentEnabled.class, joinPoint);
        AgentConfigurationImpl mutableConfiguration = getMutableAgentConfiguration();
        boolean previousValue = mutableConfiguration.isEnabled();
        // set it to annotated value
        mutableConfiguration.setEnabled(annotation.value());
        ConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setEnabled(previousValue);
            ConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        }
    }

    @Around("execution(@ConfigureAgentStackTraceInitialDelayMillis void org.jmonitor..*Test.*())")
    public void aroundConfigureAgentStackTraceInitialDelayMillisPointcut(
            ProceedingJoinPoint joinPoint) throws Throwable {

        ConfigureAgentStackTraceInitialDelayMillis annotation =
                getAnnotation(ConfigureAgentStackTraceInitialDelayMillis.class, joinPoint);
        AgentConfigurationImpl mutableConfiguration = getMutableAgentConfiguration();
        int previousValue = mutableConfiguration.getStackTraceInitialDelayMillis();
        // set it to annotated value
        mutableConfiguration.setStackTraceInitialDelayMillis(annotation.value());
        ConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setStackTraceInitialDelayMillis(previousValue);
            ConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        }
    }

    @Around("execution(@ConfigureAgentStackTracePeriodMillis void org.jmonitor..*Test.*())")
    public void aroundConfigureConfigureAgentStackTracePeriodMillisPointcut(
            ProceedingJoinPoint joinPoint) throws Throwable {

        ConfigureAgentStackTracePeriodMillis annotation =
                getAnnotation(ConfigureAgentStackTracePeriodMillis.class, joinPoint);
        AgentConfigurationImpl mutableConfiguration = getMutableAgentConfiguration();
        int previousValue = mutableConfiguration.getStackTracePeriodMillis();
        // set it to annotated value
        mutableConfiguration.setStackTracePeriodMillis(annotation.value());
        ConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setStackTracePeriodMillis(previousValue);
            ConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        }
    }

    @Around("execution(@ConfigureAgentStuckThresholdMillis void org.jmonitor..*Test.*())")
    public void aroundConfigureAgentStuckThresholdMillisPointcut(ProceedingJoinPoint joinPoint)
            throws Throwable {

        ConfigureAgentStuckThresholdMillis annotation =
                getAnnotation(ConfigureAgentStuckThresholdMillis.class, joinPoint);
        AgentConfigurationImpl mutableConfiguration = getMutableAgentConfiguration();
        int previousValue = mutableConfiguration.getStuckThresholdMillis();
        // set it to annotated value
        mutableConfiguration.setStuckThresholdMillis(annotation.value());
        ConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setStuckThresholdMillis(previousValue);
            ConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        }
    }

    @Around("execution(@ConfigureAgentThresholdMillis void org.jmonitor..*Test.*())")
    public void aroundConfigureAgentThresholdMillisPointcut(ProceedingJoinPoint joinPoint)
            throws Throwable {

        ConfigureAgentThresholdMillis annotation =
                getAnnotation(ConfigureAgentThresholdMillis.class, joinPoint);
        AgentConfigurationImpl mutableConfiguration = getMutableAgentConfiguration();
        int previousValue = mutableConfiguration.getThresholdMillis();
        // set it to annotated value
        mutableConfiguration.setThresholdMillis(annotation.value());
        ConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setThresholdMillis(previousValue);
            ConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        }
    }

    @Around("execution(@ConfigureCollectorLogActiveFilename void org.jmonitor..*Test.*())")
    public void aroundConfigureCollectorLogActiveFilenamePointcut(ProceedingJoinPoint joinPoint)
            throws Throwable {

        ConfigureCollectorLogActiveFilename annotation =
                getAnnotation(ConfigureCollectorLogActiveFilename.class, joinPoint);
        CollectorConfigurationImpl mutableConfiguration = getMutableCollectorConfiguration();
        String previousValue = mutableConfiguration.getLogActiveFilename();
        // set it to annotated value
        mutableConfiguration.setLogActiveFilename(annotation.value());
        ConfigurationServiceImpl.getInstance().updateCollectorConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setLogActiveFilename(previousValue);
            ConfigurationServiceImpl.getInstance().updateCollectorConfiguration(
                    mutableConfiguration);
        }
    }

    private AgentConfigurationImpl getMutableAgentConfiguration() {
        AgentConfiguration configuration =
                ConfigurationServiceImpl.getInstance().getAgentConfiguration();
        return ConfigurationImplHelper.copyOf(configuration);
    }

    private CollectorConfigurationImpl getMutableCollectorConfiguration() {
        CollectorConfiguration configuration =
                ConfigurationServiceImpl.getInstance().getCollectorConfiguration();
        return ConfigurationImplHelper.copyOf(configuration);
    }

    public static <T extends Annotation> T getAnnotation(Class<T> annotationClass,
            ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return method.getAnnotation(annotationClass);
    }
}
