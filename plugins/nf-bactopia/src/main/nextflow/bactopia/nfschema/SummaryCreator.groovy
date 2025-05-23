//
// Sourced and modified from nf-schema
// Original Code: https://github.com/nextflow-io/nf-schema/blob/master/src/main/groovy/nextflow/validation/summary/SummaryCreator.groovy
//
package nextflow.bactopia.nfschema

import groovy.util.logging.Slf4j
import java.nio.file.Files
import java.nio.file.Path
import nextflow.Nextflow
import nextflow.script.WorkflowMetadata

import nextflow.bactopia.BactopiaConfig
import static nextflow.bactopia.nfschema.Files.paramsLoad
import static nextflow.bactopia.nfschema.Common.getBasePath

/**
 * @author : Robert Petit <robbie.petit@gmail.com>
 * @author : mirpedrol <mirp.julia@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 * @author : KevinMenden
 */

@Slf4j
class SummaryCreator {

    final private BactopiaConfig config

    SummaryCreator(BactopiaConfig config) {
        this.config = config
    }

    public Map getSummaryMap(
        Map options,
        WorkflowMetadata workflow,
        String baseDir,
        Map params
    ) {
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : config.parametersSchema
        
        // Get a selection of core Nextflow workflow options
        def Map workflowSummary = [:]
        if (workflow.revision) {
            workflowSummary['revision'] = workflow.revision
        }
        workflowSummary['runName']      = workflow.runName
        workflowSummary['launchDir']    = workflow.launchDir
        workflowSummary['workDir']      = workflow.workDir
        workflowSummary['projectDir']   = workflow.projectDir
        workflowSummary['userName']     = workflow.userName
        workflowSummary['profile']      = workflow.profile
        workflowSummary['configFiles']  = workflow.configFiles ? workflow.configFiles.join(', ') : ''

        // Get pipeline parameters defined in JSON Schema
        def Map paramsSummary = [:]
        def Map paramsMap = paramsLoad( Path.of(getBasePath(baseDir, schemaFilename)) )
        for (group in paramsMap.keySet()) {
            def Map groupSummary = getSummaryMapFromParams(params, paramsMap.get(group) as Map)
            /*
            config.summary.hideParams.each { hideParam ->
                def List<String> hideParamList = hideParam.tokenize(".") as List<String>
                def Integer indexCounter = 0
                def Map nestedSummary = groupSummary
                if(hideParamList.size() >= 2 ) {
                    hideParamList[0..-2].each { it ->
                        nestedSummary = nestedSummary?.get(it, null)
                    }
                }
                if(nestedSummary != null ) {
                    nestedSummary.remove(hideParamList[-1])
                }
            }
            */
            paramsSummary.put(group, groupSummary)
        }
        paramsSummary.put('Core Nextflow options', workflowSummary)
        return paramsSummary
    }

    //
    // Create a summary map for the given parameters
    //
    private Map getSummaryMapFromParams(Map params, Map paramsSchema) {
        def Map summary = [:]
        for (String param in paramsSchema.keySet()) {
            if (params.containsKey(param)) {
                def Map schema = paramsSchema.get(param) as Map 
                if (params.get(param) instanceof Map && schema.containsKey("properties")) {
                    summary.put(param, getSummaryMapFromParams(params.get(param) as Map, schema.get("properties") as Map))
                    continue
                }
                def String value = params.get(param)
                def String defaultValue = schema.get("default")
                def String type = schema.type
                if (defaultValue != null) {
                    if (type == 'string') {
                        // TODO rework this in a more flexible way
                        if (defaultValue.contains('$projectDir') || defaultValue.contains('${projectDir}')) {
                            def sub_string = defaultValue.replace('\$projectDir', '')
                            sub_string     = sub_string.replace('\${projectDir}', '')
                            if (value.contains(sub_string)) {
                                defaultValue = value
                            }
                        }
                        if (defaultValue.contains('$params.outdir') || defaultValue.contains('${params.outdir}')) {
                            def sub_string = defaultValue.replace('\$params.outdir', '')
                            sub_string     = sub_string.replace('\${params.outdir}', '')
                            if ("${params.outdir}${sub_string}" == value) {
                                defaultValue = value
                            }
                        }
                    }
                }

                // We have a default in the schema, and this isn't it
                if (defaultValue != null && value != defaultValue) {
                    summary.put(param, value)
                }
                // No default in the schema, and this isn't empty or false
                else if (defaultValue == null && value != "" && value != null && value != false && value != 'false') {
                    summary.put(param, value)
                }
            }
        }
        return summary
    }

}
