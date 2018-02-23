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
package org.ystia.yorc.alien4cloud.plugin.modifiers;

import alien4cloud.paas.wf.validation.WorkflowValidator;
import alien4cloud.tosca.context.ToscaContextual;
import lombok.extern.java.Log;
import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.TopologyModifierSupport;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.definitions.AbstractPropertyValue;
import org.alien4cloud.tosca.model.templates.Capability;
import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.utils.TopologyNavigationUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Transform a matched K8S topology containing <code>Container</code>s, <code>Deployment</code>s, <code>Service</code>s
 * and replace them with <code>DeploymentResource</code>s and <code>ServiceResource</code>s.
 * <p>
 * TODO: add logs using FlowExecutionContext
 */
@Log
@Component(value = FipTopologyModifier.YORC_OPENSTACK_FIP_MODIFIER_TAG)
public class FipTopologyModifier extends TopologyModifierSupport {

    public static final String YORC_OPENSTACK_FIP_MODIFIER_TAG = "yorc-openstack-fip-modifier";

    @Override
    @ToscaContextual
    public void process(Topology topology, FlowExecutionContext context) {
        log.info("Processing topology " + topology.getId());

        try {
            WorkflowValidator.disableValidationThreadLocal.set(true);
            doProcess(topology, context);
        } finally {
            WorkflowValidator.disableValidationThreadLocal.remove();
        }
    }

    private void doProcess(Topology topology, FlowExecutionContext context) {
        Csar csar = new Csar(topology.getArchiveName(), topology.getArchiveVersion());

        // for each Service create a node of type ServiceResource
        Set<NodeTemplate> publicNetworksNodes = TopologyNavigationUtil.getNodesOfType(topology, "yorc.nodes.openstack.PublicNetwork", false);

        publicNetworksNodes.forEach(newtworkNodeTemplate -> {
            final AbstractPropertyValue networkName = newtworkNodeTemplate.getProperties().get("floating_network_name");

            for (NodeTemplate nodeTemplate : new ArrayList<>(topology.getNodeTemplates().values())) {
                if (nodeTemplate.getRelationships() == null) continue;
                nodeTemplate.getRelationships().forEach((rel, relationshipTemplate) -> {
                    if (relationshipTemplate.getTarget().equals(newtworkNodeTemplate.getName())) {

                        Map<String, AbstractPropertyValue> properties = new LinkedHashMap<>();
                        properties.put("floating_network_name", networkName);

                        Map<String, Capability> capabilities = new LinkedHashMap<>();
                        Capability connectionCap = new Capability();
                        connectionCap.setType("yorc.capabilities.openstack.FIPConnectivity");
                        capabilities.put("connection", connectionCap);

                        String fipName = "FIP" + nodeTemplate.getName();

                        NodeTemplate nt = addNodeTemplate(csar, topology, fipName, "yorc.nodes.openstack.FloatingIP", "1.0.0");
                        nt.setProperties(properties);
                        nt.setCapabilities(capabilities);

                        relationshipTemplate.setTarget(fipName);
                        relationshipTemplate.setRequirementType("yorc.capabilities.openstack.FIPConnectivity");
                    }
                });
            }
        });
        publicNetworksNodes.forEach(pnn -> removeNode(topology, pnn));
    }
}
