/**
 * detect-configuration
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.detect.hub;

import java.util.Map;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.integration.hub.detect.configuration.ConnectionManager;
import com.blackducksoftware.integration.hub.detect.configuration.DetectConfiguration;
import com.blackducksoftware.integration.hub.detect.configuration.DetectProperty;
import com.blackducksoftware.integration.hub.detect.configuration.PropertyAuthority;
import com.blackducksoftware.integration.hub.detect.exception.DetectUserFriendlyException;
import com.blackducksoftware.integration.hub.detect.exitcode.ExitCodeType;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.synopsys.integration.blackduck.api.generated.discovery.ApiDiscovery;
import com.synopsys.integration.blackduck.api.generated.response.CurrentVersionView;
import com.synopsys.integration.blackduck.configuration.HubServerConfig;
import com.synopsys.integration.blackduck.configuration.HubServerConfigBuilder;
import com.synopsys.integration.blackduck.rest.BlackduckRestConnection;
import com.synopsys.integration.blackduck.service.BinaryScannerService;
import com.synopsys.integration.blackduck.service.CodeLocationService;
import com.synopsys.integration.blackduck.service.HubRegistrationService;
import com.synopsys.integration.blackduck.service.HubService;
import com.synopsys.integration.blackduck.service.HubServicesFactory;
import com.synopsys.integration.blackduck.service.ProjectService;
import com.synopsys.integration.blackduck.service.ReportService;
import com.synopsys.integration.blackduck.service.ScanStatusService;
import com.synopsys.integration.exception.EncryptionException;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;
import com.synopsys.integration.phonehome.PhoneHomeClient;
import com.synopsys.integration.phonehome.PhoneHomeService;
import com.synopsys.integration.rest.connection.RestConnection;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.ResourceUtil;

public class HubServiceManager {
    private final Logger logger = LoggerFactory.getLogger(HubServiceManager.class);

    private final DetectConfiguration detectConfiguration;
    private final ConnectionManager connectionManager;
    private final Gson gson;
    private final JsonParser jsonParser;

    private Slf4jIntLogger slf4jIntLogger = new Slf4jIntLogger(logger);
    private HubServerConfig hubServerConfig;
    private HubServicesFactory hubServicesFactory;

    public HubServiceManager(final DetectConfiguration detectConfiguration, final ConnectionManager connectionManager, final Gson gson, final JsonParser jsonParser) {
        this.detectConfiguration = detectConfiguration;
        this.connectionManager = connectionManager;
        this.gson = gson;
        this.jsonParser = jsonParser;
    }

    public void init() throws IntegrationException, DetectUserFriendlyException {
        try {
            hubServerConfig = createHubServerConfig(slf4jIntLogger);
            hubServicesFactory = createHubServicesFactory(slf4jIntLogger, hubServerConfig);
        } catch (IllegalStateException | EncryptionException e) {
            throw new DetectUserFriendlyException(String.format("Not able to process Black Duck connection: %s", e.getMessage()), e, ExitCodeType.FAILURE_HUB_CONNECTIVITY);
        }
        final HubService hubService = createHubService();
        final CurrentVersionView currentVersion = hubService.getResponse(ApiDiscovery.CURRENT_VERSION_LINK_RESPONSE);
        logger.info(String.format("Successfully connected to BlackDuck (version %s)!", currentVersion.version));
    }

    public boolean testHubConnection(final IntLogger intLogger) {
        try {
            assertHubConnection(intLogger);
            return true;
        } catch (final IntegrationException e) {
            intLogger.error(String.format("Could not reach the Black Duck server or the credentials were invalid: %s", e.getMessage()), e);
        }
        return false;
    }

    public void assertHubConnection(final IntLogger intLogger) throws IntegrationException {
        logger.info("Attempting connection to the Black Duck server");
        RestConnection connection = null;

        try {
            final HubServerConfig hubServerConfig = createHubServerConfig(intLogger);
            connection = hubServerConfig.createRestConnection(intLogger);
            connection.connect();
            logger.info("Connection to the Black Duck server was successful");
        } catch (final IllegalStateException e) {
            throw new IntegrationException(e.getMessage(), e);
        } finally {
            ResourceUtil.closeQuietly(connection);
        }
    }

    public BinaryScannerService createBinaryScannerService() {
        return hubServicesFactory.createBinaryScannerService();
    }

    public HubService createHubService() {
        return hubServicesFactory.createHubService();
    }

    public HubRegistrationService createHubRegistrationService() {
        return hubServicesFactory.createHubRegistrationService();
    }

    public ProjectService createProjectService() {
        return hubServicesFactory.createProjectService();
    }

    public PhoneHomeService createPhoneHomeService() {
        return hubServicesFactory.createPhoneHomeService(Executors.newSingleThreadExecutor());
    }

    public CodeLocationService createCodeLocationService() {
        return hubServicesFactory.createCodeLocationService();
    }

    public ScanStatusService createScanStatusService() {
        return hubServicesFactory.createScanStatusService(detectConfiguration.getLongProperty(DetectProperty.DETECT_API_TIMEOUT, PropertyAuthority.None));
    }

    public ReportService createReportService() throws IntegrationException {
        return hubServicesFactory.createReportService(detectConfiguration.getLongProperty(DetectProperty.DETECT_API_TIMEOUT, PropertyAuthority.None));
    }

    private HubServicesFactory createHubServicesFactory(final IntLogger slf4jIntLogger, final HubServerConfig hubServerConfig) throws IntegrationException {
        final BlackduckRestConnection restConnection = hubServerConfig.createRestConnection(slf4jIntLogger);

        return new HubServicesFactory(gson, jsonParser, restConnection, slf4jIntLogger);
    }

    private HubServerConfig createHubServerConfig(final IntLogger slf4jIntLogger) {
        final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
        hubServerConfigBuilder.setLogger(slf4jIntLogger);

        final Map<String, String> blackduckHubProperties = detectConfiguration.getBlackduckProperties();
        hubServerConfigBuilder.setFromProperties(blackduckHubProperties);

        return hubServerConfigBuilder.build();
    }

    public HubServerConfig getHubServerConfig() {
        return hubServerConfig;
    }

    public HubServicesFactory getHubServicesFactory() {
        return hubServicesFactory;
    }

    public PhoneHomeClient createPhoneHomeClient() {
        return hubServicesFactory.createPhoneHomeClient();
    }

    public IntEnvironmentVariables getEnvironmentVariables() {
        try {
            return (IntEnvironmentVariables) HubServicesFactory.class.getDeclaredField("intEnvironmentVariables").get(hubServicesFactory);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return new IntEnvironmentVariables();
        }
    }
}
