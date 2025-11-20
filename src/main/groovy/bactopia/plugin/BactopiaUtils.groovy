//
// This file holds several Groovy functions that could be useful for any Nextflow pipeline
//
// Modified from NF-Core's template: https://github.com/nf-core/tools
package bactopia.plugin

import groovy.util.logging.Slf4j
import java.io.RandomAccessFile
import java.util.stream.IntStream
import java.util.zip.GZIPInputStream
import org.json.JSONArray
import org.json.JSONObject
import org.yaml.snakeyaml.Yaml

import nextflow.Session
import nextflow.script.WorkflowMetadata

import bactopia.plugin.BactopiaConfig
import bactopia.plugin.BactopiaSchema
import bactopia.plugin.nfschema.HelpMessageCreator
import bactopia.plugin.nfschema.SummaryCreator

import static bactopia.plugin.BactopiaTemplate.dashedLine
import static bactopia.plugin.BactopiaTemplate.getLogColors
import static bactopia.plugin.BactopiaTemplate.getLogo
import static bactopia.plugin.BactopiaTemplate.getWorkflowSummary

import static bactopia.plugin.nfschema.Common.getLongestKeyLength

@Slf4j
class BactopiaUtils {
    //
    // When running with -profile conda, warn if channels have not been set-up appropriately
    //
    public static void checkCondaChannels() {
        Yaml parser = new Yaml()
        def channels = []
        try {
            def config = parser.load("conda config --show channels".execute().text)
            channels = config.channels
        } catch(NullPointerException | IOException e) {
            log.warn("Could not verify conda channel configuration.")
            return
        }

        // Check that all channels are present
        def required_channels = ['conda-forge', 'bioconda', 'defaults']
        def conda_check_failed = !required_channels.every { ch -> ch in channels }

        // Check that they are in the right order
        conda_check_failed |= !(channels.indexOf('conda-forge') < channels.indexOf('bioconda'))
        conda_check_failed |= !(channels.indexOf('bioconda') < channels.indexOf('defaults'))

        if (conda_check_failed) {
            log.warn(
                "=============================================================================\n" +
                "  There is a problem with your Conda configuration!\n\n" +
                "  You will need to set-up the conda-forge and bioconda channels correctly.\n" +
                "  Please refer to https://bioconda.github.io/user/install.html#set-up-channels\n" +
                "  NB: The order of the channels matters!\n" +
                "==================================================================================="
            )
        }
    }


    //
    // Join module args with appropriate spacing
    //
    public static String joinModuleArgs(args_list) {
        return ' ' + args_list.join(' ')
    }


    //
    //  Verify input is a positive integer
    //
    public static Integer isPositiveInteger(value, name) {
        def error = 0
        if (value.getClass() == Integer) {
            if (value < 0) {
                log.error('* --'+ name +': "' + value + '" is not a positive integer.')
                error = 1
            }
        } else {
            if (!value.isInteger()) {
                log.error('* --'+ name +': "' + value + '" is not numeric.')
                error = 1
            } else if (value as Integer < 0) {
                log.error('* --'+ name +': "' + value + '" is not a positive integer.')
                error = 1
            }
        }
        return error
    }


    //
    //  Verify input file exists
    //
    public static Boolean fileExists(filename) {
        if (isLocal(filename)) {
            return new File(filename).exists()
        }
        // For remote files, we assume they exist
        return true
    }


    //
    // Check if workflow is a Bactopia Tool
    //
    public static Boolean isBactopiaTool(params) {
        if (params.containsKey('is_subworkflow') && params.is_subworkflow) {
            return true
        }
        return false
    }


    //
    // Check if file is remote (e.g. AWS, Azure, GCP)
    //
    public static Boolean isLocal(filename) {
        if (filename.startsWith('gs://') || filename.startsWith('s3://') || filename.startsWith('az://') || filename.startsWith('https://')) {
            return false
        }
        return true
    }


    //
    //  Check is a file is not found
    //
    public static Integer fileNotFound(filename, parameter) {
        if (!fileExists(filename)) {
            log.error('* --'+ parameter +': Unable to find "' + filename + '", please verify it exists.'.trim())
            return 1
        }
        return 0
    }


    //
    //  Verify input file is GZipped
    //
    public static Integer fileNotGzipped(filename, parameter) {
        // https://github.com/ConnectedPlacesCatapult/TomboloDigitalConnector/blob/master/src/main/java/uk/org/tombolo/importer/ZipUtils.java

        if (fileNotFound(filename, parameter)) {
            return 1
        } else {
            int magic = 0
            try {
                RandomAccessFile raf = new RandomAccessFile(new File(filename), "r");
                magic = raf.read() & 0xff | ((raf.read() << 8) & 0xff00);
                raf.close();
            } catch (Throwable e) {
                log.error('* --'+ parameter +': Please verify "' + filename + '" is compressed using GZIP')
                return 1
            }

            if (magic == GZIPInputStream.GZIP_MAGIC) {
                return 0
            } else {
                log.error('* --'+ parameter +': Please verify "' + filename + '" is compressed using GZIP')
                return 1
            }
        }
    }

    //=========================================================================================
    //
    // TraceObserver helper functions
    // 
    //=========================================================================================


    //
    // Groovy Map of the help message
    //
    public static String paramsHelp(Session session, BactopiaConfig config) {
        def Map params = session.params
        def String help = ""
        def HelpMessageCreator helpCreator = new HelpMessageCreator(config, session, params["help_all"])
        help += helpCreator.getBeforeText(session, (String) params["workflow"]["name"], (String) params["workflow"]["description"])
        if (params["help_all"]) {
            log.debug("Printing out the full help message")
            help += helpCreator.getFullMessage()
        } else if (params["help"]) {
            log.debug("Printing out the short help message")
            def paramValue = null
            help += helpCreator.getShortMessage(paramValue instanceof String ? paramValue : "")
        }
        help += helpCreator.getAfterText()
        return help
    }

    //
    // Groovy Map summarizing parameters/workflow options used by the pipeline
    //
    public static Map paramsSummaryMap(
        Map options = null,
        WorkflowMetadata workflow,
        Session session,
        BactopiaConfig config
        ) {
        def SummaryCreator creator = new SummaryCreator(config)
        return creator.getSummaryMap(
            options,
            workflow,
            session.baseDir.toString(),
            session.params
        )
    }

    private static Map flattenNestedParamsMap(Map paramsMap) {
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
     * Beautify parameters for summary and return as string
     */
    public static String paramsSummaryLog(
        WorkflowMetadata workflow,
        Session session,
        BactopiaConfig config,
        Map options = null
    ) {
        def Map params = session.params
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : config.parametersSchema

        def colors = getLogColors(config.monochromeLogs)
        String output = ''
        output += getLogo(workflow, config.monochromeLogs, params.workflow.name, params.workflow.description)

        def Map paramsMap = paramsSummaryMap(
            options,
            workflow,
            session,
            config
        )
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
        output += "!! Only displaying parameters that differ from the defaults !!\n"
        output += dashedLine(config.monochromeLogs) + "\n"
        return output
    }

    /*
     * Beautify parameters for summary and return as string
     */
     public static String workflowSummary(Session session, BactopiaConfig config) {
        def Map params = session.params
        def WorkflowMetadata metadata = session.getWorkflowMetadata()
        return getWorkflowSummary( 
            metadata,
            params,
            session.config.manifest.version,
            config.monochromeLogs,
        )
    }

}
