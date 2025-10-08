package bactopia.plugin.inputs

import groovy.util.logging.Slf4j
import java.nio.file.Path
import java.nio.file.Files

import static bactopia.plugin.BactopiaUtils.fileExists
import static bactopia.plugin.BactopiaTemplate.dashedLine

@Slf4j
class Bactopia {
    //
    // Create input channel data structure based on runtype
    //
    public static List collectBactopiaInputs(Map params, String runtype) {
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
                return [[meta, [params.r1], [params.r2], params.empty_extra]]
            } else if (runtype == "hybrid" || runtype == "short_polish") {
                return [[meta, [params.r1], [params.r2], params.ont]]
            } else if (runtype == "assembly") {
                return [[meta, [params.empty_r1], [params.empty_r2], params.assembly]]
            } else if (runtype == "ont") {
                return [[meta, [params.ont], [params.empty_r2], params.empty_extra]]
            } else {
                return [[meta, [params.se], [params.empty_r2], params.empty_extra]]
            }
        }


    }

    //
    // Handle multiple FASTQ files for merging
    //
    public static List<String> handleMultipleFqs(String readSet) {
        def List<String> fqs = []
        def String[] reads = readSet.split(",")
        reads.each { fq ->
            fqs << fq
        }
        return fqs
    }


    //
    // Process FOFN file and determine input type for each row
    //
    public static List processFOFN(Map params) {
        def results = []
        def headers = null
        def isFirstLine = true
        
        // Read the samples file (TSV format)
        new File(params.samples).splitEachLine('\t') { columns ->
            // Strip whitespace from each column
            columns = columns.collect { it.trim() }
            
            if (isFirstLine) {
                headers = columns
                isFirstLine = false
            } else if (columns.size() > 0 && columns[0]) { // Skip empty lines
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


    //
    // Process a single FOFN line and determine input type
    //
    private static List _processFOFNLine(Map line, Map params) {
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
            if (line.runtype == 'single-end' || line.runtype == 'ont') {
                return [meta, [line.r1], [params.empty_r2], params.empty_extra]
            } else if (line.runtype == 'paired-end') {
                return [meta, [line.r1], [line.r2], params.empty_extra]
            } else if (line.runtype == 'hybrid' || line.runtype == 'short_polish') {
                return [meta, [line.r1], [line.r2], line.extra]
            } else if (line.runtype == 'assembly') {
                return [meta, [params.empty_r1], [params.empty_r2], line.extra]
            } else if (line.runtype == 'merge-pe') {
                return [meta, handleMultipleFqs(line.r1), handleMultipleFqs(line.r2), params.empty_extra]
            } else if (line.runtype == 'hybrid-merge-pe' || line.runtype == 'short_polish-merge-pe') {
                return [meta, handleMultipleFqs(line.r1), handleMultipleFqs(line.r2), line.extra]
            } else if (line.runtype == 'merge-se') {
                return [meta, handleMultipleFqs(line.r1), [params.empty_r2], params.empty_extra]
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


    //
    // Process accessions from CSV file
    //
    public static List processAccessions(Map params) {
        def results = []
        def headers = null
        def isFirstLine = true
        
        // Read the accessions file (TSV format)
        new File(params.accessions).splitEachLine('\t') { columns ->
            // Strip whitespace from each column
            columns = columns.collect { it.trim() }
            
            if (isFirstLine) {
                headers = columns
                isFirstLine = false
            } else if (columns.size() > 0 && columns[0]) { // Skip empty lines
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


    //
    // Process accessions from FOFN
    //
    public static List _processAccessionsLine(Map line, Map params) {
        /* Parse line and determine if single end or paired reads*/
        def Map meta = [:]

        if (line.accession.startsWith('GCF') || line.accession.startsWith('GCA')) {
            meta.id = line.accession.split(/\./)[0]
            meta.name = line.accession.split(/\./)[0]
            meta.runtype = "assembly_accession"
            meta.genome_size = params.genome_size
            meta.species = params.species
            return [meta, [params.empty_r1], [params.empty_r2], params.empty_extra]
        } else if (line.accession.startsWith('DRX') || line.accession.startsWith('ERX') || line.accession.startsWith('SRX')) {
            meta.id = line.accession
            meta.name = line.accession
            meta.runtype = line.runtype == 'ont' ? "sra_accession_ont" : "sra_accession"

            // if genome_size is provided, use it, otherwise use the genome_size from the FOFN
            meta.genome_size = params.genome_size ? params.genome_size : line.genome_size

            // if species is provided, use it, otherwise use the species from the FOFN
            meta.species = params.species ? params.species : line.species
            return [meta, [params.empty_r1], [params.empty_r2], params.empty_extra]
        } else {
            log.error(
                "Invalid accession: ${line.accession} is not an accepted accession type. Accessions must " +
                "be Assembly (GCF_*, GCA*) or Experiment (DRX*, ERX*, SRX*) accessions. Please correct to " +
                "continue.\n\nYou can use 'bactopia search' to convert BioProject, BioSample, or Run " +
                "accessions into an Experiment accession."
            )
        }
    }


    //
    // Process single accession
    //
    public static List processAccession(Map params) {
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
            return [[meta, [params.empty_r1], [params.empty_r2], params.empty_extra]]
        }
        log.error("Accession cannot be empty, please provide a valid accession to continue.")
    }
}
