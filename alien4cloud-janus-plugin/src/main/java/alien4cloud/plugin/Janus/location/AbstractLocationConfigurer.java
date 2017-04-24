/*
* Copyright 2016 Bull Atos.  All Rights Reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
* See the NOTICE file distributed with this work for additional information
* regarding copyright ownership.
*/
package alien4cloud.plugin.Janus.location;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import alien4cloud.common.AlienConstants;
import alien4cloud.deployment.matching.services.nodes.MatchingConfigurations;
import alien4cloud.deployment.matching.services.nodes.MatchingConfigurationsParser;
import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.orchestrators.locations.services.LocationResourceGeneratorService;
import alien4cloud.orchestrators.plugin.ILocationConfiguratorPlugin;
import alien4cloud.orchestrators.plugin.model.PluginArchive;
import alien4cloud.paas.exception.PluginParseException;
import alien4cloud.plugin.Janus.service.PluginArchiveService;
import alien4cloud.plugin.PluginManager;
import alien4cloud.plugin.model.ManagedPlugin;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.alien4cloud.tosca.catalog.ArchiveParser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Configure resources for the location type.
 */
@Slf4j
@Component
@Scope("prototype")
public abstract class AbstractLocationConfigurer implements ILocationConfiguratorPlugin {

    @Inject
    protected ArchiveParser archiveParser;
    @Inject
    protected MatchingConfigurationsParser matchingConfigurationsParser;
    @Inject
    protected PluginManager pluginManager;
    @Inject
    protected ManagedPlugin selfContext;
    @Inject
    protected LocationResourceGeneratorService resourceGeneratorService;
    protected List<PluginArchive> archives;

    @Inject
     private PluginArchiveService archiveService;

    @Override
    public List<PluginArchive> pluginArchives() throws PluginParseException {
        if (this.archives == null) {
                    parseLocationArchives(getLocationArchivePaths());
                }
                return this.archives;
    }

    protected void addToAchive(List<PluginArchive> archives, String path) throws ParsingException {
        Path archivePath = selfContext.getPluginPath().resolve(path);
        // Parse the archives
        ParsingResult<ArchiveRoot> result = archiveParser.parseDir(archivePath, AlienConstants.GLOBAL_WORKSPACE_ID);
        PluginArchive pluginArchive = new PluginArchive(result.getResult(), archivePath);
        archives.add(pluginArchive);
    }

    public List<String> getAllResourcesTypes() {
        List<String> resourcesTypes = Lists.newArrayList();
        for (PluginArchive pluginArchive : this.pluginArchives()) {
            for (String nodeType : pluginArchive.getArchive().getNodeTypes().keySet()) {
                resourcesTypes.add(nodeType);
            }
        }
        return resourcesTypes;
    }

    private void parseLocationArchives(String[] paths) {
        this.archives = Lists.newArrayList();
        for (String path : paths) {
            this.archives.add(archiveService.parsePluginArchives(path));
        }
    }


    public Map<String, MatchingConfiguration> getMatchingConfigurations(String matchingConfigRelativePath) {
        Path matchingConfigPath = selfContext.getPluginPath().resolve(matchingConfigRelativePath);
        MatchingConfigurations matchingConfigurations = null;
        try {
            matchingConfigurations = matchingConfigurationsParser.parseFile(matchingConfigPath).getResult();
        } catch (ParsingException e) {
            return Maps.newHashMap();
        }
        return matchingConfigurations.getMatchingConfigurations();
    }

    protected abstract String[] getLocationArchivePaths();
}
