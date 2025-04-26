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
package nextflow.bactopia

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

import nextflow.bactopia.BactopiaConfig
import nextflow.bactopia.BactopiaSchema
import nextflow.bactopia.nfschema.SummaryCreator

import static nextflow.bactopia.inputs.BactopiaTools.collectInputs
import static nextflow.bactopia.BactopiaTemplate.getLogColors
import static nextflow.bactopia.BactopiaTemplate.getLogo
import static nextflow.bactopia.nfschema.Common.getLongestKeyLength

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
    }


    //
    // Collect Bactopia Tool inputs
    //
    @Function
    public List bactopiaToolInputs(
        String bactopiaDir,
        String extension,
        String includeFile,
        String excludeFile
    ) {
        return collectInputs(bactopiaDir, extension, includeFile, excludeFile)
    }


    //
    // Groovy Map summarising parameters/workflow options used by the pipeline
    //
    @Function
    public Map paramsSummaryMap(
        Map options = null,
        WorkflowMetadata workflow
        ) {
        def SummaryCreator creator = new SummaryCreator(config)
        return creator.getSummaryMap(
            options,
            workflow,
            session.baseDir.toString(),
            session.params
        )
    }

    /*
     * Beautify parameters for summary and return as string
     */
    @Function
    public String paramsSummaryLog(
        Map options = null,
        WorkflowMetadata workflow
    ) {

        def Map params = session.params
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : config.parametersSchema

        def colors = getLogColors(config.monochromeLogs)
        String output = ''
        log.info getLogo(workflow, config.monochromeLogs, params.workflow.name, params.workflow.description)

        def Map paramsMap = paramsSummaryMap(workflow, parameters_schema: schemaFilename)
        paramsMap.each { key, value ->
            paramsMap[key] = flattenNestedParamsMap(value as Map)
        }
        def maxChars  = getLongestKeyLength(paramsMap)
        for (group in paramsMap.keySet()) {
            def Map group_params = paramsMap.get(group) as Map // This gets the parameters of that particular group
            if (group_params) {
                output += "$colors.bold$group$colors.reset\n"
                for (String param in group_params.keySet()) {
                    output += "  " + colors.blue + param.padRight(maxChars) + ": " + colors.green +  group_params.get(param) + colors.reset + '\n'
                }
                output += '\n'
            }
        }
        output += "!! Only displaying parameters that differ from the pipeline defaults !!\n"
        output += "-${colors.dim}----------------------------------------------------${colors.reset}-"
        return output
    }

    private Map flattenNestedParamsMap(Map paramsMap) {
        def Map returnMap = [:]
        paramsMap.each { param, value ->
            def String key = param as String
            if (value instanceof Map) {
                def Map flatMap = flattenNestedParamsMap(value as Map)
                flatMap.each { flatParam, flatValue ->
                    returnMap.put(key + "." + flatParam, flatValue)
                }
            } else {
                returnMap.put(key, value)
            }
        }
        return returnMap
    }

    /*
    * Function to loop over all parameters defined in schema and check
    * whether the given parameters adhere to the specifications
    */
    @Function
    void validateParameters(
        Map options = null
    ) {
        def BactopiaSchema validator = new BactopiaSchema(config)
        validator.validateParameters(
            options,
            session.params,
            session.baseDir.toString()
        )
    }
}
