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

package org.jmonitor.mock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.DeclarePrecedence;
import org.aspectj.lang.annotation.Pointcut;
import org.jmonitor.api.probe.ProbeExecution;
import org.jmonitor.api.probe.ProbeExecutionCreator;
import org.jmonitor.api.probe.ProbeExecutionManagerFactory;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO move to src/test/aspect?
@Aspect
@DeclarePrecedence("org.jmonitor.test.configuration.ConfigurationAspect, org.jmonitor.mock.MockProbe")
public class MockProbe {

    @Pointcut("execution(@org.jmonitor.mock.WrapInMockProbeExecution void org.jmonitor..*Test.*())")
    void testPointcut() {
    }

    @Around("testPointcut()")
    public void aroundTestPointcut(ProceedingJoinPoint joinPoint) throws Throwable {

        ProbeExecutionCreator probeExecutionCreator = new ProbeExecutionCreator() {
            public ProbeExecution createProbeExecution() {
                return new MockProbeExecution();
            }
        };

        ProbeExecutionManagerFactory.getManager().execute(probeExecutionCreator, joinPoint);
    }
}
