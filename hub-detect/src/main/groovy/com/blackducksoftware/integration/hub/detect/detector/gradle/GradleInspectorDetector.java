/**
 * hub-detect
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
package com.blackducksoftware.integration.hub.detect.detector.gradle;

import java.io.File;

import com.blackducksoftware.integration.hub.detect.detector.Detector;
import com.blackducksoftware.integration.hub.detect.detector.DetectorEnvironment;
import com.blackducksoftware.integration.hub.detect.detector.DetectorException;
import com.blackducksoftware.integration.hub.detect.detector.DetectorType;
import com.blackducksoftware.integration.hub.detect.detector.ExtractionId;
import com.blackducksoftware.integration.hub.detect.workflow.extraction.Extraction;
import com.blackducksoftware.integration.hub.detect.workflow.file.DetectFileFinder;
import com.blackducksoftware.integration.hub.detect.workflow.search.result.DetectorResult;
import com.blackducksoftware.integration.hub.detect.workflow.search.result.ExecutableNotFoundDetectorResult;
import com.blackducksoftware.integration.hub.detect.workflow.search.result.FileNotFoundDetectorResult;
import com.blackducksoftware.integration.hub.detect.workflow.search.result.InspectorNotFoundDetectorResult;
import com.blackducksoftware.integration.hub.detect.workflow.search.result.PassedDetectorResult;

public class GradleInspectorDetector extends Detector {
    public static final String BUILD_GRADLE_FILENAME = "build.gradle";

    private final DetectFileFinder fileFinder;
    private final GradleExecutableFinder gradleFinder;
    private final GradleInspectorManager gradleInspectorManager;
    private final GradleInspectorExtractor gradleInspectorExtractor;

    private String gradleExe;
    private String gradleInspector;

    public GradleInspectorDetector(final DetectorEnvironment environment, final DetectFileFinder fileFinder, final GradleExecutableFinder gradleFinder, final GradleInspectorManager gradleInspectorManager,
        final GradleInspectorExtractor gradleInspectorExtractor) {
        super(environment, "Gradle Inspector", DetectorType.GRADLE);
        this.fileFinder = fileFinder;
        this.gradleFinder = gradleFinder;
        this.gradleInspectorManager = gradleInspectorManager;
        this.gradleInspectorExtractor = gradleInspectorExtractor;
    }

    @Override
    public DetectorResult applicable() {
        final File buildGradle = fileFinder.findFile(environment.getDirectory(), BUILD_GRADLE_FILENAME);
        if (buildGradle == null) {
            return new FileNotFoundDetectorResult(BUILD_GRADLE_FILENAME);
        }

        return new PassedDetectorResult();
    }

    @Override
    public DetectorResult extractable() throws DetectorException {
        gradleExe = gradleFinder.findGradle(environment);
        if (gradleExe == null) {
            return new ExecutableNotFoundDetectorResult("gradle");
        }

        gradleInspector = gradleInspectorManager.getGradleInspector();
        if (gradleInspector == null) {
            return new InspectorNotFoundDetectorResult("gradle");
        }

        return new PassedDetectorResult();
    }

    @Override
    public Extraction extract(final ExtractionId extractionId) {
        return gradleInspectorExtractor.extract(environment.getDirectory(), gradleExe, gradleInspector, extractionId);
    }

}
