package bactopia.plugin.inputs

import groovy.util.logging.Slf4j
import java.nio.file.Path
import nextflow.util.RecordMap

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
                                        def Map inputs = _collectInputs(sample, params.bactopia, params.workflow.ext)
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
            if (file == null || isEmptyFile(file)) {
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
     * Keys in the returned map use the standardized ext vocabulary:
     * fna, fna_anno, faa, gff, gbk, tsv_meta, blastdb, r1, r2, se, lr
     *
     * @param sample The sample name
     * @param dir The Bactopia directory path
     * @param ext List of required input keys from the controlled vocabulary
     * @return Map containing collected inputs or error message
     */
    private static Map _collectInputs(String sample, String dir, List<String> ext) {
        def Map PATHS = [:]
        PATHS.blastdb   = "annotator"
        PATHS.fastq     = "qc"
        PATHS.fna       = "assembler"
        PATHS.fna_anno  = "annotator"
        PATHS.faa       = "annotator"
        PATHS.gbk       = "annotator"
        PATHS.gff       = "annotator"
        PATHS.tsv_meta  = "gather"

        // Set up file paths
        def String baseDir = "${dir}/${sample}/main/"
        def String sePath = "${baseDir}/${PATHS['fastq']}/${sample}_SE.fastq.gz"
        def String ontPath = "${baseDir}/${PATHS['fastq']}/${sample}_ONT.fastq.gz"
        def String pe1Path = "${baseDir}/${PATHS['fastq']}/${sample}_R1.fastq.gz"
        def String pe2Path = "${baseDir}/${PATHS['fastq']}/${sample}_R2.fastq.gz"
        def String fnaPath = "${baseDir}/${PATHS['fna']}/${sample}.fna"
        def String tsvMetaPath = "${baseDir}/${PATHS['tsv_meta']}/${sample}-meta.tsv"

        // Check if the SE reads are ONT or Illumina
        def Boolean isOnt = fileExists("${baseDir}/${PATHS['fna']}/supplemental/ont.txt")

        // Collect all possible input types using standardized key names
        def Map inputs = [
            'meta': ['id': sample, 'name': sample],
            'tsv_meta': fileExists(tsvMetaPath) ? tsvMetaPath : null,
            'r1': fileExists(pe1Path) ? pe1Path : null,
            'r2': fileExists(pe2Path) ? pe2Path : null,
            'se': fileExists(sePath) ? sePath : null,
            'lr': fileExists(ontPath) ? ontPath : null,
            'fna': fileExists(fnaPath) ? fnaPath : fileExists("${fnaPath}.gz") ? "${fnaPath}.gz" : null,
            'faa': null,
            'fna_anno': null,
            'gbk': null,
            'gff': null,
            'blastdb': null,
            'missing_required': false
        ]

        // Handle annotations (prefer Bakta over Prokka)
        inputs['faa'] = _findAnnotationFile(baseDir, PATHS['faa'], sample, 'faa', 'faa') ?: inputs['faa']
        inputs['fna_anno'] = _findAnnotationFile(baseDir, PATHS['fna_anno'], sample, 'fna', 'fna') ?: inputs['fna_anno']
        inputs['gff'] = _findAnnotationFile(baseDir, PATHS['gff'], sample, 'gff3', 'gff') ?: inputs['gff']
        inputs['gbk'] = _findAnnotationFile(baseDir, PATHS['gbk'], sample, 'gbff', 'gbk') ?: inputs['gbk']

        def String blastdb = "${baseDir}/${PATHS['blastdb']}/bakta/${sample}-blastdb.tar.gz"
        blastdb = fileExists(blastdb) ? blastdb : "${baseDir}/${PATHS['blastdb']}/prokka/${sample}-blastdb.tar.gz"
        if (fileExists(blastdb)) {
            inputs['blastdb'] = blastdb
        }

        // Determine required files from ext list
        def Set<String> READ_KEYS = ['r1', 'r2', 'se', 'lr'] as Set
        def List<String> resolved = ext.contains('fastq')
            ? (ext.findAll { it != 'fastq' } + ['r1', 'r2', 'se', 'lr']).unique()
            : new ArrayList<>(ext)
        def List<String> nonReadKeys = resolved.findAll { !(it in READ_KEYS) }
        def List<String> requestedReadKeys = resolved.findAll { it in READ_KEYS }

        // Non-read keys: all required (AND logic)
        def List required_files = []
        nonReadKeys.each { String key -> required_files << inputs[key] }

        // Read keys: detect available reads, set metadata, require at least one valid set
        if (requestedReadKeys) {
            def boolean readFound = false

            // Check for long reads (lr) (short_polish or ont)
            if ('lr' in requestedReadKeys && inputs['lr'] && isOnt) {
                inputs['meta'].single_end = true
                inputs['meta'].runtype = 'ont'
                required_files << inputs['lr']
                readFound = true
            }

            // Check for SE illumina reads (single_end non-ONT)
            if (!readFound && 'se' in requestedReadKeys && inputs['se'] && !isOnt) {
                inputs['meta'].single_end = true
                inputs['meta'].runtype = 'illumina'
                required_files << inputs['se']
                readFound = true
            }

            // Check for PE illumina reads (hybrid or illumina_pe)
            if (!readFound && 'r1' in requestedReadKeys && 'r2' in requestedReadKeys && inputs['r1'] && inputs['r2'] && !isOnt) {
                inputs['meta'].single_end = false
                inputs['meta'].runtype = 'illumina'
                required_files << inputs['r1']
                required_files << inputs['r2']
                readFound = true
            }

            // No acceptable reads found
            if (!readFound) {
                required_files << null
            }
        }

        // Check for missing required files
        inputs['missing_required'] = _missingRequiredFiles(required_files)

        // Convert all non-meta, non-boolean fields to Path for record type compatibility
        inputs.each { String key, value ->
            if (value != null && !(value instanceof Path) && !(value instanceof Map) && !(value instanceof Boolean)) {
                inputs[key] = Path.of(value.toString())
            }
        }

        // Promote meta to RecordMap so downstream Nextflow processes receive a Record
        inputs['meta'] = new RecordMap(inputs['meta'] as Map)

        return inputs
    }
}
