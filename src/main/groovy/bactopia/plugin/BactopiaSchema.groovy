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

import static nextflow.bactopia.nfschema.Common.getBasePath
import static nextflow.bactopia.BactopiaUtils.isLocal
import static nextflow.bactopia.BactopiaUtils.isPositiveInteger
import static nextflow.bactopia.BactopiaUtils.fileNotFound
import static nextflow.bactopia.BactopiaUtils.fileNotGzipped
import static nextflow.bactopia.BactopiaTemplate.getLogColors
import static nextflow.bactopia.BactopiaTemplate.logError

@Slf4j
class BactopiaSchema {

    private ValidatorFactory validator
    private BactopiaConfig config
    private Boolean isBactopiaTool = false

    BactopiaSchema(BactopiaConfig config) {
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

    public String validateParameters(
        Map options = null,
        Map inputParams = [:],
        String baseDir,
        Boolean isBactopiaTool = false
    ) {
        String run_type = ""
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
        def  validationErrors = validationResult[0]
        def unevaluatedParams = validationResult[1]
        //log.info "Validation errors: ${validationErrors}"
        //log.info "Unevaluated parameters: ${unevaluatedParams}"

        if (validationErrors.size() > 0) {
            logError("The following parameters are invalid:")
            for (error in validationErrors) {
                logError("    ${error}")
            }
            log.info " "
            log.error "Validation of pipeline parameters failed! Please correct to continue"
            System.exit(1)
        }

        if (isBactopiaTool) {
            run_type = validateBactopiaToolParams(inputParams)
        } else {
            run_type = validateBactopiaParams(inputParams)
        }

        log.debug "Finishing parameters validation"

        return run_type
    }


    //------------------------------------------------------------------------------------------------------------------
    // BEGIN - nextflow/nf-schema functions
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
    // END - nextflow/nf-schema functions


    public static String validateBactopiaToolParams(Map params) {
        def Integer error = 0
        def ArrayList missing_required = []

        // General Bactopia Tool parameter checks
        if (params.bactopia) {
            if (fileNotFound(params.bactopia, "bactopia")) {
                error += 1
                missing_required << "--bactopia"
            }
        } else {
            missing_required << "--bactopia"
        }

        if (params.include && params.exclude) {
            logError("'--include' and '--exclude' cannot be used together")
            error += 1
        } else if (params.include) {
            error += fileNotFound(params.include, "include")
        } else if (params.exclude) {
            error += fileNotFound(params.exclude, "exclude")
        }

        // Workflow specific parameter checks
        if (params.workflow.name == "ariba") {
            if (!params.ariba_db) {
                error += 1
                missing_required << "--ariba_db"
            }
        } else if (params.workflow.name == "bakta") {
            if (params.bakta_db) {
                if (!params.download_bakta) {
                    if (params.bakta_db.endsWith(".tar.gz")) {
                        error += fileNotFound(params.bakta_db, "bakta_db")
                    } else {
                        error += fileNotFound("${params.bakta_db}/bakta.db", "bakta_db")
                    }
                }
            } else {
                missing_required << "--bakta_db"
            }
        } else if (params.workflow.name == "blastn") {
            if (params.blastn_query) {
                error += fileNotFound(params.blastn_query, "blastn_query")
            } else {
                missing_required << "--blastn_query"
            }
        } else if (params.workflow.name == "blastp") {
            if (params.blastp_query) {
                error += fileNotFound(params.blastp_query, "blastp_query")
            } else {
                missing_required << "--blastp_query"
            }
        } else if (params.workflow.name == "blastx") {
            if (params.blastx_query) {
                error += fileNotFound(params.blastx_query, "blastx_query")
            } else {
                missing_required << "--blastx_query"
            }
        } else if (params.workflow.name == "eggnog") {
            if (params.eggnog_db) {
                if (!params.download_eggnog) {
                    if (params.eggnog_db.endsWith(".tar.gz")) {
                        error += fileNotFound(params.eggnog_db, "eggnog_db")
                    } else {
                        error += fileNotFound("${params.eggnog_db}/eggnog.db", "eggnog_db")
                    }
                }
            } else {
                missing_required << "--eggnog_db"
            }
        } else if (params.workflow.name == "gamma") {
            if (params.gamma_db) {
                error += fileNotFound(params.gamma_db, "gamma_db")
            } else {
                missing_required << "--gamma_db"
            }
        } else if (params.workflow.name == "gtdb") {
            if (params.gtdb) {
                if (!params.download_gtdb) {
                    if (params.gtdb.endsWith(".tar.gz")) {
                        error += fileNotFound(params.gtdb, "gtdb")
                    } else {
                        error += fileNotFound("${params.gtdb}/metadata/metadata.txt", "gtdb")
                    }
                }
            } else {
                missing_required << "--gtdb"
            }
        } else if (params.workflow.name == "kraken2") {
            if (params.kraken2_db) {
                if (params.kraken2_db.endsWith(".tar.gz")) {
                    error += fileNotFound(params.kraken2_db, "kraken2_db")
                } else {
                    error += fileNotFound("${params.kraken2_db}/hash.k2d", "kraken2_db")
                }
            } else {
                missing_required << "--kraken2_db"
            }
        } else if (params.workflow.name == "mashdist") {
            if (params.mash_sketch) {
                error += fileNotFound(params.mash_sketch, "mash_sketch")
            } else {
                missing_required << "--mash_sketch"
            }
        } else if (params.workflow.name == "midas") {
            if (params.midas_db) {
                if (params.midas_db.endsWith(".tar.gz")) {
                    error += fileNotFound(params.midas_db, "midas_db")
                } else {
                    error += fileNotFound("${params.midas_db}/genome_info.txt", "midas_db")
                }
            } else {
                missing_required << "--midas_db"
            }
        } else if (params.workflow.name == "mykrobe") {
            if (!params.mykrobe_species) {
                error += 1
                missing_required << "--mykrobe_species"
            }
        } else if (params.workflow.name == "pangenome") {
            if (params.traits) {
                error += fileNotFound(params.traits, "traits")
            }
        } else if (params.workflow.name == "scoary") {
            if (params.traits) {
                error += fileNotFound(params.traits, "traits")
            } else {
                missing_required << "--traits"
            }
        } else if (params.workflow.name == "snippy") {
            if (params.accession && params.reference) {
                log.error "'--accession' and '--reference' cannot be used together"
                error += 1
            } else if (params.reference) {
                error += fileNotFound(params.reference, "reference")
            } else if (!params.accession && !params.reference) {
                log.error "Either '--accession' and '--reference' is required"
                error += 1
            }
        } else if (params.workflow.name == "srahumanscrubber") {
            if (params.scrubber_db) {
                if (!params.download_scrubber) {
                    error += fileNotFound(params.scrubber_db, "scrubber_db")
                }
            } else {
                missing_required << "--scrubber_db"
            }
        } else if (params.workflow.name == "sylph") {
            if (params.sylph_db) {
                error += fileNotFound(params.sylph_db, "sylph_db")
            } else {
                missing_required << "--sylph_db"
            }
        } else if (params.workflow.name == "tblastn") {
            if (params.tblastn_query) {
                error += fileNotFound(params.tblastn_query, "tblastn_query")
            } else {
                missing_required << "--tblastn_query"
            }
        } else if (params.workflow.name == "tblastx") {
            if (params.tblastx_query) {
                error += fileNotFound(params.tblastx_query, "tblastx_query")
            } else {
                missing_required << "--tblastx_query"
            }
        }

        // If errors print outcome
        if (missing_required.size() > 0) {
            logError("Required parameters are missing, please check: " + missing_required.join(", "))
            error += 1
        }

        if (error > 0) {
            log.error("\nValidation of pipeline parameters failed! Please correct to continue")
            System.exit(1)
        }

        return "success"
    }


    public static String validateBactopiaParams(Map params) {
        def Integer error = 0
        def String run_type = ""

        if (params.samples) {
            error += fileNotFound(params.samples, "samples")
            run_type = "is_fofn"
        } else if (params.r1 && params.r2 && params.ont && params.short_polish && params.sample) {
            if (isLocal(params.r1)) {
                error += fileNotGzipped(params.r1, "r1")
            }
            if (isLocal(params.r2)) {
                error += fileNotGzipped(params.r2, "r2")
            }
            if (isLocal(params.ont)) {
                error += fileNotGzipped(params.ont, "ont")
            }
            run_type = "short_polish"
        } else if (params.r1 && params.r2 && params.ont && params.hybrid && params.sample) {
            if (isLocal(params.r1)) {
                error += fileNotGzipped(params.r1, "r1")
            }
            if (isLocal(params.r2)) {
                error += fileNotGzipped(params.r2, "r2")
            }
            if (isLocal(params.ont)) {
                error += fileNotGzipped(params.ont, "ont")
            }
            run_type = "hybrid"
        } else if (params.r1 && params.r2 && params.se) {
            logError("Cannot use --r1, --r2, and --se together")
            error += 1
        } else if (params.r1 && params.r2 && params.ont) {
            logError("Cannot use --r1, --r2, and --ont together, unless using --short_polish or --hybrid")
            error += 1
        } else if (params.ont && params.se) {
            logError("Cannot use --ont and --se together")
            error += 1
        } else if (params.r1 && params.r2 && params.sample) {
            if (isLocal(params.r1)) {
                error += fileNotGzipped(params.r1, "r1")
            }
            if (isLocal(params.r2)) {
                error += fileNotGzipped(params.r2, "r2")
            }
            run_type = "paired-end"
        } else if (params.ont && params.sample) {
            if (isLocal(params.ont)) {
                error += fileNotGzipped(params.ont, "ont")
            }
            run_type = "ont"
        } else if (params.se && params.sample) {
            if (isLocal(params.se)) {
                error += fileNotGzipped(params.se, "se")
            }
            run_type = "single-end"
        } else if (params.assembly && params.sample) {
            if (isLocal(params.assembly)) {
                error += fileNotGzipped(params.assembly, "assembly")
            }
            run_type = "assembly"
        } else if (params.accessions) {
            error += fileNotFound(params.accessions, "accessions")
            run_type = "is_accessions"
        } else if (params.accession) {
            run_type = "is_accession"
        } else {
            logError("One or more required parameters are missing, please check and try again.")
            error += 1
        }

        if (params.check_samples && !params.samples) {
            logError("To use --check_samples, you must also provide a FOFN to check using --samples.")
            error += 1
        }

        if (params.max_downloads >= 10) {
            log.warn "Please be aware the value you have set for --max_downloads (${params.max_downloads}) may cause NCBI " +
                     "to temporarily block your IP address due to too many queries at once."
        }

        if (params.genome_size) {
            error += isPositiveInteger(params.genome_size, "genome_size")
        }

        if (params.containsKey('adaptors')) {
            if (params.adaptors) {
                if (isLocal(params.adaptors)) {
                    error += fileNotFound(params.adaptors, "adaptors")
                }
            }

            if (params.phix) {
                if (isLocal(params.phix)) {
                    error += fileNotFound(params.phix, "phix")
                }
            }
        }

        // following should only be checked for specific workflows
        if (['bactopia', 'staphopia'].contains(params.workflow.name)) {
            if (params.use_bakta) {
                if (params.bakta_db) {
                    if (!params.download_bakta) {
                        if (params.bakta_db.endsWith(".tar.gz")) {
                            error += fileNotFound(params.bakta_db, "bakta_db")
                        } else {
                            error += fileNotFound("${params.bakta_db}/bakta.db", "bakta_db")
                        }
                    }
                } else {
                    logError("Bactopia requires --bakta_db to be set when using --use_bakta")
                    error += 1
                }
            }
        } else if (params.workflow.name = "teton") {
            if (params.kraken2_db) {
                if (isLocal(params.kraken2_db)) {
                    if (params.kraken2_db.endsWith(".tar.gz")) {
                        error += fileNotFound(params.kraken2_db, "kraken2_db")
                    } else {
                        error += fileNotFound("${params.kraken2_db}/hash.k2d", "kraken2_db")
                    }
                }
            } else 
            if (params.kraken2_db) {
                logError("Teton requires '--kraken2_db' to be provided")
                error += 1
            }
        }

        if (error > 0) {
            log.error "\nValidation of pipeline parameters failed! Please correct to continue"
            System.exit(1)
        }

        return run_type
    }

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

}
