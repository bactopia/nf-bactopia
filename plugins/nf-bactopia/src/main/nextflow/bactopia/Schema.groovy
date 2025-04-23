//
// This file holds several functions used to perform JSON parameter validation, help and
// summary rendering for Bactopia.
//
// This is based on the original NF-Core template (the OG 'libs' folder) and the nf-validation
// plugin (which you should probably be using instead of this file).
package nextflow.bactopia

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.json.JSONObject
import org.json.JSONArray

import groovy.util.logging.Slf4j
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import java.util.regex.Matcher

import dev.harrel.jsonschema.ValidatorFactory
import dev.harrel.jsonschema.Validator
import dev.harrel.jsonschema.EvaluatorFactory
import dev.harrel.jsonschema.FormatEvaluatorFactory
import dev.harrel.jsonschema.JsonNode
import dev.harrel.jsonschema.providers.OrgJsonNode

import nextflow.Nextflow
import nextflow.util.Duration
import nextflow.util.MemoryUnit

import nextflow.bactopia.BactopiaConfig
import nextflow.bactopia.nfschema.JsonSchemaValidator

@Slf4j
class Schema {

    private ValidatorFactory validator
    private BactopiaConfig config

    Schema(BactopiaConfig config) {
        this.validator = new ValidatorFactory()
            .withJsonNodeFactory(new OrgJsonNode.Factory())
            .withEvaluatorFactory(new FormatEvaluatorFactory())
        this.config = config
    }

    final List<String> NF_OPTIONS = [
            // Options for base `nextflow` command
            'bg',
            'c',
            'C',
            'config',
            'd',
            'D',
            'dockerize',
            'h',
            'log',
            'q',
            'quiet',
            'syslog',
            'v',

            // Options for `nextflow run` command
            'ansi',
            'ansi-log',
            'bg',
            'bucket-dir',
            'c',
            'cache',
            'config',
            'dsl2',
            'dump-channels',
            'dump-hashes',
            'E',
            'entry',
            'latest',
            'lib',
            'main-script',
            'N',
            'name',
            'offline',
            'params-file',
            'pi',
            'plugins',
            'poll-interval',
            'pool-size',
            'profile',
            'ps',
            'qs',
            'queue-size',
            'r',
            'resume',
            'revision',
            'stdin',
            'stub',
            'stub-run',
            'test',
            'w',
            'with-charliecloud',
            'with-conda',
            'with-dag',
            'with-docker',
            'with-mpi',
            'with-notification',
            'with-podman',
            'with-report',
            'with-singularity',
            'with-timeline',
            'with-tower',
            'with-trace',
            'with-weblog',
            'without-docker',
            'without-podman',
            'work-dir'
        ]

    private List<String> errors = []
    private boolean hasErrors() { errors.size()>0 }
    private List<String> getErrors() { errors }

    private List<String> warnings = []
    private boolean hasWarnings() { warnings.size()>0 }
    private List<String> getWarnings() { warnings }

    // The length of the terminal
    private Integer terminalLength = System.getenv("COLUMNS")?.toInteger() ?: 100

    public validateParameters(
        Map options = null,
        Map inputParams = [:],
        String baseDir
    ) {
        log.debug "Starting parameters validation"
        // Read schema file
        def String schemaFilename = options?.containsKey('parameters_schema') ? options.parameters_schema as String : config.parametersSchema
        def String schemaString = Files.readString( Path.of(getBasePath(baseDir, schemaFilename)) )

        // Clean the parameters and convert to JSON
        def cleanedParams = cleanParameters(inputParams)
        def paramsJSON = new JSONObject(new JsonBuilder(cleanedParams).toString())

        // Validate the parameters
        def validator = new JsonSchemaValidator(config)
        Tuple2<List<String>,List<String>> validationResult = validator.validate(paramsJSON, schemaString)
        def validationErrors = validationResult[0]
        def unevaluatedParams = validationResult[1]
        log.info "Validation errors: ${validationErrors}"
        log.info "Unevaluated parameters: ${unevaluatedParams}"

/*
        // Read the JSON schema

        // Collect expected parameters from the schema
        def Map schemaParams = [:]
        schemaParams += (Map) new JsonSlurper().parseText(schema_string).get('$defs')
        def expectedParams = []
        def enums = [:]
        for (group in schemaParams) {
            def Map properties = (Map) group.value['properties']
            for (p in properties) {
                // Store the expected parameter name
                def String key = (String) p.key
                expectedParams.push(p.key)

                // Collect enums for validation error messages
                def Map property = (Map) properties[key] as Map
                if (property.containsKey('enum')) {
                    enums[p.key] = property['enum']
                }
            }
        }

        // Compare expected against parameters received
        def expectedParamsLowerCase = expectedParams.collect{ it.replace("-", "").toLowerCase() }
        def ignoreParams = options?.containsKey('ignore_params') ? options.ignore_params as String : config.ignoreParams
        for (param in params.keySet()) {
            // Handle core Nextflow parameters (error)
            if (NF_OPTIONS.contains(param)) {
                errors << "You used a core Nextflow option with two hyphens: '--${param}'. Please resubmit with '-${param}'"
            }

            // Handle unexpected parameters (warning)
            def paramLowerCase = param.replace("-", "").toLowerCase()
            def isCamelCaseBug = (param.contains("-") && !expectedParams.contains(param) && expectedParamsLowerCase.contains(paramLowerCase))
            if (!expectedParams.contains(param) && !ignoreParams.contains(param) && !isCamelCaseBug) {
                // Temporarily remove camelCase/camel-case params #1035
                def unexpectedParamsLowerCase = unexpectedParams.collect{ it.replace("-", "").toLowerCase()}
                if (!unexpectedParamsLowerCase.contains(paramLowerCase)){
                    warnings << "* --${param}: ${params[unexpectedParam].toString()}"
                }
            }
        }

        // Print warnings
        if (this.hasWarnings()) {
            def msg = "The following invalid input values have been detected:\n\n" + this.getWarnings().join('\n').trim() + "\n\n"
            log.warn(msg)
        }

        //=====================================================================//
        // Validate parameters against the schema
        InputStream input_stream = new File(Path.of(getBasePath(baseDir, schemaFilename))).newInputStream()
        JSONObject raw_schema = new JSONObject(new JSONTokener(input_stream))

        // Remove anything that's in params.schema_ignore_params
        raw_schema = removeIgnoredParams(raw_schema, params)

        Schema schema = SchemaLoader.load(raw_schema)



        

        // Validate
        try {
            schema.validate(params_json)
        } catch (ValidationException e) {
            println ''
            log.error 'ERROR: Validation of pipeline parameters failed!'
            JSONObject exceptionJSON = e.toJSON()
            printExceptions(exceptionJSON, params_json, log, enums)
            println ''
            has_error = true
        }


        if (hasErrors()) {
            System.exit(1)
        }
*/
        log.debug "Finishing parameters validation"
    }


    //==================================================================================================================
    //
    // BEGIN functions have been sourced and modified from nextflow/nf-schema
    //
    private Tuple2<List<String>,List<String>> validateObject(JsonNode input, String validationType, Object rawJson, String schemaString) {
        def JSONObject schema = new JSONObject(schemaString)
        
        def Validator.Result result = this.validator.validate(schema, input)
        def List<String> errors = []
        result.getErrors().each { error ->
            def String errorString = error.getError()

            // Skip double error in the parameter schema
            if (errorString.startsWith("Value does not match against the schemas at indexes") && validationType == "parameter") {
                return
            }

            def String instanceLocation = error.getInstanceLocation()
            def String value = getValueFromJsonPointer(instanceLocation, rawJson)
            if(config.maxErrValSize >= 1 && value.size() > config.maxErrValSize) {
                value = "${value[0..(config.maxErrValSize/2-1)]}...${value[-config.maxErrValSize/2..-1]}" as String
            }

            // Return a standard error message for object validation
            if (validationType == "object") {
                errors.add("${instanceLocation ? instanceLocation + ' ' : ''}(${value}): ${errorString}" as String)
                return
            }

            // Get the custom errorMessage if there is one and the validation errors are not about the content of the file
            def String schemaLocation = error.getSchemaLocation().replaceFirst(/^[^#]+/, "")
            def String customError = ""
            if (!errorString.startsWith("Validation of file failed:")) {
                customError = getValueFromJsonPointer("${schemaLocation}/errorMessage", schema) as String
            }

            // Change some error messages to make them more clear
            def String keyword = error.getKeyword()
            if (keyword == "required") {
                def Matcher matcher = errorString =~ ~/\[\[([^\[\]]*)\]\]$/
                def String missingKeywords = matcher.findAll().flatten().last()
                errorString = "Missing required ${validationType}(s): ${missingKeywords}"
            }

            def List<String> locationList = instanceLocation.split("/").findAll { it != "" } as List

            def String printableError = "${validationType == 'field' ? '->' : '*'} ${errorString}" as String
            if (locationList.size() > 0 && isInteger(locationList[0]) && validationType == "field") {
                def Integer entryInteger = locationList[0] as Integer
                def String entryString = "Entry ${entryInteger + 1}" as String
                def String fieldError = "${errorString}" as String
                if(locationList.size() > 1) {
                    fieldError = "Error for ${validationType} '${locationList[1..-1].join("/")}' (${value}): ${errorString}"
                }
                printableError = "-> ${entryString}: ${fieldError}" as String
            } else if (validationType == "parameter") {
                def String fieldName = locationList.join(".")
                if(fieldName != "") {
                    printableError = "* --${fieldName} (${value}): ${errorString}" as String
                }
            }

            if(customError != "") {
                printableError = printableError + " (${customError})"
            }

            errors.add(printableError)

        }
        def List<String> unevaluated = getUnevaluated(result, rawJson)
        return Tuple.tuple(errors, unevaluated)
    }

    public Tuple2<List<String>,List<String>> validate(JSONArray input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "field", input, schemaString)
    }

    public Tuple2<List<String>,List<String>> validate(JSONObject input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "parameter", input, schemaString)
    }

    public Tuple2<List<String>,List<String>> validateObj(Object input, String schemaString) {
        def JsonNode jsonInput = new OrgJsonNode.Factory().wrap(input)
        return this.validateObject(jsonInput, "object", input, schemaString)
    }

    public static List<String> getUnevaluated(Validator.Result result, Object rawJson) {
        def Set<String> evaluated = []
        result.getAnnotations().each{ anno ->
            if(anno.keyword in ["properties", "patternProperties", "additionalProperties"]){
                evaluated.addAll(
                    anno.annotation.collect{ it ->
                    "${anno.instanceLocation.toString()}/${it.toString()}".replaceAll("^/+", "")
                    }
                )
            }
        }
        def Set<String> all_keys = []
        findAllKeys(rawJson, null, all_keys, '/')
        def unevaluated_ = all_keys - evaluated
        def unevaluated = unevaluated_.collect{ it -> !evaluated.contains(kebabToCamel(it)) ? it : null }
        return unevaluated - null
    }

    //
    // Clean and check parameters relative to Nextflow native classes
    //
    private Map cleanParameters(Map params) {
        def Map new_params = (Map) params.getClass().newInstance(params)
        for (p in params) {
            // remove anything evaluating to false
            if (!p['value'] && p['value'] != 0) {
                new_params.remove(p.key)
            }

            // Cast MemoryUnit to String
            if (p['value'] instanceof MemoryUnit) {
                new_params.replace(p.key, p['value'].toString())
            }

            // Cast Duration to String
            if (p['value'] instanceof Duration) {
                new_params.replace(p.key, p['value'].toString())
            }

            // Cast LinkedHashMap to String
            if (p['value'] instanceof LinkedHashMap) {
                new_params.replace(p.key, p['value'].toString())
            }

            // Parsed nested parameters
            if (p['value'] instanceof Map) {
                new_params.replace(p.key, cleanParameters(p['value'] as Map))
            }
        }
        return new_params
    }

    //
    // Get full path based on the base directory of the pipeline run
    //
    public static String getBasePath(String baseDir, String schemaFilename) {
        if (Path.of(schemaFilename).exists()) {
            return schemaFilename
        } else {
            return "${baseDir}/${schemaFilename}"
        }
    }
    //
    // END of functions sourced from nextflow/nf-schema
    //
    //==================================================================================================================















































    //
    // Beautify parameters for --help
    //
    /*
    public static Map paramsHelp(workflow, params, command, schema_filename=['nextflow_schema.json'], print_example=true, print_required=false) {
        Map colors = NfcoreTemplate.logColours(params.monochrome_logs)
        Integer num_hidden = 0
        String required_output = ''
        String optional_output = ''
        String param_required = ''
        String output = ''
        if (print_example == true) {
            output += 'Typical pipeline command:\n\n'
            output += "  ${colors.cyan}${command}${colors.reset}\n\n"
        }
        Map params_map = paramsLoad("${workflow.projectDir}", schema_filename)
        Integer max_chars  = paramsMaxChars(params_map) + 1
        Integer desc_indent = max_chars + 14
        Integer dec_linewidth = 160 - desc_indent
        for (group in params_map.keySet()) {
            Boolean is_required = false
            if (print_required == true && group != "Required Parameters") {
                continue
            }

            Integer num_params = 0
            String group_output = ''
            if (group != 'Required Parameters') {
                group_output += colors.underlined + colors.bold + group + colors.reset + '\n'
            }
            String group_optional = ''
            String group_required = ''
            def group_params = params_map.get(group)  // This gets the parameters of that particular group
            for (param in group_params.keySet()) {
                String param_output = ''
                if (!params.help_all) {
                    if (group_params.get(param).hidden && (!params.show_hidden_params || !params.help_all)) {
                        num_hidden += 1
                        continue;
                    }
                }
                if (group_params.get(param).containsKey('header')) {
                    param_output += '  ' + colors.underlined + colors.bold + group_params.get(param).header + colors.reset + '\n'

                    if (group_params.get(param).header.endsWith('Assembly')) {
                        param_output += '  ' + colors.dim + 'Note: Error free Illumina reads are simulated for assemblies' + colors.reset + '\n'
                    }
                }

                def type = '[' + group_params.get(param).type + ']'
                def description = group_params.get(param).description
                def defaultValue = group_params.get(param).default ? " [default: " + group_params.get(param).default.toString() + "]" : ''
                def description_default = description + colors.dim + defaultValue + colors.reset
                // Wrap long description texts
                // Loosely based on https://dzone.com/articles/groovy-plain-text-word-wrap
                if (description_default.length() > dec_linewidth){
                    List olines = []
                    String oline = "" // " " * indent
                    description_default.split(" ").each() { wrd ->
                        if ((oline.size() + wrd.size()) <= dec_linewidth) {
                            oline += wrd + " "
                        } else {
                            olines += oline
                            oline = wrd + " "
                        }
                    }
                    olines += oline
                    description_default = olines.join("\n" + " " * desc_indent)
                }

                param_output += "  --" +  param.padRight(max_chars) + colors.dim + type.padRight(10) + colors.reset + description_default + '\n'
                num_params += 1
                
                if (group == "Required Parameters") {
                    group_required += param_output
                } else if (group_params.get(param).containsKey('is_required')) {
                    param_required += param_output
                } else {
                    group_optional += param_output
                }
            }

            if (num_params > 0) {
                required_output += group_required
                optional_output += group_output + group_optional + '\n'
            }
        }

        if (param_required.length() > 0) {
            required_output += '\n  ' + colors.underlined + colors.bold + "Workflow Specific" + colors.reset + '\n'
            required_output += param_required
        }

        def Map help = [:]
        required_output = colors.underlined + colors.bold + 'Required Parameters' + colors.reset + '\n' + required_output
        help['output'] = output + required_output + optional_output
        help['num_hidden'] = num_hidden
        return help
    }
    */

    //
    // Beautify parameters for --help
    //
    /*
    public static String paramsRequired(workflow, params, schema_filename=['nextflow_schema.json']) {
        Map colors = NfcoreTemplate.logColours(params.monochrome_logs)
        Integer num_hidden = 0
        String required_output = ''
        String param_required = ''
        String output = ''
        Map params_map = paramsLoad("${workflow.projectDir}", schema_filename)
        Integer max_chars = paramsMaxChars(params_map) + 1
        Integer desc_indent = max_chars + 14
        Integer dec_linewidth = 160 - desc_indent
        for (group in params_map.keySet()) {
            Integer num_params = 0
            String group_output = ''
            def group_params = params_map.get(group)  // This gets the parameters of that particular group
            for (param in group_params.keySet()) {
                String param_output = ''
                if (!params.help_all) {
                    if (group_params.get(param).hidden && !params.show_hidden_params) {
                        num_hidden += 1
                        continue;
                    }
                }
                if (group_params.get(param).containsKey('header')) {
                    param_output += '  ' + colors.underlined + colors.bold + group_params.get(param).header + colors.reset + '\n'

                    if (group_params.get(param).header.endsWith('Assembly')) {
                        param_output += '  ' + colors.dim + 'Note: Error free Illumina reads are simulated for assemblies' + colors.reset + '\n'
                    }
                }

                def type = '[' + group_params.get(param).type + ']'
                def description = group_params.get(param).description
                def defaultValue = group_params.get(param).default ? " [default: " + group_params.get(param).default.toString() + "]" : ''
                def description_default = description + colors.dim + defaultValue + colors.reset
                // Wrap long description texts
                // Loosely based on https://dzone.com/articles/groovy-plain-text-word-wrap
                if (description_default.length() > dec_linewidth){
                    List olines = []
                    String oline = "" // " " * indent
                    description_default.split(" ").each() { wrd ->
                        if ((oline.size() + wrd.size()) <= dec_linewidth) {
                            oline += wrd + " "
                        } else {
                            olines += oline
                            oline = wrd + " "
                        }
                    }
                    olines += oline
                    description_default = olines.join("\n" + " " * desc_indent)
                }
                param_output += "  --" +  param.padRight(max_chars) + colors.dim + type.padRight(10) + colors.reset + description_default + '\n'
                num_params += 1

                if (group == "Required Parameters") {
                    group_output += param_output
                } else if (group_params.get(param).containsKey('is_required')) {
                    param_required += param_output
                }
            }

            if (num_params > 0){
                required_output += group_output
            }
        }

        if (param_required.length() > 0) {
            required_output += '\n  ' + colors.underlined + colors.bold + "Workflow Specific" + colors.reset + '\n'
            required_output += param_required
        }

        if (num_hidden > 0){
            output += colors.dim + "!! Hiding $num_hidden params, use --show_hidden_params (or --help_all) to show them !!\n" + colors.reset
        }
        required_output = colors.underlined + colors.bold + 'Required Parameters' + colors.reset + '\n' + required_output + '\n'
        required_output += NfcoreTemplate.dashedLine(params.monochrome_logs)
        return required_output
    }
    */

    //
    // Beautify parameters for --list_wfs
    //
    /*
    public static String listWorkflows(workflow, params) {
        Map colors = NfcoreTemplate.logColours(params.monochrome_logs)
        Integer num_hidden = 0
        String output  = ''
        output += "Below are a list of workflows you can call using the ${colors.cyan}--wf${colors.reset} parameter.\n\n"
        Integer max_chars = params.workflows.keySet().sort({ a, b -> b.length() <=> a.length() })[0].length() + 1
        Integer desc_indent = max_chars + 14
        Integer dec_linewidth = 160 - desc_indent
        for (group in params.available_workflows.keySet()) {
            String group_name = group == 'bactopia' ? 'Bactopia' : 'Bactopia Tools'
            output += colors.underlined + colors.bold + group_name + colors.reset + '\n'
            if (group == 'bactopia') {
                for (wf in params.available_workflows[group].sort()) {
                    if (wf == 'updater') {
                        continue
                    }
                    def description = params.workflows[wf].description
                    def description_default = description 
                    // Wrap long description texts
                    // Loosely based on https://dzone.com/articles/groovy-plain-text-word-wrap
                    if (description_default.length() > dec_linewidth){
                        List olines = []
                        String oline = "" // " " * indent
                        description_default.split(" ").each() { wrd ->
                            if ((oline.size() + wrd.size()) <= dec_linewidth) {
                                oline += wrd + " "
                            } else {
                                olines += oline
                                oline = wrd + " "
                            }
                        }
                        olines += oline
                        description_default = olines.join("\n" + " " * desc_indent)
                    }
                    String wf_name = wf == 'bactopia' ? 'bactopia (default)' : wf
                    output += "  " +  wf_name.padRight(max_chars) + description_default.padRight(10) + '\n'
                }
                output += '\n'
            } else {
                output += 'Bactopia Tools can include multiple tools (Subworkflows) or a single tool (Modules).\n\n'
                for (wf_type in ['subworkflows', 'modules']) {
                    Integer wf_total = params.available_workflows[group][wf_type].size()
                    String wf_desc = wf_type == 'subworkflows' ? 'Subworkflows' : 'Modules'
                    output += colors.bold + wf_desc + ' (' + wf_total + ')' + colors.reset + '\n'
                    for (wf in params.available_workflows[group][wf_type].sort()) {

                        def description = params.workflows[wf].description
                        def description_default = description 
                        // Wrap long description texts
                        // Loosely based on https://dzone.com/articles/groovy-plain-text-word-wrap
                        if (description_default.length() > dec_linewidth){
                            List olines = []
                            String oline = "" // " " * indent
                            description_default.split(" ").each() { wrd ->
                                if ((oline.size() + wrd.size()) <= dec_linewidth) {
                                    oline += wrd + " "
                                } else {
                                    olines += oline
                                    oline = wrd + " "
                                }
                            }
                            olines += oline
                            description_default = olines.join("\n" + " " * desc_indent)
                        }
                        String wf_name = wf == 'bactopia' ? 'bactopia (default)' : wf
                        output += "  " +  wf_name.padRight(max_chars) + description_default.padRight(10) + '\n'
                    }
                    output += '\n'
                }
            }
        }
        return output
    }
    */

    //
    // Groovy Map summarising parameters/workflow options used by the pipeline
    //
    /*
    public static LinkedHashMap paramsSummaryMap(workflow, params, schema_filename=['nextflow_schema.json']) {
        // Get a selection of core Nextflow workflow options
        def Map workflow_summary = [:]
        if (workflow.revision) {
            workflow_summary['revision'] = workflow.revision
        }
        workflow_summary['runName']      = workflow.runName
        if (workflow.containerEngine) {
            workflow_summary['containerEngine'] = workflow.containerEngine
        }
        if (workflow.container) {
            workflow_summary['container'] = workflow.container
        }
        workflow_summary['launchDir']    = workflow.launchDir
        workflow_summary['workDir']      = workflow.workDir
        workflow_summary['projectDir']   = workflow.projectDir
        workflow_summary['userName']     = workflow.userName
        workflow_summary['profile']      = workflow.profile
        workflow_summary['configFiles']  = workflow.configFiles.join(', ')

        // Get pipeline parameters defined in JSON Schema
        def Map params_summary = [:]
        def blacklist  = ['hostnames']
        def params_map = paramsLoad("${workflow.projectDir}", schema_filename)
        for (group in params_map.keySet()) {
            def sub_params = new LinkedHashMap()
            def group_params = params_map.get(group)  // This gets the parameters of that particular group
            for (param in group_params.keySet()) {
                if (params.containsKey(param) && !blacklist.contains(param)) {
                    def params_value = params.get(param)
                    def schema_value = group_params.get(param).default
                    def param_type   = group_params.get(param).type
                    if (schema_value != null) {
                        if (param_type == 'string') {
                            if (schema_value.contains('$projectDir') || schema_value.contains('${projectDir}')) {
                                def sub_string = schema_value.replace('\$projectDir', '')
                                sub_string     = sub_string.replace('\${projectDir}', '')
                                if (params_value.contains(sub_string)) {
                                    schema_value = params_value
                                }
                            }
                            if (schema_value.contains('$params.outdir') || schema_value.contains('${params.outdir}')) {
                                def sub_string = schema_value.replace('\$params.outdir', '')
                                sub_string     = sub_string.replace('\${params.outdir}', '')
                                if ("${params.outdir}${sub_string}" == params_value) {
                                    schema_value = params_value
                                }
                            }
                        }
                    }

                    // We have a default in the schema, and this isn't it
                    if (schema_value != null && params_value != schema_value) {
                        sub_params.put(param, params_value)
                    }
                    // No default in the schema, and this isn't empty
                    else if (schema_value == null && params_value != "" && params_value != null && params_value != false) {
                        sub_params.put(param, params_value)
                    }
                }
            }
            params_summary.put(group, sub_params)
        }
        return [ 'Core Nextflow options' : workflow_summary ] << params_summary
    }

    //
    // Beautify parameters for summary and return as string
    //
    public static String paramsSummaryLog(workflow, params, schema_filename) {
        Map colors = NfcoreTemplate.logColours(params.monochrome_logs)
        String output  = ''
        def params_map = paramsSummaryMap(workflow, params, schema_filename=schema_filename)
        def max_chars  = paramsMaxChars(params_map)
        for (group in params_map.keySet()) {
            def group_params = params_map.get(group)  // This gets the parameters of that particular group
            if (group_params) {
                output += colors.bold + group + colors.reset + '\n'
                for (param in group_params.keySet()) {
                    if (param == 'max_memory') {
                        if (params.resources.max_memory_adjusted) {
                            output += "  " + colors.blue + param.padRight(max_chars) + ": " + colors.green +  params.resources.max_memory + colors.reset + " (Original request (" + colors.green + group_params.get(param) + colors.reset + ") adjusted to fit your system)\n"
                        } else {
                            output += "  " + colors.blue + param.padRight(max_chars) + ": " + colors.green +  group_params.get(param) + colors.reset + '\n'
                        }
                    } else if (param == 'max_cpus') {
                        if (params.resources.max_cpus_adjusted) {
                            output += "  " + colors.blue + param.padRight(max_chars) + ": " + colors.green +  params.resources.max_cpus + colors.reset + " (Original request (" + colors.green + group_params.get(param) + colors.reset + ") adjusted to fit your system)\n"
                        } else {
                            output += "  " + colors.blue + param.padRight(max_chars) + ": " + colors.green +  group_params.get(param) + colors.reset + '\n'
                        }
                    } else {
                        output += "  " + colors.blue + param.padRight(max_chars) + ": " + colors.green +  group_params.get(param) + colors.reset + '\n'
                    }
                }
                output += '\n'
            }
        }
        output += "!! Only displaying parameters that differ from the pipeline defaults !!\n"
        output += NfcoreTemplate.dashedLine(params.monochrome_logs)
        return output
    }

    //
    // Loop over nested exceptions and print the causingException
    //
    private static void printExceptions(ex_json, params_json, log, enums, limit=5) {
        def causingExceptions = ex_json['causingExceptions']
        if (causingExceptions.length() == 0) {
            def m = ex_json['message'] =~ /required key \[([^\]]+)\] not found/
            // Missing required param
            if (m.matches()) {
                log.error "* Missing required parameter: --${m[0][1]}"
            }
            // Other base-level error
            else if (ex_json['pointerToViolation'] == '#') {
                log.error "* ${ex_json['message']}"
            }
            // Error with specific param
            else {
                def param = ex_json['pointerToViolation'] - ~/^#\//
                def param_val = params_json[param].toString()
                if (enums.containsKey(param)) {
                    def error_msg = "* --${param}: '${param_val}' is not a valid choice (Available choices"
                    if (enums[param].size() > limit) {
                        log.error "${error_msg} (${limit} of ${enums[param].size()}): ${enums[param][0..limit-1].join(', ')}, ... )"
                    } else {
                        log.error "${error_msg}: ${enums[param].join(', ')})"
                    }
                } else {
                    log.error "* --${param}: ${ex_json['message']} (${param_val})"
                }
            }
        }
        for (ex in causingExceptions) {
            printExceptions(ex, params_json, log, enums)
        }
    }
    */

    //
    // Remove an element from a JSONArray
    //
    /*
    private static JSONArray removeElement(json_array, element) {
        def list = []
        int len = json_array.length()
        for (int i=0;i<len;i++){
            list.add(json_array.get(i).toString())
        }
        list.remove(element)
        JSONArray jsArray = new JSONArray(list)
        return jsArray
    }
    */

    //
    // Remove ignored parameters
    //
    /*
    private static JSONObject removeIgnoredParams(raw_schema, params) {
        // Remove anything that's in params.schema_ignore_params
        params.schema_ignore_params.split(',').each{ ignore_param ->
            if(raw_schema.keySet().contains('definitions')){
                raw_schema.definitions.each { definition ->
                    for (key in definition.keySet()){
                        if (definition[key].get("properties").keySet().contains(ignore_param)){
                            // Remove the param to ignore
                            definition[key].get("properties").remove(ignore_param)
                            // If the param was required, change this
                            if (definition[key].has("required")) {
                                def cleaned_required = removeElement(definition[key].required, ignore_param)
                                definition[key].put("required", cleaned_required)
                            }
                        }
                    }
                }
            }
            if(raw_schema.keySet().contains('properties') && raw_schema.get('properties').keySet().contains(ignore_param)) {
                raw_schema.get("properties").remove(ignore_param)
            }
            if(raw_schema.keySet().contains('required') && raw_schema.required.contains(ignore_param)) {
                def cleaned_required = removeElement(raw_schema.required, ignore_param)
                raw_schema.put("required", cleaned_required)
            }
        }
        return raw_schema
    }
    */



    //
    // This function tries to read a JSON params file
    //
    /*
    private static LinkedHashMap paramsLoad(String projectdir, ArrayList json_schema) {
        def params_map = new LinkedHashMap()
        try {
            params_map = paramsRead(projectdir, json_schema)
        } catch (Exception e) {
            println "Could not read parameters settings from JSON. $e"
            params_map = new LinkedHashMap()
        }
        return params_map
    }
    */

    //
    // Method to actually read in JSON file using Groovy.
    // Group (as Key), values are all parameters
    //    - Parameter1 as Key, Description as Value
    //    - Parameter2 as Key, Description as Value
    //    ....
    // Group
    //    -
    /*
    private static LinkedHashMap paramsRead(String projectdir, ArrayList json_schema) throws Exception {
         def Map schema_definitions = [:]
         def Map schema_properties = [:]

        for (schema in json_schema) {
            def json = new File(getSchemaPath(projectdir, schema)).text
            schema_definitions += (Map) new JsonSlurper().parseText(json).get('definitions')
            //schema_properties += (Map) new JsonSlurper().parseText(json).get('properties')
        }
        /* Tree looks like this in nf-core schema
        * definitions <- this is what the first get('definitions') gets us
                group 1
                    title
                    description
                        properties
                        parameter 1
                            type
                            description
                        parameter 2
                            type
                            description
                group 2
                    title
                    description
                        properties
                        parameter 1
                            type
                            description
        * properties <- parameters can also be ungrouped, outside of definitions
                parameter 1
                    type
                    description
        */
    /*
        // Grouped params
        def params_map = new LinkedHashMap()
        schema_definitions.each { key, val ->
            def Map group = schema_definitions."$key".properties // Gets the property object of the group
            def title = schema_definitions."$key".title
            def sub_params = new LinkedHashMap()
            group.each { innerkey, value ->
                sub_params.put(innerkey, value)
            }
            params_map.put(title, sub_params)
        }

        // Ungrouped params
        def ungrouped_params = new LinkedHashMap()
        schema_properties.each { innerkey, value ->
            ungrouped_params.put(innerkey, value)
        }
        params_map.put("Other parameters", ungrouped_params)

        return params_map
    }

    //
    // Get maximum number of characters across all parameter names
    //
    private static Integer paramsMaxChars(params_map) {
        Integer max_chars = 0
        for (group in params_map.keySet()) {
            def group_params = params_map.get(group)  // This gets the parameters of that particular group
            for (param in group_params.keySet()) {
                if (param.size() > max_chars) {
                    max_chars = param.size()
                }
            }
        }
        return max_chars
    }
    */
}
