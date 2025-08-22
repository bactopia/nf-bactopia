//
// Sourced and modified from nf-schema
// Original Code: https://github.com/nextflow-io/nf-schema/blob/master/src/main/groovy/nextflow/validation/help/HelpMessageCreator.groovy
//
package bactopia.plugin.nfschema

import groovy.util.logging.Slf4j

import java.nio.file.Path

import nextflow.Session

import bactopia.plugin.BactopiaConfig

import static bactopia.plugin.BactopiaTemplate.dashedLine
import static bactopia.plugin.BactopiaTemplate.getLogColors
import static bactopia.plugin.BactopiaTemplate.getLogo

import static bactopia.plugin.nfschema.Common.getBasePath
import static bactopia.plugin.nfschema.Common.longestStringLength
import static bactopia.plugin.nfschema.Common.getLongestKeyLength
import static bactopia.plugin.nfschema.Files.paramsLoad

/**
 * This class contains methods to write a help message
 *
 * @author : Robert Petit <robbie.petit@gmail.com>
 * @author : nvnieuwk <nicolas.vannieuwkerke@ugent.be>
 *
 */

@Slf4j
class HelpMessageCreator {

    private final BactopiaConfig config
    private final Map colors
    private Integer hiddenParametersCount = 0
    private Map<String,Map> paramsMap
    private Boolean showHidden = false

    // The length of the terminal
    private Integer terminalLength = System.getenv("COLUMNS")?.toInteger() ?: 100

    HelpMessageCreator(BactopiaConfig inputConfig, Session session, Boolean helpAll) {
        config = inputConfig
        colors = getLogColors(config.monochromeLogs)
        session = session
        paramsMap = paramsLoad( Path.of(getBasePath(session.baseDir.toString(), config.parametersSchema)) )
        showHidden = helpAll
        addHelpParameters()
    }

    public String getShortMessage(String param) {
        def String helpMessage = ""
        if (param) {
            def List<String> paramNames = param.tokenize(".") as List<String>
            def Map paramOptions = [:]
            paramsMap.each { String group, Map groupParams ->
                if (groupParams.containsKey(paramNames[0])) {
                    paramOptions = groupParams.get(paramNames[0]) as Map 
                }
            }
            if (paramNames.size() > 1) {
                paramNames.remove(0)
                paramNames.each {
                    paramOptions = (Map) paramOptions?.properties?[it] ?: [:]
                }
            }
            if (!paramOptions) {
                throw new Exception("Unable to create help message: Specified param '${param}' does not exist in JSON schema.")
            }
            if(paramOptions.containsKey("properties")) {
                paramOptions.properties = removeHidden(paramOptions.properties)
            }
            helpMessage = getDetailedHelpString(param, paramOptions)
        } else {
            helpMessage = getGroupHelpString()
        }
        return helpMessage
    }

    public String getFullMessage() {
        return getGroupHelpString(true)
    }

    public String getBeforeText(Session session, String workflowName, String workflowDescription) {
        def String beforeText = getLogo(session, config.monochromeLogs, workflowName, workflowDescription)
        return beforeText + "\n"
    }

    public String getAfterText() {
        def String afterText = ""
        if (hiddenParametersCount > 0) {
            afterText += " !!${colors.dim} Hiding ${hiddenParametersCount} param(s), use `--help_all` to show them${colors.reset} !!"
        }
        afterText += "\n"
        return afterText
    }

    //
    // Get a detailed help string from one parameter
    //
    private String getDetailedHelpString(String paramName, Map paramOptions) {
        def String helpMessage = "${colors.underlined}${colors.bold}--${paramName}${colors.reset}\n"
        def Integer optionMaxChars = longestStringLength(paramOptions.keySet().collect { it == "properties" ? "options" : it } as List<String>)
        for (option in paramOptions) {
            def String key = option.key
            if (key == "fa_icon" || (key == "type" && option.value == "object")) {
                continue
            }
            if (key == "properties") {
                def Map subParamsOptions = [:]
                flattenNestedSchemaMap(option.value as Map).each { String subParam, Map value ->
                    subParamsOptions.put("${paramName}.${subParam}" as String, value)
                }
                def Integer maxChars = longestStringLength(subParamsOptions.keySet() as List<String>) + 1
                def String subParamsHelpString = getHelpListParams(subParamsOptions, maxChars, paramName)
                    .collect {
                        "      --" + it[4..it.length()-1]
                    }
                    .join("\n")
                helpMessage += "    " + colors.dim + "options".padRight(optionMaxChars) + ": " + colors.reset + "\n" + subParamsHelpString + "\n\n"
                continue
            }
            def String value = option.value
            if (value.length() > terminalLength) {
                value = wrapText(value)
            }
            helpMessage += "    " + colors.dim + key.padRight(optionMaxChars) + ": " + colors.reset + value + '\n'
        }
        return helpMessage
    }

    //
    // Get the full help message for a grouped params structure in list format
    //
    private String getGroupHelpString(Boolean showNested = false) {
        def String helpMessage = ""
        def Map<String,Map> visibleParamsMap = paramsMap.collectEntries { key, Map value -> [key, removeHidden(value)]}
        def Map<String,Map> parsedParams = showNested ? visibleParamsMap.collectEntries { key, Map value -> [key, flattenNestedSchemaMap(value)] } : visibleParamsMap
        def Integer maxChars = getLongestKeyLength(parsedParams) + 1
        if (parsedParams.containsKey("Other parameters")) {
            def Map ungroupedParams = parsedParams["Other parameters"]
            parsedParams.remove("Other parameters")
            helpMessage += getHelpListParams(ungroupedParams, maxChars + 2).collect {
                it[2..it.length()-1]
            }.join("\n") + "\n\n"
        }
        parsedParams.each { String group, Map groupParams ->
            def List<String> helpList = getHelpListParams(groupParams, maxChars)
            if (helpList.size() > 0) {
                helpMessage += "${colors.underlined}${colors.bold}${group}${colors.reset}\n" as String
                helpMessage += helpList.join("\n") + "\n\n"
            }
        }
        return helpMessage
    }

    public Map<String,Map> removeHidden(Map<String,Map> map) {
        if (showHidden) {
            log.debug("Showing all parameters, including hidden ones")
            return map
        }

        // Remove hidden parameters
        log.debug("Removing hidden parameters")
        def Map<String,Map> returnMap = [:]
        map.each { String key, Map value ->
            if(!value.hidden) {
                returnMap[key] = value
            } else if(value.containsKey("properties")) {
                value.properties = removeHidden(value.properties)
                returnMap[key] = value
            } else {
                hiddenParametersCount++
            }
        }
        return returnMap
    }

    //
    // Get help for params in list format
    //
    private List<String> getHelpListParams(Map<String,Map> params, Integer maxChars, String parentParameter = "") {
        def List helpMessage = []
        def Integer typeMaxChars = longestStringLength(params.collect { key, value -> 
            def Object type = value.get("type", "")
            return type instanceof String && type.length() > 0 ? "[${type}]" : type as String}
        )
        for (String paramName in params.keySet()) {
            def Map paramOptions = params.get(paramName) as Map
            def Object paramType = paramOptions.get("type", "")
            def String type = paramType instanceof String && paramType.length() > 0 ? '[' + paramType + ']' : paramType as String
            def String enumsString = ""
            if (paramOptions.enum != null) {
                def List enums = (List) paramOptions.enum
                def String chopEnums = enums.join(", ")
                if(chopEnums.length() > terminalLength){
                    chopEnums = chopEnums.substring(0, terminalLength-5)
                    chopEnums = chopEnums.substring(0, chopEnums.lastIndexOf(",")) + ", ..."
                }
                enumsString = " (accepted: " + chopEnums + ") "
            }
            def String description = paramOptions.description ? paramOptions.description as String + " " : ""
            def defaultValue = paramOptions.default != null ? "[default: " + paramOptions.default.toString() + "] " : ''
            def String nestedParamName = parentParameter ? parentParameter + "." + paramName : paramName
            def String nestedString = paramOptions.properties ? "(This parameter has sub-parameters. Use '--help ${nestedParamName}' to see all sub-parameters) " : ""
            def descriptionDefault = description + colors.dim + enumsString + defaultValue + colors.reset + nestedString

            // Add subheaders if they exist
            if (paramOptions.containsKey('header')) {
                helpMessage.add("  " + colors.underlined + colors.bold + paramOptions.header + colors.reset)

                if (paramOptions.header.endsWith('Assembly')) {
                    helpMessage.add("  " + colors.dim + "Note: Error free Illumina reads are simulated for assemblies" + colors.reset)
                }
            }
            // Wrap long description texts
            // Loosely based on https://dzone.com/articles/groovy-plain-text-word-wrap
            if (descriptionDefault.length() > terminalLength){
                descriptionDefault = wrapText(descriptionDefault)
            }
            helpMessage.add("  --" +  paramName.padRight(maxChars) + colors.dim + type.padRight(typeMaxChars + 1) + colors.reset + descriptionDefault)
        }
        return helpMessage
    }

    //
    // Flattens the schema params map so all nested parameters are shown as their full name
    //
    private Map<String,Map> flattenNestedSchemaMap(Map params) {
        def Map returnMap = [:]
        params.each { String key, Map value ->
            if (value.containsKey("properties")) {
                def flattenedMap = flattenNestedSchemaMap(value.properties)
                flattenedMap.each { String k, Map v ->
                    returnMap.put(key + "." + k, v)
                }
            } else {
                returnMap.put(key, value)
            }
        }
        return returnMap
    }

    //
    // This function adds the help parameters to the main parameters map as ungrouped parameters
    //
    private void addHelpParameters() {
        if (!paramsMap.containsKey("Other parameters")) {
            paramsMap["Other parameters"] = [:]
        }
        paramsMap["Other parameters"]["help"] = [
            "type": ["boolean"],
            "description": "Show this message for all non-hidden parameters."
        ]
        paramsMap["Other parameters"]["help_all"] = [
            "type": "boolean",
            "description": "Show the help message for all parameters."
        ]
    }

    //
    // Wrap too long text
    //
    private String wrapText(String text) {
        def List olines = []
        def String oline = ""
        text.split(" ").each() { wrd ->
            if ((oline.size() + wrd.size()) <= terminalLength) {
                oline += wrd + " "
            } else {
                olines += oline
                oline = wrd + " "
            }
        }
        olines += oline
        return olines.join("\n")
    }


}
