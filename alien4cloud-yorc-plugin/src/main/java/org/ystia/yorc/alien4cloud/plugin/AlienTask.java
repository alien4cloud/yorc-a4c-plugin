/**
 * Copyright 2018 Bull S.A.S. Atos Technologies - Bull, Rue Jean Jaures, B.P.68, 78340, Les Clayes-sous-Bois, France.
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
package org.ystia.yorc.alien4cloud.plugin;


import org.ystia.yorc.alien4cloud.plugin.rest.RestClient;

public abstract class AlienTask {

    protected YorcPaaSProvider orchestrator;
    protected RestClient restClient;

    /**
     * Constructor
     * @param  provider
     */
    public AlienTask(YorcPaaSProvider provider) {
        this.orchestrator = provider;
        this.restClient = provider.getRestClient();
    }

    /**
     * Run this task
     */
    public abstract void run();
}