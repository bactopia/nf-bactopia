/*
 * Copyright 2021, Seqera Labs
 * Modifications Copyright 2025, Robert A. Petit III
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
package bactopia.plugin

import groovy.json.JsonBuilder
import org.json.JSONObject
import org.json.JSONArray

import groovy.util.logging.Slf4j
import java.nio.file.Files
import java.nio.file.Path
import nextflow.Nextflow
import nextflow.plugin.extension.Function
import nextflow.plugin.extension.PluginExtensionPoint
import nextflow.script.WorkflowMetadata
import nextflow.Session

import bactopia.plugin.BactopiaConfig
import bactopia.plugin.BactopiaSchema
import bactopia.plugin.nfschema.HelpMessageCreator
import bactopia.plugin.nfschema.SummaryCreator

import static bactopia.plugin.inputs.Bactopia.collectBactopiaInputs
import static bactopia.plugin.inputs.BactopiaTools.collectBactopiaToolInputs
import static bactopia.plugin.BactopiaTemplate.dashedLine
import static bactopia.plugin.BactopiaTemplate.getLogColors
import static bactopia.plugin.BactopiaTemplate.getLogo
import static bactopia.plugin.BactopiaTemplate.getWorkflowSummary
import static bactopia.plugin.nfschema.Common.getLongestKeyLength

/**
 * Example plugin extension showing how to implement a basic
 * channel factory method, a channel operator and a custom function.
 *
 * @author Robert Petit <robbie.petit@gmail.com>
 * @author : jorge <jorge.aguilera@seqera.io>
 *
 */
@Slf4j
class BactopiaExtension extends PluginExtensionPoint {

    private Session session
    private BactopiaConfig config

    @Override
    protected void init(Session session) {
        this.session = session

        // Help message logic
        def Map params = (Map)session.params ?: [:]
        config = new BactopiaConfig(session?.config?.navigate('bactopia') as Map, params)
        
        // Initialize global log capture if not already done
        if (!BactopiaLoggerFactory.isInitialized()) {
            BactopiaLoggerFactory.initialize(config.monochromeLogs)
        }
    }

    //
    // Collect Bactopia inputs
    //
    @Function
    public Map bactopiaInputs(String runType) {
        def Map params = session.params
        def List samples = collectBactopiaInputs(params, runType)
        def Map logs = BactopiaLoggerFactory.captureAndClearLogs()
        return [
            hasErrors: logs.hasErrors,
            error: logs.error,
            logs: logs.logs,
            samples: samples
        ]
    }

    //
    // Collect Bactopia Tool inputs
    //
    @Function
    public Map bactopiaToolInputs(
        String bactopiaDir,
        String extension,
        String includeFile,
        String excludeFile
    ) {
        def List samples = collectBactopiaToolInputs(bactopiaDir, extension, includeFile, excludeFile)
        def Map logs = BactopiaLoggerFactory.captureAndClearLogs()
        return [
            hasErrors: logs.hasErrors,
            error: logs.error,
            logs: logs.logs,
            samples: samples
        ]
    }


    /*
    * Function to loop over all parameters defined in schema and check
    * whether the given parameters adhere to the specifications
    */
    @Function
    Map validateParameters(
        Map options = null,
        Boolean isBactopiaTool = false
    ) {
        def BactopiaSchema validator = new BactopiaSchema(config)
        def String result = validator.validateParameters(
            options,
            session.params,
            session.baseDir.toString(),
            isBactopiaTool
        )
        return BactopiaLoggerFactory.captureAndClearLogs(result)
    }
    
    /*
    * Get all captured logs from the plugin
    */
    @Function
    String getCapturedLogs(Boolean withColors = true) {
        // Get all global logs directly from the factory
        def Map result = BactopiaLoggerFactory.captureAndClearLogs("", withColors)
        return result.logs
    }
    
    /*
    * Clear all captured logs
    */
    @Function
    void clearCapturedLogs() {
        BactopiaLoggerFactory.clearLogs()
    }
}
