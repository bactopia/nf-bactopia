package nextflow.bactopia.inputs

import groovy.util.logging.Slf4j
import java.nio.file.Path

import static nextflow.bactopia.BactopiaUtils.fileExists

@Slf4j
class BactopiaTools {


    //
    // Collect the input samples from the Bactopia directory to be used by a given Bactopia Tool
    //
    public static List collectInputs(String bactopiaDir, String extension, String includeFile, String excludeFile) {
        def Boolean includeAll = true
        def List inclusions = processFOFN(includeFile, true)
        def List exclusions = processFOFN(excludeFile, false)
        def List ignoreList = ['.nextflow', 'bactopia-info', 'bactopia-tools', 'work', 'bactopia-runs', 'pipeline_info']

        // Check if bactopiaDir exists, and if so loop through it
        def List samples = []
        def List missing = []
        if (bactopiaDir) {
            Path bactopiaPath = Path.of(bactopiaDir)
            if (bactopiaPath.exists()) {
                // loop through the Bactopia directory and collect the samples
                bactopiaPath.eachFile { item ->
                    if (item.isDirectory()) {
                        def String sample = item.getName()
                        if (!ignoreList.contains(sample)) {
                            if (inclusions.contains(sample) || includeAll) {
                                if (!exclusions.contains(sample)) {
                                    if (_isSampleDir(sample, bactopiaDir)) {
                                        def List inputs = _collectInputs(sample, bactopiaDir, extension)
                                        log.info ("inputs: ${inputs.getClass()}")
                                        if (inputs[0] instanceof String) {
                                            missing << inputs
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
                log.error("The Bactopia directory ${bactopiaDir} (--bactopia) does not exist.")
            }
        } else {
            log.error("--bactopia is is not set.")
            System.exit(1)
        }

        log.info("Found ${samples.size()} samples to process")
        if (missing.size() > 0) {
            log.warn("${missing.size()} samples were excluded due to missing files. They are:")
            for (sample in missing) {
                log.warn("    ${sample}")
            }
        }
        log.info("\nIf this looks wrong, now's your chance to back out (CTRL+C 3 times).")
        log.info("Sleeping for 5 seconds...")
        log.info("--------------------------------------------------------------------")
        sleep(5000)
        return samples
    }


    //
    // Process the include/exclude FOFN files
    //
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
                    log.info "Including ${samples.size()} samples for analysis"
                } else {
                    log.info "Excluding ${samples.size()} samples from the analysis"
                }
            }
        }
        return samples
    }


    //
    // Test if the sample directory is likely to contain Bactopia results
    //
    private static Boolean _isSampleDir(String sample, String dir) {
        return fileExists("${dir}/${sample}/main/gather/${sample}-meta.tsv")
    }


    //
    // Navigate the Bactopia output directory and collect the inputs for a given Bactopia Tool
    //
    private static List _collectInputs(String sample, String dir, String extension) {
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
        def String meta = "${baseDir}/${PATHS['meta']}/${sample}-meta.tsv"
        
        // Check if the SE reads are ONT or Illumina
        def Boolean ont = false
        if (fileExists("${baseDir}/${PATHS['fastq']}/summary/${sample}-final_NanoPlot-report.html")) {
            // the se read is ONT data
            ont = true
        }

        // Determine the inputs files required for the given extension
        // NOTE: Remote files will be assumed to exist
        //
        // Return List looks like:
        // [ [id:sample, single_end:true/false, runtype:'illumina'/'ont'], [file1], [file2], ... ]
        // 0 - meta map
        // 1 - input files
        // 2 - extra files
        // 3 - extra files
        if (extension == "illumina_fastq") {
            // Prioritize PE reads first
            if (fileExists(pe1) && fileExists(pe2)) {
                return [[id:sample, single_end:false, runtype:'illumina'], [pe1, pe2], [], []]
            } else if (fileExists(se) && !ont) {
                return [[id:sample, single_end:true, runtype:'illumina'], [se], [], []]
            }
        } else if (extension == 'fastq') {
            if (fileExists(se)) {
                if (ont) {
                    return [[id:sample, single_end:true, runtype:'ont'], [se], [], []]
                } else {
                    return [[id:sample, single_end:true, runtype:'illumina'], [se], []]
                }
            } else if (fileExists(pe1) && fileExists(pe2)) {
                return [[id:sample, single_end:false, runtype:'illumina'], [pe1, pe2], [], []]
            }
        } else if (extension == 'fna_fastq') {
            if (fileExists(se)) {
                def String runtype = "illumina"
                if (ont) {
                    runtype = "ont"
                }

                if (fileExists("${fna}.gz")) {
                    return [[id:sample, single_end:true, is_compressed:true, runtype:runtype], ["${fna}.gz"], [se], []]
                } else if (fileExists(fna)) {
                    return [[id:sample, single_end:true, is_compressed:false, runtype:runtype], [fna], [se], []]
                }
            } else if (fileExists(pe1) && fileExists(pe2)) {
                if (fileExists("${fna}.gz")) {
                    return [[id:sample, single_end:false, is_compressed:true, runtype:'illumina'], ["${fna}.gz"], [pe1, pe2], []]
                } else if (fileExists(fna)) {
                    return [[id:sample, single_end:false, is_compressed:false, runtype:'illumina'], [fna], [pe1, pe2], []]
                }
            }
        } else if (extension == 'fna_faa_gff') {
            // Default to Bakta faa
            fna = "${baseDir}/${PATHS['faa']}/bakta/${sample}.fna"
            def String faa = "${baseDir}/${PATHS['faa']}/bakta/${sample}.faa"
            def String gff = "${baseDir}/${PATHS['faa']}/bakta/${sample}.gff3"
            if (!fileExists(faa) && !fileExists("${faa}.gz")) {
                // Fall back on Prokka
                fna = "${baseDir}/${PATHS['faa']}/prokka/${sample}.fna"
                faa = "${baseDir}/${PATHS['faa']}/prokka/${sample}.faa"
                gff = "${baseDir}/${PATHS['faa']}/prokka/${sample}.gff"
            }

            if (fileExists("${fna}.gz") && fileExists("${faa}.gz") && fileExists("${gff}.gz")) {
                return [[id:sample, is_compressed:true], ["${fna}.gz"], ["${faa}.gz"], ["${gff}.gz"]]
            } else if (fileExists(fna) && fileExists(faa) && fileExists(gff)) {
                return [[id:sample, is_compressed:false], [fna], [faa], [gff]]
            }
        } else if (extension == 'fna_faa') {
            // Default to Bakta faa
            def String faa = "${baseDir}/${PATHS['faa']}/bakta/${sample}.faa"
            if (!fileExists(faa) && !fileExists("${faa}.gz")) {
                // Fall back on Prokka
                faa = "${baseDir}/${PATHS['faa']}/prokka/${sample}.faa"
            }

            if (fileExists("${fna}.gz") && fileExists("${faa}.gz")) {
                return [[id:sample, is_compressed:true], ["${fna}.gz"], ["${faa}.gz"], []]
            } else if (fileExists(fna) && fileExists(faa)) {
                return [[id:sample, is_compressed:false], [fna], [faa], []]
            }
        } else if (extension == 'fna_meta') {
            // include the meta file
            if (fileExists("${fna}.gz") && fileExists(meta)) {
                return [[id:sample, is_compressed:true], ["${fna}.gz"], [meta], []]
            } else if (fileExists(fna) && fileExists(meta)) {
                return [[id:sample, is_compressed:false], [fna], [meta], []]
            }
        } else if (extension == 'blastdb') {
            // Default to Bakta blastdb
            def String input = "${baseDir}/${PATHS[extension]}/bakta/${sample}-${extension}.tar.gz"
            if (!fileExists(input)) {
                // Fall back on Prokka
                input = "${baseDir}/${PATHS[extension]}/prokka/${sample}-${extension}.tar.gz"
            }

            if (fileExists(input)) {
                return [[id:sample], [input], [], []]
            }
        } else {
            // The remaining are generic 1 to 1 mappings
            def String input = "${baseDir}/${PATHS[extension]}/${sample}.${extension}"
            if (extension == "gbk") {
                // Default to Bakta (gbff)
                input = "${baseDir}/${PATHS[extension]}/bakta/${sample}.gbff"
                if (!fileExists(input) && !fileExists("${input}.gz")) {
                    // Fall back on Prokka (gbk)
                    input = "${baseDir}/${PATHS[extension]}/prokka/${sample}.${extension}"
                }
            } else if (extension == "gff") {
                // Default to Bakta (gff3)
                input = "${baseDir}/${PATHS[extension]}/bakta/${sample}.gff3"
                if (!fileExists(input) && !fileExists("${input}.gz")) {
                    // Fall back on Prokka (gff)
                    input = "${baseDir}/${PATHS[extension]}/prokka/${sample}.${extension}"
                }
            } else if (extension == "faa") {
                // Default to Bakta faa
                input = "${baseDir}/${PATHS[extension]}/bakta/${sample}.${extension}"
                if (!fileExists(input) && !fileExists("${input}.gz")) {
                    // Fall back on Prokka
                    input = "${baseDir}/${PATHS[extension]}/prokka/${sample}.${extension}"
                }
            } 

            if (fileExists("${input}.gz")) {
                return [[id:sample, is_compressed:true], ["${input}.gz"], [], []]
            } else if (fileExists(input)) {
                return [[id:sample, is_compressed:false], [input], [], []]
            }
        }

        // If we get here, the sample is missing the required files
        return [sample]
    }
}
