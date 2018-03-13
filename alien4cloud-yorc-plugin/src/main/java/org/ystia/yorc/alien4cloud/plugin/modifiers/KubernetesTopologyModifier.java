package org.ystia.yorc.alien4cloud.plugin.modifiers;

import lombok.extern.java.Log;

import org.springframework.stereotype.Component;

import alien4cloud.paas.wf.validation.WorkflowValidator;
import alien4cloud.tosca.context.ToscaContextual;

import org.alien4cloud.tosca.utils.TopologyNavigationUtil;

import org.alien4cloud.tosca.model.templates.Topology;
import org.alien4cloud.tosca.model.Csar;
import org.alien4cloud.tosca.model.CSARDependency;
import org.alien4cloud.tosca.model.templates.NodeTemplate;


import org.alien4cloud.alm.deployment.configuration.flow.FlowExecutionContext;
import org.alien4cloud.alm.deployment.configuration.flow.TopologyModifierSupport;
import org.alien4cloud.alm.deployment.configuration.flow.ITopologyModifier;

import alien4cloud.component.ICSARRepositorySearchService;



import javax.annotation.Resource;
import java.util.Set;

/**
 * Created by danesa on 02/03/18.
 *
 */
@Log
@Component(value = KubernetesTopologyModifier.YORC_KUBERNETES_MODIFIER_TAG)
public class KubernetesTopologyModifier extends TopologyModifierSupport {

    public static final String YORC_KUBERNETES_MODIFIER_TAG = "yorc-kubernetes-modifier";

    /**
     * The below constants' values come from kubernetes resource types definitions:
     * - template_name
     * - template_version
     */
    protected static final String YORC_KUBERNETES_TYPES_ARCHIVE_NAME = "yorc-kubernetes-types";
    protected static final String YORC_KUBERNETES_TYPES_ARCHIVE_VERSION = "1.0.0-SNAPSHOT";

    // A4C K8S resource types defined in org.alien4cloud.plugin.kubernetes.modifier
    public static final String K8S_TYPES_DEPLOYMENT_RESOURCE = "org.alien4cloud.kubernetes.api.types.DeploymentResource";
    public static final String K8S_TYPES_SERVICE_RESOURCE = "org.alien4cloud.kubernetes.api.types.ServiceResource";
    // Yorc K8S resource types
    protected static final String YORC_KUBERNETES_TYPES_DEPLOYMENT_RESOURCE = "yorc.nodes.kubernetes.api.types.DeploymentResource";
    protected static final String YORC_KUBERNETES_TYPES_SERVICE_RESOURCE = "yorc.nodes.kubernetes.api.types.ServiceResource";


    @Resource(name="kubernetes-final-modifier")
    ITopologyModifier alien_kubernetes_modifier;

    @Resource
    protected ICSARRepositorySearchService csarRepoService;

    @Override
    @ToscaContextual
    public void process(Topology topology, FlowExecutionContext context) {

        /**
         * Transform a matched K8S topology containing <code>Container</code>s, <code>Deployment</code>s, <code>Service</code>s
         * and replace them with <code>DeploymentResource</code>s and <code>ServiceResource</code>s.
         */
        alien_kubernetes_modifier.process(topology, context);

        try {
            WorkflowValidator.disableValidationThreadLocal.set(true);
            doProcess(topology, context);
        } finally {
            WorkflowValidator.disableValidationThreadLocal.remove();
        }
    }

    private void doProcess(Topology topology, FlowExecutionContext context) {
        log.info("~~~~~~~~~ Yorc Plugin : Processing topology " + topology.getId());

        Csar csar = new Csar(topology.getArchiveName(), topology.getArchiveVersion());

        // Import yorc-kubernetes-types
        Csar yorcKubernetesTypesCsar = csarRepoService.getArchive(YORC_KUBERNETES_TYPES_ARCHIVE_NAME, YORC_KUBERNETES_TYPES_ARCHIVE_VERSION);
        if (yorcKubernetesTypesCsar == null) {
            // could not find the CSAR ... ?
            log.info("~~~~~~~~~ Yorc Plugin : could not find " + YORC_KUBERNETES_TYPES_ARCHIVE_NAME + ":" + YORC_KUBERNETES_TYPES_ARCHIVE_VERSION);
            return;
        }

        CSARDependency yorcKubernetesTypesDependency = new CSARDependency();
        yorcKubernetesTypesDependency.setName(YORC_KUBERNETES_TYPES_ARCHIVE_NAME);
        yorcKubernetesTypesDependency.setVersion(YORC_KUBERNETES_TYPES_ARCHIVE_VERSION);
        yorcKubernetesTypesDependency.setHash(yorcKubernetesTypesCsar.getDefinitionHash());

        topology.getDependencies().add(yorcKubernetesTypesDependency);
        log.info("~~~~~~~~~ Yorc Plugin : Yorc kubernetes types archive added as dependency to the topology");

        // Transform alien kubernetes resource types to yorc kubernetes resource types
        //
        // Treat service resource types
        transformKubernetesResourceTypes(topology,  csar, "service", YORC_KUBERNETES_TYPES_ARCHIVE_VERSION);

        // Treat deployment resource types
        transformKubernetesResourceTypes(topology,  csar, "deployment", YORC_KUBERNETES_TYPES_ARCHIVE_VERSION);


        log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ");

    }

    private void transformKubernetesResourceTypes(Topology topology, Csar csar, String resourceType, String resourceArchiveVersion) {
        String sourceResourceType = null;
        String targetResourceType = null;

        switch (resourceType) {
            case "service" :
                sourceResourceType = K8S_TYPES_SERVICE_RESOURCE;
                targetResourceType = YORC_KUBERNETES_TYPES_SERVICE_RESOURCE;
                break;
            case "deployment" :
                sourceResourceType = K8S_TYPES_DEPLOYMENT_RESOURCE;
                targetResourceType = YORC_KUBERNETES_TYPES_DEPLOYMENT_RESOURCE;
                break;
            default:
                log.info("~~~~~~~~~ Yorc Plugin : currently supported K8S resources are " + "service" + " and " + "deployment");
                break;
        }

        if (sourceResourceType == null || targetResourceType == null) {
            return;
        }

        final String effectiveTargetResourceType = targetResourceType;

        Set<NodeTemplate> serviceNodes = TopologyNavigationUtil.getNodesOfType(topology, sourceResourceType, false);

        serviceNodes.forEach(serviceNodeTemplate -> {

            NodeTemplate yorcServiceNodeTemplate = replaceNode(csar, topology, serviceNodeTemplate, effectiveTargetResourceType, resourceArchiveVersion);

            log.info("~~~~~~~~~ Yorc Plugin : >>> service resource node " + yorcServiceNodeTemplate.getName() + " now has type " + yorcServiceNodeTemplate.getType());
        });
    }

}
