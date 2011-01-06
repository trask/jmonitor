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

import org.jmonitor.agent.configuration.AgentConfiguration;

import com.google.gwt.core.client.GWT;
import com.pietschy.gwt.pectin.client.bean.BeanModelProvider;
import com.pietschy.gwt.pectin.client.form.FieldModel;
import com.pietschy.gwt.pectin.client.form.FormModel;
import com.pietschy.gwt.pectin.client.form.FormattedFieldModel;
import com.pietschy.gwt.pectin.client.format.IntegerFormat;

/**
 * 
 * 
 * @author Trask Stalnaker
 * @since 1.0
 */
public class AgentConfigurationFormModel extends FormModel {

    public abstract static class AgentConfigurationProvider extends
            BeanModelProvider<AgentConfiguration> {
    }

    private final AgentConfigurationProvider configurationProvider =
            GWT.create(AgentConfigurationProvider.class);

    private final FieldModel<Boolean> enabled;
    private final FormattedFieldModel<Integer> thresholdMillis;
    private final FormattedFieldModel<Integer> stuckThresholdMillis;
    private final FormattedFieldModel<Integer> stackTraceInitialDelayMillis;
    private final FormattedFieldModel<Integer> stackTracePeriodMillis;
    private final FormattedFieldModel<Integer> maxTraceEventsPerOperation;

    public AgentConfigurationFormModel() {

        enabled = fieldOfType(Boolean.class).boundTo(configurationProvider, "enabled");
        thresholdMillis =
                formattedFieldOfType(Integer.class).using(new IntegerFormat()).boundTo(
                        configurationProvider, "thresholdMillis");
        stuckThresholdMillis =
                formattedFieldOfType(Integer.class).using(new IntegerFormat()).boundTo(
                        configurationProvider, "stuckThresholdMillis");
        stackTraceInitialDelayMillis =
                formattedFieldOfType(Integer.class).using(new IntegerFormat()).boundTo(
                        configurationProvider, "stackTraceInitialDelayMillis");
        stackTracePeriodMillis =
                formattedFieldOfType(Integer.class).using(new IntegerFormat()).boundTo(
                        configurationProvider, "stackTracePeriodMillis");
        maxTraceEventsPerOperation =
                formattedFieldOfType(Integer.class).using(new IntegerFormat()).boundTo(
                        configurationProvider, "maxTraceEventsPerOperation");
    }

    public FieldModel<Boolean> isEnabled() {
        return enabled;
    }

    public FormattedFieldModel<Integer> getThresholdMillis() {
        return thresholdMillis;
    }

    public FormattedFieldModel<Integer> getStuckThresholdMillis() {
        return stuckThresholdMillis;
    }

    public FormattedFieldModel<Integer> getStackTraceInitialDelayMillis() {
        return stackTraceInitialDelayMillis;
    }

    public FormattedFieldModel<Integer> getStackTracePeriodMillis() {
        return stackTracePeriodMillis;
    }

    public FormattedFieldModel<Integer> getMaxTraceEventsPerOperation() {
        return maxTraceEventsPerOperation;
    }

    public void setBean(AgentConfiguration configuration) {
        configurationProvider.setValue(configuration);
    }

    public void commit() {
        configurationProvider.commit();
    }
}
