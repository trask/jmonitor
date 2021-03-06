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
import org.jmonitor.agent.configuration.AgentConfiguration;
import org.jmonitor.collector.service.model.CollectorConfiguration;

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
        AgentConfiguration mutableConfiguration = getMutableAgentConfiguration();
        boolean previousValue = mutableConfiguration.isEnabled();
        // set it to annotated value
        mutableConfiguration.setEnabled(annotation.value());
        AgentConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setEnabled(previousValue);
            AgentConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        }
    }

    @Around("execution(@ConfigureAgentStackTraceInitialDelayMillis void org.jmonitor..*Test.*())")
    public void aroundConfigureAgentStackTraceInitialDelayMillisPointcut(
            ProceedingJoinPoint joinPoint) throws Throwable {

        ConfigureAgentStackTraceInitialDelayMillis annotation =
                getAnnotation(ConfigureAgentStackTraceInitialDelayMillis.class, joinPoint);
        AgentConfiguration mutableConfiguration = getMutableAgentConfiguration();
        int previousValue = mutableConfiguration.getStackTraceInitialDelayMillis();
        // set it to annotated value
        mutableConfiguration.setStackTraceInitialDelayMillis(annotation.value());
        AgentConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setStackTraceInitialDelayMillis(previousValue);
            AgentConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        }
    }

    @Around("execution(@ConfigureAgentStackTracePeriodMillis void org.jmonitor..*Test.*())")
    public void aroundConfigureConfigureAgentStackTracePeriodMillisPointcut(
            ProceedingJoinPoint joinPoint) throws Throwable {

        ConfigureAgentStackTracePeriodMillis annotation =
                getAnnotation(ConfigureAgentStackTracePeriodMillis.class, joinPoint);
        AgentConfiguration mutableConfiguration = getMutableAgentConfiguration();
        int previousValue = mutableConfiguration.getStackTracePeriodMillis();
        // set it to annotated value
        mutableConfiguration.setStackTracePeriodMillis(annotation.value());
        AgentConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setStackTracePeriodMillis(previousValue);
            AgentConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        }
    }

    @Around("execution(@ConfigureAgentStuckThresholdMillis void org.jmonitor..*Test.*())")
    public void aroundConfigureAgentStuckThresholdMillisPointcut(ProceedingJoinPoint joinPoint)
            throws Throwable {

        ConfigureAgentStuckThresholdMillis annotation =
                getAnnotation(ConfigureAgentStuckThresholdMillis.class, joinPoint);
        AgentConfiguration mutableConfiguration = getMutableAgentConfiguration();
        int previousValue = mutableConfiguration.getStuckThresholdMillis();
        // set it to annotated value
        mutableConfiguration.setStuckThresholdMillis(annotation.value());
        AgentConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setStuckThresholdMillis(previousValue);
            AgentConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        }
    }

    @Around("execution(@ConfigureAgentThresholdMillis void org.jmonitor..*Test.*())")
    public void aroundConfigureAgentThresholdMillisPointcut(ProceedingJoinPoint joinPoint)
            throws Throwable {

        ConfigureAgentThresholdMillis annotation =
                getAnnotation(ConfigureAgentThresholdMillis.class, joinPoint);
        AgentConfiguration mutableConfiguration = getMutableAgentConfiguration();
        int previousValue = mutableConfiguration.getThresholdMillis();
        // set it to annotated value
        mutableConfiguration.setThresholdMillis(annotation.value());
        AgentConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setThresholdMillis(previousValue);
            AgentConfigurationServiceImpl.getInstance().updateAgentConfiguration(mutableConfiguration);
        }
    }

    @Around("execution(@ConfigureCollectorLogActiveFilename void org.jmonitor..*Test.*())")
    public void aroundConfigureCollectorLogActiveFilenamePointcut(ProceedingJoinPoint joinPoint)
            throws Throwable {

        ConfigureCollectorLogActiveFilename annotation =
                getAnnotation(ConfigureCollectorLogActiveFilename.class, joinPoint);
        CollectorConfiguration mutableConfiguration = getMutableCollectorConfiguration();
        String previousValue = mutableConfiguration.getLogActiveFilename();
        // set it to annotated value
        mutableConfiguration.setLogActiveFilename(annotation.value());
        CollectorConfigurationServiceImpl.getInstance().updateCollectorConfiguration(mutableConfiguration);
        try {
            joinPoint.proceed();
        } finally {
            // set it back to original value
            mutableConfiguration.setLogActiveFilename(previousValue);
            CollectorConfigurationServiceImpl.getInstance().updateCollectorConfiguration(
                    mutableConfiguration);
        }
    }

    private AgentConfiguration getMutableAgentConfiguration() {
        AgentConfiguration configuration =
                AgentConfigurationServiceImpl.getInstance().getAgentConfiguration();
        return AgentConfigurationHelper.copyOf(configuration);
    }

    private CollectorConfiguration getMutableCollectorConfiguration() {
        CollectorConfiguration configuration =
                CollectorConfigurationServiceImpl.getInstance().getCollectorConfiguration();
        return AgentConfigurationHelper.copyOf(configuration);
    }

    public static <T extends Annotation> T getAnnotation(Class<T> annotationClass,
            ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return method.getAnnotation(annotationClass);
    }
}
