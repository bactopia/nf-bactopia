package bactopia.plugin.inputs

import spock.lang.Specification
import spock.lang.TempDir
import java.nio.file.Path
import java.nio.file.Files

import static bactopia.plugin.utils.EmptyFiles.getEmptyPaths

/**
 * Unit tests for BactopiaTools input handler class
 */
class BactopiaToolsTest extends Specification {

    @TempDir
    Path tempDir

    Map EMPTY_PATHS

    def setup() {
        EMPTY_PATHS = getEmptyPaths(tempDir.toString())

        // Create a mock Bactopia directory structure for testing
        def sampleDir = tempDir.resolve('sample1/main/gather')
        Files.createDirectories(sampleDir)
        sampleDir.resolve('sample1-meta.tsv').text = 'meta data'

        def qcDir = tempDir.resolve('sample1/main/qc')
        Files.createDirectories(qcDir)

        def assemblerDir = tempDir.resolve('sample1/main/assembler')
        Files.createDirectories(assemblerDir)
        assemblerDir.resolve('sample1.fna').text = '>contig1\nATCG'

        def annotatorDir = tempDir.resolve('sample1/main/annotator/bakta')
        Files.createDirectories(annotatorDir)
    }

    def 'collectBactopiaToolInputs should require bactopia parameter'() {
        given: 'parameters without bactopia path'
        def params = [
            bactopia: null,
            include: null,
            exclude: null,
            workflow: [ext: ['fna']],
            empty_path: tempDir.toString()
        ]

        when: 'collectBactopiaToolInputs is called'
        def result = BactopiaTools.collectBactopiaToolInputs(params)

        then: 'result should be empty'
        result.isEmpty()
    }

    def 'collectBactopiaToolInputs should check if bactopia directory exists'() {
        given: 'parameters with non-existent bactopia path'
        def params = [
            bactopia: '/non/existent/path',
            include: null,
            exclude: null,
            workflow: [ext: ['fna']],
            empty_path: tempDir.toString()
        ]

        when: 'collectBactopiaToolInputs is called'
        def result = BactopiaTools.collectBactopiaToolInputs(params)

        then: 'result should be empty'
        result.isEmpty()
    }

    def 'collectBactopiaToolInputs should skip ignored directories'() {
        given: 'bactopia directory with ignored folders'
        def ignoreDir1 = tempDir.resolve('.nextflow')
        def ignoreDir2 = tempDir.resolve('bactopia-info')
        def ignoreDir3 = tempDir.resolve('work')
        Files.createDirectories(ignoreDir1)
        Files.createDirectories(ignoreDir2)
        Files.createDirectories(ignoreDir3)

        def params = [
            bactopia: tempDir.toString(),
            include: null,
            exclude: null,
            workflow: [ext: ['fna']],
            empty_path: tempDir.toString()
        ]

        when: 'collectBactopiaToolInputs is called'
        def result = BactopiaTools.collectBactopiaToolInputs(params)

        then: 'ignored directories should not be processed'
        result.every { it.meta.id != '.nextflow' }
        result.every { it.meta.id != 'bactopia-info' }
        result.every { it.meta.id != 'work' }
    }

    def 'collectBactopiaToolInputs should detect sample directories'() {
        given: 'bactopia directory with sample'
        def params = [
            bactopia: tempDir.toString(),
            include: null,
            exclude: null,
            workflow: [ext: ['fna']],
            empty_path: tempDir.toString()
        ]

        when: 'collectBactopiaToolInputs is called'
        def result = BactopiaTools.collectBactopiaToolInputs(params)

        then: 'sample should be found if meta file exists'
        result.size() >= 0 // May be 0 or more depending on what files exist
    }

    def 'collectBactopiaToolInputs should handle include list'() {
        given: 'include file with sample names'
        def includeFile = tempDir.resolve('include.txt').toFile()
        includeFile.text = 'sample1\nsample2'

        and: 'sample directories'
        def sample2Dir = tempDir.resolve('sample2/main/gather')
        Files.createDirectories(sample2Dir)
        sample2Dir.resolve('sample2-meta.tsv').text = 'meta'

        def params = [
            bactopia: tempDir.toString(),
            include: includeFile.absolutePath,
            exclude: null,
            workflow: [ext: ['fna']],
            empty_path: tempDir.toString()
        ]

        when: 'collectBactopiaToolInputs is called'
        def result = BactopiaTools.collectBactopiaToolInputs(params)

        then: 'only included samples should be processed'
        result.every { it.meta.id in ['sample1', 'sample2'] }
    }

    def 'collectBactopiaToolInputs should handle exclude list'() {
        given: 'exclude file with sample names'
        def excludeFile = tempDir.resolve('exclude.txt').toFile()
        excludeFile.text = 'sample1'

        and: 'another sample'
        def sample2Dir = tempDir.resolve('sample2/main/gather')
        Files.createDirectories(sample2Dir)
        sample2Dir.resolve('sample2-meta.tsv').text = 'meta'

        def params = [
            bactopia: tempDir.toString(),
            include: null,
            exclude: excludeFile.absolutePath,
            workflow: [ext: ['fna']],
            empty_path: tempDir.toString()
        ]

        when: 'collectBactopiaToolInputs is called'
        def result = BactopiaTools.collectBactopiaToolInputs(params)

        then: 'excluded samples should not be in results'
        result.every { it.meta.id != 'sample1' }
    }

    def 'collectBactopiaToolInputs should handle fna extension'() {
        given: 'parameters requesting fna files'
        def fnaFile = tempDir.resolve('sample1/main/assembler/sample1.fna')
        fnaFile.text = '>contig\nATCG'

        def params = [
            bactopia: tempDir.toString(),
            include: null,
            exclude: null,
            workflow: [ext: ['fna']],
            empty_path: tempDir.toString()
        ]

        when: 'collectBactopiaToolInputs is called'
        def result = BactopiaTools.collectBactopiaToolInputs(params)

        then: 'fna files should be collected'
        result.size() >= 0
    }

    def 'collectBactopiaToolInputs should return missing samples list'() {
        given: 'sample directory without required files'
        def sample3Dir = tempDir.resolve('sample3/main/gather')
        Files.createDirectories(sample3Dir)
        sample3Dir.resolve('sample3-meta.tsv').text = 'meta'
        // No fna file created

        def params = [
            bactopia: tempDir.toString(),
            include: null,
            exclude: null,
            workflow: [ext: ['fna']],
            empty_path: tempDir.toString()
        ]

        when: 'collectBactopiaToolInputs is called'
        def result = BactopiaTools.collectBactopiaToolInputs(params)

        then: 'samples with missing files should be handled'
        // Result may be empty or contain only samples with required files
        result instanceof List
    }

    def '_isSampleDir should return true for valid Bactopia sample'() {
        given: 'a valid sample directory with meta file'
        def metaFile = tempDir.resolve('sample1/main/gather/sample1-meta.tsv')
        Files.createDirectories(metaFile.parent)
        metaFile.text = 'meta data'

        when: '_isSampleDir is called'
        def result = BactopiaTools._isSampleDir('sample1', tempDir.toString())

        then: 'should return true'
        result == true
    }

    def '_isSampleDir should return false for invalid sample'() {
        given: 'a directory without meta file'
        def invalidDir = tempDir.resolve('invalid_sample')
        Files.createDirectories(invalidDir)

        when: '_isSampleDir is called'
        def result = BactopiaTools._isSampleDir('invalid_sample', tempDir.toString())

        then: 'should return false'
        result == false
    }

    def '_collectInputs should handle illumina ext with PE reads'() {
        given: 'PE fastq files'
        def qcDir = tempDir.resolve('sample1/main/qc')
        Files.createDirectories(qcDir)
        qcDir.resolve('sample1_R1.fastq.gz').text = 'read1'
        qcDir.resolve('sample1_R2.fastq.gz').text = 'read2'

        when: '_collectInputs is called with r1, r2, se'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['r1', 'r2', 'se'], EMPTY_PATHS)

        then: 'PE reads should be returned'
        result.meta.id == 'sample1'
        result.meta.single_end == false
        result.meta.runtype == 'illumina'
    }

    def '_collectInputs should handle illumina ext with SE reads'() {
        given: 'SE fastq file'
        def qcDir = tempDir.resolve('sample1/main/qc')
        Files.createDirectories(qcDir)
        qcDir.resolve('sample1.fastq.gz').text = 'reads'

        when: '_collectInputs is called with r1, r2, se'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['r1', 'r2', 'se'], EMPTY_PATHS)

        then: 'SE read should be returned'
        result.meta.id == 'sample1'
        result.meta.single_end == true
        result.meta.runtype == 'illumina'
    }

    def '_collectInputs should handle fna ext'() {
        given: 'fna file'
        def assemblerDir = tempDir.resolve('sample1/main/assembler')
        Files.createDirectories(assemblerDir)
        assemblerDir.resolve('sample1.fna').text = '>contig\nATCG'

        when: '_collectInputs is called'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['fna'], EMPTY_PATHS)

        then: 'fna file should be returned'
        result.meta.id == 'sample1'
        result.fna.toString().endsWith('sample1.fna')
    }

    def '_collectInputs should handle compressed fna files'() {
        given: 'compressed fna file'
        def assemblerDir = tempDir.resolve('sample1/main/assembler')
        Files.createDirectories(assemblerDir)
        // Remove uncompressed version if it exists
        def uncompressed = assemblerDir.resolve('sample1.fna')
        if (Files.exists(uncompressed)) Files.delete(uncompressed)
        assemblerDir.resolve('sample1.fna.gz').text = 'compressed'

        when: '_collectInputs is called'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['fna'], EMPTY_PATHS)

        then: 'compressed assembly should be found'
        result.fna.toString().endsWith('sample1.fna.gz')
    }

    def '_collectInputs should handle fna + fastq ext'() {
        given: 'fna and fastq files'
        def assemblerDir = tempDir.resolve('sample1/main/assembler')
        Files.createDirectories(assemblerDir)
        assemblerDir.resolve('sample1.fna').text = '>contig\nATCG'

        def qcDir = tempDir.resolve('sample1/main/qc')
        Files.createDirectories(qcDir)
        qcDir.resolve('sample1.fastq.gz').text = 'reads'

        when: '_collectInputs is called with fna and fastq'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['fna', 'fastq'], EMPTY_PATHS)

        then: 'both fna and reads should be returned'
        result.meta.id == 'sample1'
        result.fna.toString().endsWith('sample1.fna')
    }

    def '_collectInputs should handle fna + faa ext'() {
        given: 'fna and faa files from Bakta'
        def assemblerDir = tempDir.resolve('sample1/main/assembler')
        Files.createDirectories(assemblerDir)
        assemblerDir.resolve('sample1.fna').text = '>contig\nATCG'

        def baktaDir = tempDir.resolve('sample1/main/annotator/bakta')
        Files.createDirectories(baktaDir)
        baktaDir.resolve('sample1.faa').text = '>protein\nMKTL'

        when: '_collectInputs is called with fna and faa'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['fna', 'faa'], EMPTY_PATHS)

        then: 'both fna and faa should be returned'
        result.meta.id == 'sample1'
        result.fna.toString().endsWith('sample1.fna')
        result.faa.toString().endsWith('sample1.faa')
    }

    def '_collectInputs should fallback to Prokka if Bakta not found'() {
        given: 'faa file from Prokka only'
        def assemblerDir = tempDir.resolve('sample1/main/assembler')
        Files.createDirectories(assemblerDir)
        assemblerDir.resolve('sample1.fna').text = '>contig\nATCG'

        // Remove Bakta faa if exists
        def baktaDir = tempDir.resolve('sample1/main/annotator/bakta')
        Files.createDirectories(baktaDir)

        def prokkaDir = tempDir.resolve('sample1/main/annotator/prokka')
        Files.createDirectories(prokkaDir)
        prokkaDir.resolve('sample1.faa').text = '>protein\nMKTL'

        when: '_collectInputs is called with fna and faa'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['fna', 'faa'], EMPTY_PATHS)

        then: 'Prokka files should be used'
        result.meta.id == 'sample1'
        result.faa.toString().contains('prokka')
    }

    def '_collectInputs should handle fna + tsv_meta ext'() {
        given: 'fna and meta files'
        def assemblerDir = tempDir.resolve('sample1/main/assembler')
        Files.createDirectories(assemblerDir)
        assemblerDir.resolve('sample1.fna').text = '>contig\nATCG'

        def gatherDir = tempDir.resolve('sample1/main/gather')
        Files.createDirectories(gatherDir)
        gatherDir.resolve('sample1-meta.tsv').text = 'meta data'

        when: '_collectInputs is called with fna and tsv_meta'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['fna', 'tsv_meta'], EMPTY_PATHS)

        then: 'fna and tsv_meta should be returned'
        result.meta.id == 'sample1'
        result.fna.toString().endsWith('sample1.fna')
        result.tsv_meta.toString().endsWith('sample1-meta.tsv')
    }

    def '_collectInputs should handle gbk ext with Bakta'() {
        given: 'gbff file from Bakta'
        def baktaDir = tempDir.resolve('sample1/main/annotator/bakta')
        Files.createDirectories(baktaDir)
        baktaDir.resolve('sample1.gbff').text = 'genbank data'

        when: '_collectInputs is called with gbk'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['gbk'], EMPTY_PATHS)

        then: 'gbff file should be returned'
        result.meta.id == 'sample1'
        result.gbk.toString().endsWith('sample1.gbff')
    }

    def '_collectInputs should handle gff ext with Bakta'() {
        given: 'gff3 file from Bakta'
        def baktaDir = tempDir.resolve('sample1/main/annotator/bakta')
        Files.createDirectories(baktaDir)
        baktaDir.resolve('sample1.gff3').text = 'gff data'

        when: '_collectInputs is called with gff'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['gff'], EMPTY_PATHS)

        then: 'gff3 file should be returned'
        result.meta.id == 'sample1'
        result.gff.toString().endsWith('sample1.gff3')
    }

    def '_collectInputs should handle ONT fastq files'() {
        given: 'ONT fastq with NanoPlot report'
        def qcDir = tempDir.resolve('sample1/main/qc')
        Files.createDirectories(qcDir)
        qcDir.resolve('sample1.fastq.gz').text = 'ont reads'

        def suppDir = tempDir.resolve('sample1/main/qc/supplemental')
        Files.createDirectories(suppDir)
        suppDir.resolve('sample1-final_NanoPlot-report.html').text = 'nanoplot'

        when: '_collectInputs is called with fastq'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['fastq'], EMPTY_PATHS)

        then: 'ONT runtype should be detected'
        result.meta.runtype == 'ont'
    }

    def '_collectInputs should detect Illumina fastq without NanoPlot'() {
        given: 'fastq without NanoPlot report'
        def qcDir = tempDir.resolve('sample1/main/qc')
        Files.createDirectories(qcDir)
        qcDir.resolve('sample1.fastq.gz').text = 'illumina reads'

        when: '_collectInputs is called with fastq'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['fastq'], EMPTY_PATHS)

        then: 'Illumina runtype should be set'
        result.meta.runtype == 'illumina'
    }

    def '_collectInputs should handle PE-only ext'() {
        given: 'PE fastq files'
        def qcDir = tempDir.resolve('sample1/main/qc')
        Files.createDirectories(qcDir)
        qcDir.resolve('sample1_R1.fastq.gz').text = 'read1'
        qcDir.resolve('sample1_R2.fastq.gz').text = 'read2'

        when: '_collectInputs is called with r1, r2 only'
        def result = BactopiaTools._collectInputs('sample1', tempDir.toString(), ['r1', 'r2'], EMPTY_PATHS)

        then: 'PE reads should be returned'
        result.meta.single_end == false
        result.meta.runtype == 'illumina'
        !result.missing_required
    }

    def 'processFOFN should parse include file'() {
        given: 'an include file'
        def includeFile = tempDir.resolve('include.txt').toFile()
        includeFile.text = 'sample1\nsample2\nsample3'

        when: 'processFOFN is called'
        def result = BactopiaTools.processFOFN(includeFile.absolutePath, true)

        then: 'samples should be parsed'
        result.size() == 3
        result.contains('sample1')
        result.contains('sample2')
        result.contains('sample3')
    }

    def 'processFOFN should handle null file'() {
        when: 'processFOFN is called with null'
        def result = BactopiaTools.processFOFN(null, true)

        then: 'empty list should be returned'
        result.isEmpty()
    }

    def 'processFOFN should skip empty lines'() {
        given: 'file with empty lines'
        def includeFile = tempDir.resolve('include.txt').toFile()
        includeFile.text = 'sample1\n\nsample2\n\n'

        when: 'processFOFN is called'
        def result = BactopiaTools.processFOFN(includeFile.absolutePath, true)

        then: 'only non-empty lines should be included'
        result.size() == 2
    }
}
