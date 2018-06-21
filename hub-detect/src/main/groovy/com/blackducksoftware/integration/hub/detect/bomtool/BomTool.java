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
package com.blackducksoftware.integration.hub.detect.bomtool;

import com.blackducksoftware.integration.hub.detect.bomtool.result.BomToolResult;
import com.blackducksoftware.integration.hub.detect.evaluation.BomToolEnvironment;
import com.blackducksoftware.integration.hub.detect.evaluation.BomToolException;
import com.blackducksoftware.integration.hub.detect.extraction.model.Extraction;
import com.blackducksoftware.integration.hub.detect.model.BomToolGroupType;

public abstract class BomTool {
    protected BomToolEnvironment environment;

    public BomTool(final BomToolEnvironment environment) {
        this.environment = environment;
    }

    //Applicable should be light-weight and should never throw an exception. Look for files, check properties, short and sweet.
    public abstract BomToolResult applicable();

    //Extractable may be as heavy as needed, and may (and sometimes should) fail. Make web requests, install inspectors or run executables.
    public abstract BomToolResult extractable() throws BomToolException;

    //Perform the extraction.
    public abstract Extraction extract(ExtractionId extractionId);

    public abstract String getName();
    public abstract BomToolGroupType getBomToolGroupType();
    public abstract BomToolType getBomToolType();

    public String getDescriptiveName() {
        return getBomToolGroupType().toString() + " - " + getName();
    }
}