package bactopia.plugin.inputs

import groovy.util.logging.Slf4j
import nextflow.util.RecordMap

@Slf4j
class Bactopia {
    /**
     * Create input channel data structure based on runtype.
     *
     * @param params The workflow parameters
     * @param runtype The type of run (paired-end, hybrid, assembly, etc.)
     * @return List of Map of sample data structures
     */
    public static List<Map> collectBactopiaInputs(Map params, String runtype) {
        if (runtype == "is_fofn") {
            return processFOFN(params)
        } else if (runtype == "is_accessions") {
            return processAccessions(params)
        } else if (runtype == "is_accession") {
            return processAccession(params)
        } else {
            def Map meta = [:]
            meta.id = params.sample
            meta.name = params.sample
            meta.runtype = runtype
            meta.genome_size = params.genome_size
            meta.species = params.species
            
            if (runtype == "paired-end") {
                meta.single_end = false
                return [[
                    'meta': new RecordMap(meta),
                    'r1': [params.r1],
                    'r2': [params.r2],
                    'se': [],
                    'lr': [],
                    'assembly': []
                ]]
            } else if (runtype == "single-end") {
                meta.single_end = true
                return [[
                    'meta': new RecordMap(meta),
                    'r1': [],
                    'r2': [],
                    'se': [params.se],
                    'lr': [],
                    'assembly': []
                ]]
            } else if (runtype == "hybrid" || runtype == "short_polish") {
                meta.single_end = runtype == "short_polish" ? true : false
                return [[
                    'meta': new RecordMap(meta),
                    'r1': [params.r1],
                    'r2': [params.r2],
                    'se': [],
                    'lr': [params.ont],
                    'assembly': []
                ]]
            } else if (runtype == "assembly") {
                return [[
                    'meta': new RecordMap(meta),
                    'r1': [],
                    'r2': [],
                    'se': [],
                    'lr': [],
                    'assembly': [params.assembly]
                ]]
            } else if (runtype == "ont") {
                meta.single_end = true
                return [[
                    'meta': new RecordMap(meta),
                    'r1': [],
                    'r2': [],
                    'se': [],
                    'lr': [params.ont],
                    'assembly': []
                ]]
            } else {
                log.error("Invalid runtype '${runtype}' provided, please correct to continue. Expected: paired-end, single-end, hybrid, short_polish, assembly, or ont")
            }
        }
    }

    /**
     * Handle multiple FASTQ files for merging.
     *
     * @param readSet The read set string containing file paths
     * @return List of FASTQ file paths
     */
    public static List<String> handleMultipleFqs(String readSet) {
        def List<String> fqs = []
        def String[] reads = readSet.split(",")
        reads.each { fq ->
            fqs << fq
        }
        return fqs
    }

    /**
     * Process FOFN file and determine input type for each row.
     *
     * @param params The workflow parameters
     * @return List of processed sample data structures
     */
    public static List<Map> processFOFN(Map params) {
        def results = []
        def headers = null
        def isFirstLine = true
        def hasValidHeaders = true

        // Read the samples file (TSV format)
        new File(params.samples).splitEachLine('\t') { columns ->
            // Strip whitespace from each column
            columns = columns.collect { it.trim() }

            if (isFirstLine) {
                headers = columns
                isFirstLine = false
                def requiredHeaders = ['sample', 'runtype', 'genome_size', 'species', 'r1', 'r2', 'se', 'ont', 'assembly']
                def missingHeaders = requiredHeaders.findAll { !headers.contains(it) }
                if (missingHeaders) {
                    log.error(
                        "Missing required column(s) ${missingHeaders.collect { "'${it}'" }.join(', ')} " +
                        "in ${params.samples}. Found columns: ${headers.join(', ')}.\n\n" +
                        "Please use 'bactopia prepare' to generate a properly formatted FOFN file."
                    )
                    hasValidHeaders = false
                }
            } else if (hasValidHeaders && columns.size() > 0 && columns[0]) { // Skip empty lines
                // Create map from headers and values
                def line = [:]
                headers.eachWithIndex { header, index ->
                    if (index < columns.size()) {
                        line[header] = columns[index]
                    }
                }
                
                // Process and collect the result
                results << _processFOFNLine(line, params)
            }
        }
        
        return results
    }

    /**
     * Process a single FOFN line and determine input type.
     *
     * @param line The FOFN line data
     * @param params The workflow parameters
     * @return Map containing the processed sample data
     */
    private static Map _processFOFNLine(Map line, Map params) {
        /* Parse line and determine if single end or paired reads*/
        def Map meta = [:]
        meta.id = line.sample
        meta.name = line.sample
        meta.runtype = line.runtype

        if (params.genome_size) {
            // user provided via --genome_size, use it
            meta.genome_size = params.genome_size
        } else {
            // use size available in FOFN
            meta.genome_size = line.genome_size
        }

        if (params.species) {
            // user provided via --species, use it
            meta.species = params.species
        } else {
            // use species available in FOFN
            meta.species = line.species
        }
        
        if (line.sample) {
            // List contents = [meta, [r1], [r2], [se], [ont], [assembly]]
            if (line.runtype == 'ont') {
                meta.single_end = true
                return [
                    'meta': new RecordMap(meta),
                    'r1': [],
                    'r2': [],
                    'se': [],
                    'lr': [line.ont],
                    'assembly': []
                ]
            } else if (line.runtype == 'single-end') {
                meta.single_end = true
                return [
                    'meta': new RecordMap(meta),
                    'r1': [],
                    'r2': [],
                    'se': [line.se],
                    'lr': [],
                    'assembly': []
                ]
            } else if (line.runtype == 'paired-end') {
                meta.single_end = false
                return [
                    'meta': new RecordMap(meta),
                    'r1': [line.r1],
                    'r2': [line.r2],
                    'se': [],
                    'lr': [],
                    'assembly': []
                ]
            } else if (line.runtype == 'hybrid' || line.runtype == 'short_polish') {
                // short polish = ONT primary reads, so single-end
                // hybrid = Illumina primary reads, so paired-end
                meta.single_end = line.runtype == 'short_polish' ? true : false
                return [
                    'meta': new RecordMap(meta),
                    'r1': [line.r1],
                    'r2': [line.r2],
                    'se': [],
                    'lr': [line.ont],
                    'assembly': []
                ]
            } else if (line.runtype == 'assembly') {
                return [
                    'meta': new RecordMap(meta),
                    'r1': [],
                    'r2': [],
                    'se': [],
                    'lr': [],
                    'assembly': [line.assembly]
                ]
            } else if (line.runtype == 'merge-pe') {
                meta.single_end = false
                return [
                    'meta': new RecordMap(meta),
                    'r1': handleMultipleFqs(line.r1),
                    'r2': handleMultipleFqs(line.r2),
                    'se': [],
                    'lr': [],
                    'assembly': []
                ]
            } else if (line.runtype == 'hybrid-merge-pe' || line.runtype == 'short_polish-merge-pe') {
                // short polish = ONT primary reads, so single-end
                // hybrid = Illumina primary reads, so paired-end
                meta.single_end = line.runtype == 'short_polish-merge-pe' ? true : false
                return [
                    'meta': new RecordMap(meta),
                    'r1': handleMultipleFqs(line.r1),
                    'r2': handleMultipleFqs(line.r2),
                    'se': [],
                    'lr': [line.ont],
                    'assembly': []
                ]
            } else if (line.runtype == 'merge-se') {
                meta.single_end = true
                return [
                    'meta': new RecordMap(meta),
                    'r1': [],
                    'r2': [],
                    'se': handleMultipleFqs(line.se),
                    'lr': [],
                    'assembly': []
                ]
            } else {
                log.error(
                    "Invalid runtype ${line.runtype} found, please correct to continue. " +
                    "Expected: single-end, paired-end, hybrid, short_polish, merge-pe, hybrid-merge-pe, short_polish-merge-pe, merge-se, or assembly"
                )
            }
        } else {
            log.error("Sample name cannot be null: ${line}")
        }
    }

    /**
     * Process accessions from TSV file.
     *
     * @param params The workflow parameters
     * @return List of Maps of processed accession data structures
     */
    public static List<Map> processAccessions(Map params) {
        def results = []
        def headers = null
        def isFirstLine = true
        def hasValidHeaders = true

        // Read the accessions file (TSV format)
        new File(params.accessions).splitEachLine('\t') { columns ->
            // Strip whitespace from each column
            columns = columns.collect { it.trim() }

            if (isFirstLine) {
                headers = columns
                isFirstLine = false
                def requiredHeaders = ['accession', 'runtype', 'species', 'genome_size']
                def missingHeaders = requiredHeaders.findAll { !headers.contains(it) }
                if (missingHeaders) {
                    log.error(
                        "Missing required column(s) ${missingHeaders.collect { "'${it}'" }.join(', ')} " +
                        "in ${params.accessions}. Found columns: ${headers.join(', ')}.\n\n" +
                        "Please use 'bactopia search' to generate a properly formatted accessions file."
                    )
                    hasValidHeaders = false
                }
            } else if (hasValidHeaders && columns.size() > 0 && columns[0]) { // Skip empty lines
                // Create map from headers and values
                def row = [:]
                headers.eachWithIndex { header, index ->
                    if (index < columns.size()) {
                        row[header] = columns[index]
                    }
                }
                
                // Process and collect the result
                results << _processAccessionsLine(row, params)
            }
        }
        
        return results
    }

    /**
     * Process a single accession line from FOFN.
     *
     * @param line The accession line data
     * @param params The workflow parameters
     * @return List containing the processed accession data
     */
    private static Map _processAccessionsLine(Map line, Map params) {
        /* Parse line and determine if single end or paired reads*/
        def Map meta = [:]

        if (line.accession.startsWith('GCF') || line.accession.startsWith('GCA')) {
            meta.id = line.accession.split(/\./)[0]
            meta.name = line.accession.split(/\./)[0]
            meta.runtype = "assembly_accession"
            meta.genome_size = params.genome_size
            meta.species = params.species
            return [
                'meta': new RecordMap(meta),
                'r1': [],
                'r2': [],
                'se': [],
                'lr': [],
                'assembly': []
            ]
        } else if (line.accession.startsWith('DRX') || line.accession.startsWith('ERX') || line.accession.startsWith('SRX')) {
            meta.id = line.accession
            meta.name = line.accession
            meta.runtype = line.runtype == 'ont' ? "sra_accession_ont" : "sra_accession"

            // if genome_size is provided, use it, otherwise use the genome_size from the FOFN
            meta.genome_size = params.genome_size ? params.genome_size : line.genome_size

            // if species is provided, use it, otherwise use the species from the FOFN
            meta.species = params.species ? params.species : line.species
            return [
                'meta': new RecordMap(meta),
                'r1': [],
                'r2': [],
                'se': [],
                'lr': [],
                'assembly': []
            ]
        } else {
            log.error(
                "Invalid accession: ${line.accession} is not an accepted accession type. Accessions must " +
                "be Assembly (GCF_*, GCA*) or Experiment (DRX*, ERX*, SRX*) accessions. Please correct to " +
                "continue.\n\nYou can use 'bactopia search' to convert BioProject, BioSample, or Run " +
                "accessions into an Experiment accession."
            )
        }
    }

    /**
     * Process single accession.
     *
     * @param params The workflow parameters
     * @return List containing the processed accession data
     */
    public static List<Map> processAccession(Map params) {
        String accession = params.accession
        def Map meta = [:]
        meta.genome_size = params.genome_size
        meta.species = params.species

        if (accession.length() > 0) {
            if (accession.startsWith('GCF') || accession.startsWith('GCA')) {
                meta.id = accession.split(/\./)[0]
                meta.name = accession.split(/\./)[0]
                meta.runtype = "assembly_accession"
            } else if (accession.startsWith('DRX') || accession.startsWith('ERX') || accession.startsWith('SRX')) {
                meta.id = accession
                meta.name = accession
                meta.runtype = params.ont ? "sra_accession_ont" : "sra_accession"
            } else {
                log.error(
                    "Invalid accession: ${accession} is not an accepted accession type. Accessions must be " +
                    "Assembly (GCF_*, GCA*) or Experiment (DRX*, ERX*, SRX*) accessions. Please correct to " +
                    "continue.\n\nYou can use 'bactopia search' to convert BioProject, BioSample, or Run " +
                    "accessions into an Experiment accession."
                )
            }
            return [[
                'meta': new RecordMap(meta),
                'r1': [],
                'r2': [],
                'se': [],
                'lr': [],
                'assembly': []
            ]]
        }
        log.error("Accession cannot be empty, please provide a valid accession to continue.")
    }
}
