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
package org.jmonitor.ui.client.configuration.model;

import org.jmonitor.configuration.shared.model.ProbeConfigurationImpl;

import com.google.gwt.core.client.GWT;
import com.pietschy.gwt.pectin.client.bean.BeanModelProvider;
import com.pietschy.gwt.pectin.client.form.FormModel;

/**
 * @author Trask Stalnaker
 * @since 1.0
 */
// TODO expose probe configuration via advanced configuration GUI
public class ProbeConfigurationFormModel extends FormModel {

    public static abstract class ProbeConfigurationProvider extends
            BeanModelProvider<ProbeConfigurationImpl> {}

    private ProbeConfigurationProvider configurationProvider =
            GWT.create(ProbeConfigurationProvider.class);

    public ProbeConfigurationFormModel() {}

    public void setValue(ProbeConfigurationImpl configuration) {
        configurationProvider.setValue(configuration);
    }

    public void commit() {
        configurationProvider.commit();
    }
}
