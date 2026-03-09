package bactopia.plugin.inputs

import groovy.util.logging.Slf4j
import java.nio.file.Path

import static bactopia.plugin.utils.EmptyFiles.getEmptyPaths
import static bactopia.plugin.utils.EmptyFiles.isEmptyFile
import static bactopia.plugin.BactopiaUtils.fileExists
import static bactopia.plugin.BactopiaTemplate.dashedLine

@Slf4j
class BactopiaTools {
    /**
     * Collect the input samples from the Bactopia directory to be used by a given Bactopia Tool.
     *
     * @param params The workflow parameters
     * @return List of collected sample inputs
     */
    public static List collectBactopiaToolInputs(Map params) {
        def List inclusions = processFOFN(params.include, true)
        def Boolean includeAll = inclusions.isEmpty()
        def List exclusions = processFOFN(params.exclude, false)
        def List ignoreList = ['.nextflow', 'bactopia-info', 'bactopia-tools', 'work', 'bactopia-runs', 'pipeline_info']

        // Check if params.bactopia exists, and if so loop through it
        def List samples = []
        def List missing = []
        def Map EMPTY_PATHS = getEmptyPaths(params.empty_path)
        if (params.bactopia) {
            Path bactopiaPath = Path.of(params.bactopia)
            if (bactopiaPath.exists()) {
                // loop through the Bactopia directory and collect the samples
                bactopiaPath.eachFile { item ->
                    if (item.isDirectory()) {
                        def String sample = item.getName()
                        if (!ignoreList.contains(sample)) {
                            if (inclusions.contains(sample) || includeAll) {
                                if (!exclusions.contains(sample)) {
                                    if (_isSampleDir(sample, params.bactopia)) {
                                        def Map inputs = _collectInputs(sample, params.bactopia, params.workflow.ext, EMPTY_PATHS)
                                        if (inputs.missing_required) {
                                            missing << inputs.meta.id
                                        } else {
                                            samples << inputs
                                        }
                                    } else {
                                        log.info("${sample} does not appear to be a Bactopia sample, skipping...")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                log.error "The Bactopia directory ${params.bactopia} (--bactopia) does not exist."
            }
        } else {
            log.error "--bactopia is not set."
        }

        if (samples.size() == 0) {
            log.error(
                "No samples were found to process! Please verify the --bactopia path \n" +
                "is correct and that it contains Bactopia results."
            )
        } else {
            log.info("Found ${samples.size()} samples to process")
            if (missing.size() > 0) {
                log.warn("${missing.size()} samples were excluded due to missing files. They are:")
                for (sample in missing) {
                    log.warn("    ${sample}")
                }
            }
            log.info("\nIf this looks wrong, now's your chance to back out (CTRL+C 3 times).")
            log.info("Sleeping for 5 seconds...\n")
            log.info dashedLine()
        }
        return samples
    }

    /**
     * Process the include/exclude FOFN files.
     *
     * @param fofn The FOFN file path
     * @param isInclude Whether this is an inclusion or exclusion list
     * @return List of sample names
     */
    private static List processFOFN(String fofn, Boolean isInclude) {
        def List samples = []

        if (fofn) {
            // Check if the file exists, and if so collect the samples
            if (fileExists(fofn)) {
                new File(fofn).eachLine { line ->
                    def sample = line.trim().split('\t')[0]
                    if (sample) {
                        samples << sample
                    }
                }
            }

            // If samples were found, log the number of samples
            if (samples.size() > 0) {
                if (isInclude) {
                    log.info("Including ${samples.size()} samples for analysis")
                } else {
                    log.info("Excluding ${samples.size()} samples from the analysis")
                }
            }
        }
        return samples
    }

    /**
     * Test if the sample directory is likely to contain Bactopia results.
     *
     * @param sample The sample name
     * @param dir The Bactopia directory path
     * @return true if directory contains Bactopia results, false otherwise
     */
    private static Boolean _isSampleDir(String sample, String dir) {
        return fileExists("${dir}/${sample}/main/gather/${sample}-meta.tsv")
    }

    /**
     *
     * Checks if an extension has the required files present in the Bactopia output directory.
     *
     * @params files A list of file paths to check
     * @return true if any required files are missing, false otherwise
     */
    private static Boolean _missingRequiredFiles(List files) {
        for (file in files) {
            if (isEmptyFile(file)) {
                return true
            }
        }
        return false
    }

    /**
     * Find annotation file, preferring Bakta over Prokka, and .gz over uncompressed.
     * Returns null if not found (caller keeps existing default).
     *
     * @param baseDir The base directory path
     * @param subdir The subdirectory for the annotation type
     * @param sample The sample name
     * @param baktaExt The Bakta file extension
     * @param prokkaExt The Prokka file extension
     * @return The found file path or null
     */
    private static Path _findAnnotationFile(String baseDir, String subdir, String sample,
                                            String baktaExt, String prokkaExt) {
        // Try Bakta first
        def String bakta = "${baseDir}/${subdir}/bakta/${sample}.${baktaExt}"
        if (fileExists("${bakta}.gz")) return Path.of("${bakta}.gz")
        if (fileExists(bakta)) return Path.of(bakta)

        // Fall back to Prokka
        def String prokka = "${baseDir}/${subdir}/prokka/${sample}.${prokkaExt}"
        if (fileExists("${prokka}.gz")) return Path.of("${prokka}.gz")
        if (fileExists(prokka)) return Path.of(prokka)

        // File not found
        return null
    }

    /**
     * Navigate the Bactopia output directory and collect the inputs for a given Bactopia Tool.
     *
     * @param sample The sample name
     * @param dir The Bactopia directory path
     * @param extension The tool-specific extension configuration
     * @return Map containing collected inputs or error message
     */
    private static Map _collectInputs(String sample, String dir, String extension, Map EMPTY_PATHS) {
        def Map PATHS = [:]
        PATHS.blastdb = "annotator"
        PATHS.fastq = "qc"
        PATHS.fna = "assembler"
        PATHS.faa = "annotator"
        PATHS.gbk = "annotator"
        PATHS.gff = "annotator"
        PATHS.meta = "gather"

        // Set up the paths for each extension
        def String baseDir = "${dir}/${sample}/main/"
        def String se = "${baseDir}/${PATHS['fastq']}/${sample}.fastq.gz"
        def String pe1 = "${baseDir}/${PATHS['fastq']}/${sample}_R1.fastq.gz"
        def String pe2 = "${baseDir}/${PATHS['fastq']}/${sample}_R2.fastq.gz"
        def String fna = "${baseDir}/${PATHS['fna']}/${sample}.fna"
        def String meta_file = "${baseDir}/${PATHS['meta']}/${sample}-meta.tsv"
        
        // Check if the SE reads are ONT or Illumina
        def Boolean ont = false
        if (fileExists("${baseDir}/${PATHS['fastq']}/supplemental/${sample}-final_NanoPlot-report.html")) {
            // the se read is ONT data
            ont = true
        }

        // Collect all possible input types
        def Map inputs = [
            'meta': ['id':sample, 'name':sample],
            'meta_file': fileExists(meta_file) ? meta_file : EMPTY_PATHS.empty_meta,
            'r1': fileExists(pe1) ? pe1 : EMPTY_PATHS.empty_r1,
            'r2': fileExists(pe2) ? pe2 : EMPTY_PATHS.empty_r2,
            'se': fileExists(se) && !ont ? se : EMPTY_PATHS.empty_se,
            'ont': fileExists(se) && ont ? se : EMPTY_PATHS.empty_ont,
            'assembly': fileExists(fna) ? fna : fileExists("${fna}.gz") ? "${fna}.gz" : EMPTY_PATHS.empty_assembly,
            'proteins': EMPTY_PATHS.empty_proteins,
            'gbk': EMPTY_PATHS.empty_gbk,
            'gff': EMPTY_PATHS.empty_gff,
            'blastdb': EMPTY_PATHS.empty_blastdb,
            'missing_required': false
        ]

        // Handle annotations
        inputs['proteins'] = _findAnnotationFile(baseDir, PATHS['faa'], sample, 'faa', 'faa') ?: inputs['proteins']
        inputs['gff'] = _findAnnotationFile(baseDir, PATHS['gff'], sample, 'gff3', 'gff') ?: inputs['gff']
        inputs['gbk'] = _findAnnotationFile(baseDir, PATHS['gbk'], sample, 'gbff', 'gbk') ?: inputs['gbk']

        def String blastdb = "${baseDir}/${PATHS['blastdb']}/bakta/${sample}-blastdb.tar.gz"
        blastdb = fileExists(blastdb) ? blastdb : "${baseDir}/${PATHS['blastdb']}/prokka/${sample}-blastdb.tar.gz"
        if (fileExists(blastdb)) { 
            inputs['blastdb'] = blastdb
        }

        // Edit meta map based on extension
        // Extensions with no additions: fna_faa_gff, fna_faa, fna_meta, blastdb, gbk, gff, proteins
        def List required_files = []
        if (extension == "illumina_fastq") {
            inputs['meta'].runtype = 'illumina'
            inputs['meta'].single_end = (fileExists(pe1) && fileExists(pe2)) ? false : true

            if (inputs['meta'].single_end) {
                required_files << inputs['se']
            } else {
                required_files << inputs['r1']
                required_files << inputs['r2']
            }
        } else if (extension == 'fastq') {
            if (fileExists(se)) {
                inputs['meta'].single_end = true
                inputs['meta'].runtype = (ont) ? 'ont' : 'illumina'

                if (ont) {
                    required_files << inputs['ont']
                } else {
                    required_files << inputs['se']
                }
            } else if (fileExists(pe1) && fileExists(pe2)) {
                inputs['meta'].single_end = false
                inputs['meta'].runtype = 'illumina'
                required_files << inputs['r1']
                required_files << inputs['r2']
            }
        } else if (extension == 'fna_fastq') {
            required_files << inputs['assembly']
            if (fileExists(se)) {
                inputs['meta'].single_end = true
                inputs['meta'].runtype = (ont) ? 'ont' : 'illumina'
                if (ont) {
                    required_files << inputs['ont']
                } else {
                    required_files << inputs['se']
                }
            } else if (fileExists(pe1) && fileExists(pe2)) {
                inputs['meta'].single_end = false
                inputs['meta'].runtype = 'illumina'
                required_files << inputs['r1']
                required_files << inputs['r2']
            }
        } else if (extension == 'fna_faa_gff') {
            required_files << inputs['assembly']
            required_files << inputs['proteins']
            required_files << inputs['gff']
        } else if (extension == 'fna_faa') {
            required_files << inputs['assembly']
            required_files << inputs['proteins']
        } else if (extension == 'fna_meta') {
            required_files << inputs['assembly']
            required_files << inputs['meta_file']
        } else if (extension == 'blastdb') {
            required_files << inputs['blastdb']
        } else if (extension == 'gbk') {
            required_files << inputs['gbk']
        } else if (extension == 'gff') {
            required_files << inputs['gff']
        } else if (extension == 'proteins') {
            required_files << inputs['proteins']
        }

        // Check for missing required files
        inputs['missing_required'] = _missingRequiredFiles(required_files)

        // Convert all non-meta, non-boolean fields to Path for record type compatibility
        inputs.each { key, value ->
            if (value != null && !(value instanceof Path) && !(value instanceof Map) && !(value instanceof Boolean)) {
                inputs[key] = Path.of(value.toString())
            }
        }

        return inputs
    }
}
