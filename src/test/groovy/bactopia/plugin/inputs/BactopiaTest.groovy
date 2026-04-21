package bactopia.plugin.inputs

import spock.lang.Specification
import spock.lang.TempDir
import java.nio.file.Path

/**
 * Unit tests for Bactopia input handler class
 */
class BactopiaTest extends Specification {

    @TempDir
    Path tempDir

    def 'collectBactopiaInputs should handle paired-end runtype'() {
        given: 'parameters for paired-end reads'
        def params = [
            sample: 'test_sample',
            r1: '/path/to/read1.fastq.gz',
            r2: '/path/to/read2.fastq.gz',
            genome_size: 5000000,
            species: 'Test species',
        ]

        when: 'collectBactopiaInputs is called'
        def result = Bactopia.collectBactopiaInputs(params, 'paired-end')

        then: 'result should contain properly formatted paired-end data'
        result.size() == 1
        result[0].meta.id == 'test_sample'
        result[0].meta.name == 'test_sample'
        result[0].meta.runtype == 'paired-end'
        result[0].meta.genome_size == 5000000
        result[0].meta.species == 'Test species'
        result[0].r1 == ['/path/to/read1.fastq.gz']
        result[0].r2 == ['/path/to/read2.fastq.gz']
        result[0].se == []
    }

    def 'collectBactopiaInputs should handle single-end runtype'() {
        given: 'parameters for single-end reads'
        def params = [
            sample: 'test_sample',
            se: '/path/to/read.fastq.gz',
            genome_size: 5000000,
            species: 'Test species',
        ]

        when: 'collectBactopiaInputs is called'
        def result = Bactopia.collectBactopiaInputs(params, 'single-end')

        then: 'result should contain properly formatted single-end data'
        result.size() == 1
        result[0].meta.runtype == 'single-end'
        result[0].se == ['/path/to/read.fastq.gz']
        result[0].r1 == []
    }

    def 'collectBactopiaInputs should handle hybrid runtype'() {
        given: 'parameters for hybrid reads'
        def params = [
            sample: 'test_sample',
            r1: '/path/to/read1.fastq.gz',
            r2: '/path/to/read2.fastq.gz',
            ont: '/path/to/ont.fastq.gz',
            genome_size: 5000000,
            species: 'Test species',
        ]

        when: 'collectBactopiaInputs is called'
        def result = Bactopia.collectBactopiaInputs(params, 'hybrid')

        then: 'result should contain properly formatted hybrid data'
        result.size() == 1
        result[0].meta.runtype == 'hybrid'
        result[0].r1 == ['/path/to/read1.fastq.gz']
        result[0].r2 == ['/path/to/read2.fastq.gz']
        result[0].lr == ['/path/to/ont.fastq.gz']
    }

    def 'collectBactopiaInputs should handle short_polish runtype'() {
        given: 'parameters for short_polish'
        def params = [
            sample: 'test_sample',
            r1: '/path/to/read1.fastq.gz',
            r2: '/path/to/read2.fastq.gz',
            ont: '/path/to/ont.fastq.gz',
            genome_size: 5000000,
            species: 'Test species',
        ]

        when: 'collectBactopiaInputs is called'
        def result = Bactopia.collectBactopiaInputs(params, 'short_polish')

        then: 'result should contain properly formatted short_polish data'
        result.size() == 1
        result[0].meta.runtype == 'short_polish'
        result[0].lr == ['/path/to/ont.fastq.gz']
    }

    def 'collectBactopiaInputs should handle assembly runtype'() {
        given: 'parameters for assembly'
        def params = [
            sample: 'test_sample',
            assembly: '/path/to/assembly.fasta',
            genome_size: 5000000,
            species: 'Test species',
        ]

        when: 'collectBactopiaInputs is called'
        def result = Bactopia.collectBactopiaInputs(params, 'assembly')

        then: 'result should contain properly formatted assembly data'
        result.size() == 1
        result[0].meta.runtype == 'assembly'
        result[0].r1 == []
        result[0].r2 == []
        result[0].assembly == ['/path/to/assembly.fasta']
    }

    def 'collectBactopiaInputs should handle ont runtype'() {
        given: 'parameters for ONT reads'
        def params = [
            sample: 'test_sample',
            ont: '/path/to/ont.fastq.gz',
            genome_size: 5000000,
            species: 'Test species',
        ]

        when: 'collectBactopiaInputs is called'
        def result = Bactopia.collectBactopiaInputs(params, 'ont')

        then: 'result should contain properly formatted ONT data'
        result.size() == 1
        result[0].meta.runtype == 'ont'
        result[0].lr == ['/path/to/ont.fastq.gz']
        result[0].r1 == []
    }

    def 'handleMultipleFqs should split comma-separated file paths'() {
        given: 'a comma-separated string of file paths'
        def readSet = '/path/to/file1.fq.gz,/path/to/file2.fq.gz,/path/to/file3.fq.gz'

        when: 'handleMultipleFqs is called'
        def result = Bactopia.handleMultipleFqs(readSet)

        then: 'result should be a list of file paths'
        result.size() == 3
        result[0] == '/path/to/file1.fq.gz'
        result[1] == '/path/to/file2.fq.gz'
        result[2] == '/path/to/file3.fq.gz'
    }

    def 'handleMultipleFqs should handle single file path'() {
        given: 'a single file path'
        def readSet = '/path/to/file.fq.gz'

        when: 'handleMultipleFqs is called'
        def result = Bactopia.handleMultipleFqs(readSet)

        then: 'result should contain one file path'
        result.size() == 1
        result[0] == '/path/to/file.fq.gz'
    }

    def 'processFOFN should parse TSV file'() {
        given: 'a FOFN file with sample data'
        def fofnFile = tempDir.resolve('samples.tsv').toFile()
        fofnFile.text = '''sample\truntype\tr1\tr2\tse\tont\tassembly\tgenome_size\tspecies
sample1\tpaired-end\t/path/to/r1.fq\t/path/to/r2.fq\t\t\t\t5000000\tE. coli
sample2\tsingle-end\t\t\t/path/to/se.fq\t\t\t4500000\tS. aureus'''

        def params = [
            samples: fofnFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processFOFN is called'
        def result = Bactopia.processFOFN(params)

        then: 'result should contain parsed samples'
        result.size() == 2
        result[0].meta.id == 'sample1'
        result[0].meta.runtype == 'paired-end'
        result[1].meta.id == 'sample2'
        result[1].meta.runtype == 'single-end'
    }

    def 'processFOFN should skip empty lines'() {
        given: 'a FOFN file with empty lines'
        def fofnFile = tempDir.resolve('samples.tsv').toFile()
        fofnFile.text = '''sample\truntype\tr1\tr2\tse\tont\tassembly\tgenome_size\tspecies
sample1\tpaired-end\t/path/to/r1.fq\t\t\t\t\t\t

sample2\tsingle-end\t\t\t/path/to/se.fq\t\t\t\t'''

        def params = [
            samples: fofnFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processFOFN is called'
        def result = Bactopia.processFOFN(params)

        then: 'empty lines should be skipped'
        result.size() == 2
    }

    def 'processFOFN should use params genome_size if provided'() {
        given: 'a FOFN file and params with genome_size'
        def fofnFile = tempDir.resolve('samples.tsv').toFile()
        fofnFile.text = '''sample\truntype\tr1\tr2\tse\tont\tassembly\tgenome_size\tspecies
sample1\tpaired-end\t/path/to/r1.fq\t\t\t\t\t3000000\t'''

        def params = [
            samples: fofnFile.absolutePath,
            genome_size: 5000000,
            species: null
        ]

        when: 'processFOFN is called'
        def result = Bactopia.processFOFN(params)

        then: 'params genome_size should override FOFN genome_size'
        result[0].meta.genome_size == 5000000
    }

    def 'processFOFN should handle merge-pe runtype'() {
        given: 'a FOFN file with merge-pe runtype'
        def fofnFile = tempDir.resolve('samples.tsv').toFile()
        fofnFile.text = '''sample\truntype\tr1\tr2\tse\tont\tassembly\tgenome_size\tspecies
sample1\tmerge-pe\t/path/r1_1.fq,/path/r1_2.fq\t/path/r2_1.fq,/path/r2_2.fq\t\t\t\t\t'''

        def params = [
            samples: fofnFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processFOFN is called'
        def result = Bactopia.processFOFN(params)

        then: 'multiple files should be handled correctly'
        result[0].r1.size() == 2
        result[0].r2.size() == 2
    }

    def 'processAccessions should parse accessions TSV'() {
        given: 'an accessions file'
        def accessionsFile = tempDir.resolve('accessions.tsv').toFile()
        accessionsFile.text = '''accession\truntype\tgenome_size\tspecies
GCF_000001405.1\t\t\t
SRX12345678\tillumina\t5000000\tE. coli'''

        def params = [
            accessions: accessionsFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processAccessions is called'
        def result = Bactopia.processAccessions(params)

        then: 'accessions should be parsed correctly'
        result.size() == 2
        result[0].meta.id == 'GCF_000001405'
        result[0].meta.runtype == 'assembly_accession'
        result[1].meta.id == 'SRX12345678'
        result[1].meta.runtype == 'sra_accession'
    }

    def 'processFOFN should return empty list when sample header is missing'() {
        given: 'a FOFN file without a sample column'
        def fofnFile = tempDir.resolve('bad_samples.tsv').toFile()
        fofnFile.text = '''name\truntype\tr1
sample1\tpaired-end\t/path/to/r1.fq'''

        def params = [
            samples: fofnFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processFOFN is called'
        def result = Bactopia.processFOFN(params)

        then: 'result should be empty'
        result.size() == 0
    }

    def 'processFOFN should return empty list when only sample column present'() {
        given: 'a FOFN file with only a sample column'
        def fofnFile = tempDir.resolve('single_col.tsv').toFile()
        fofnFile.text = '''sample
sample1'''

        def params = [
            samples: fofnFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processFOFN is called'
        def result = Bactopia.processFOFN(params)

        then: 'result should be empty because runtype is missing'
        result.size() == 0
    }

    def 'processFOFN should return empty list when headers are completely wrong'() {
        given: 'a FOFN file with unrecognized headers'
        def fofnFile = tempDir.resolve('wrong_headers.tsv').toFile()
        fofnFile.text = '''col1\tcol2\tcol3
val1\tval2\tval3'''

        def params = [
            samples: fofnFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processFOFN is called'
        def result = Bactopia.processFOFN(params)

        then: 'result should be empty'
        result.size() == 0
    }

    def 'processFOFN should return empty list for header-only file'() {
        given: 'a FOFN file with only headers'
        def fofnFile = tempDir.resolve('header_only.tsv').toFile()
        fofnFile.text = 'sample\truntype\tr1\tr2\tse\tont\tassembly\tgenome_size\tspecies'

        def params = [
            samples: fofnFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processFOFN is called'
        def result = Bactopia.processFOFN(params)

        then: 'result should be empty but no error'
        result.size() == 0
    }

    def 'processFOFN should return empty list when sample header has wrong case'() {
        given: 'a FOFN file with Sample instead of sample'
        def fofnFile = tempDir.resolve('case_samples.tsv').toFile()
        fofnFile.text = '''Sample\truntype\tr1
sample1\tpaired-end\t/path/to/r1.fq'''

        def params = [
            samples: fofnFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processFOFN is called'
        def result = Bactopia.processFOFN(params)

        then: 'result should be empty due to case mismatch'
        result.size() == 0
    }

    def 'processAccessions should return empty list when accession header is missing'() {
        given: 'an accessions file without an accession column'
        def accessionsFile = tempDir.resolve('bad_accessions.tsv').toFile()
        accessionsFile.text = '''id\truntype
GCF_000001405.1\t'''

        def params = [
            accessions: accessionsFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processAccessions is called'
        def result = Bactopia.processAccessions(params)

        then: 'result should be empty'
        result.size() == 0
    }

    def 'processAccessions should return empty list when only accession column present'() {
        given: 'an accessions file with only an accession column'
        def accessionsFile = tempDir.resolve('single_col_accessions.tsv').toFile()
        accessionsFile.text = '''accession
GCF_000001405.1'''

        def params = [
            accessions: accessionsFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processAccessions is called'
        def result = Bactopia.processAccessions(params)

        then: 'result should be empty because runtype is missing'
        result.size() == 0
    }

    def 'processAccessions should return empty list when accession header has wrong case'() {
        given: 'an accessions file with Accession instead of accession'
        def accessionsFile = tempDir.resolve('case_accessions.tsv').toFile()
        accessionsFile.text = '''Accession\truntype
GCF_000001405.1\t'''

        def params = [
            accessions: accessionsFile.absolutePath,
            genome_size: null,
            species: null
        ]

        when: 'processAccessions is called'
        def result = Bactopia.processAccessions(params)

        then: 'result should be empty due to case mismatch'
        result.size() == 0
    }

    def 'processAccession should handle GCF assembly accession'() {
        given: 'parameters with GCF accession'
        def params = [
            accession: 'GCF_000001405.39',
            genome_size: 5000000,
            species: 'Human',
            ont: false
        ]

        when: 'processAccession is called'
        def result = Bactopia.processAccession(params)

        then: 'result should contain assembly accession data'
        result.size() == 1
        result[0].meta.id == 'GCF_000001405'
        result[0].meta.name == 'GCF_000001405'
        result[0].meta.runtype == 'assembly_accession'
    }

    def 'processAccession should handle GCA assembly accession'() {
        given: 'parameters with GCA accession'
        def params = [
            accession: 'GCA_000001405.15',
            genome_size: 5000000,
            species: 'Human',
            ont: false
        ]

        when: 'processAccession is called'
        def result = Bactopia.processAccession(params)

        then: 'result should contain assembly accession data'
        result[0].meta.id == 'GCA_000001405'
        result[0].meta.runtype == 'assembly_accession'
    }

    def 'processAccession should handle SRA experiment accessions'() {
        given: 'parameters with SRX accession'
        def params = [
            accession: 'SRX12345678',
            genome_size: 5000000,
            species: 'E. coli',
            ont: false
        ]

        when: 'processAccession is called'
        def result = Bactopia.processAccession(params)

        then: 'result should contain SRA accession data'
        result[0].meta.id == 'SRX12345678'
        result[0].meta.runtype == 'sra_accession'
    }

    def 'processAccession should handle ONT SRA accession'() {
        given: 'parameters with ONT SRA accession'
        def params = [
            accession: 'ERX12345678',
            genome_size: 5000000,
            species: 'E. coli',
            ont: true
        ]

        when: 'processAccession is called'
        def result = Bactopia.processAccession(params)

        then: 'result should contain ONT SRA accession data'
        result[0].meta.runtype == 'sra_accession_ont'
    }

    def 'processAccession should handle DRX accession'() {
        given: 'parameters with DRX accession'
        def params = [
            accession: 'DRX12345678',
            genome_size: 5000000,
            species: 'E. coli',
            ont: false
        ]

        when: 'processAccession is called'
        def result = Bactopia.processAccession(params)

        then: 'result should contain DRX accession data'
        result[0].meta.id == 'DRX12345678'
        result[0].meta.runtype == 'sra_accession'
    }

    def 'collectBactopiaInputs should delegate to processFOFN for is_fofn runtype'() {
        given: 'a FOFN file'
        def fofnFile = tempDir.resolve('samples.tsv').toFile()
        fofnFile.text = '''sample\truntype\tr1\tr2\tse\tont\tassembly\tgenome_size\tspecies
sample1\tsingle-end\t\t\t/path/to/r1.fq\t\t\t\t'''

        def params = [
            samples: fofnFile.absolutePath,
            genome_size: null,
            species: null,
        ]

        when: 'collectBactopiaInputs is called with is_fofn'
        def result = Bactopia.collectBactopiaInputs(params, 'is_fofn')

        then: 'result should be from processFOFN'
        result.size() == 1
        result[0].meta.id == 'sample1'
    }

    def 'collectBactopiaInputs should delegate to processAccessions for is_accessions runtype'() {
        given: 'an accessions file'
        def accessionsFile = tempDir.resolve('accessions.tsv').toFile()
        accessionsFile.text = '''accession\truntype\tspecies\tgenome_size
GCF_000001405.1\t\t\t'''

        def params = [
            accessions: accessionsFile.absolutePath,
            genome_size: null,
            species: null,
        ]

        when: 'collectBactopiaInputs is called with is_accessions'
        def result = Bactopia.collectBactopiaInputs(params, 'is_accessions')

        then: 'result should be from processAccessions'
        result.size() == 1
        result[0].meta.runtype == 'assembly_accession'
    }

    def 'collectBactopiaInputs should delegate to processAccession for is_accession runtype'() {
        given: 'parameters with single accession'
        def params = [
            accession: 'GCF_000001405.1',
            genome_size: null,
            species: null,
            ont: false,
        ]

        when: 'collectBactopiaInputs is called with is_accession'
        def result = Bactopia.collectBactopiaInputs(params, 'is_accession')

        then: 'result should be from processAccession'
        result.size() == 1
        result[0].meta.runtype == 'assembly_accession'
    }

    def 'meta map should contain all required fields'() {
        given: 'parameters for sample'
        def params = [
            sample: 'test_sample',
            r1: '/path/to/r1.fq',
            r2: '/path/to/r2.fq',
            genome_size: 5000000,
            species: 'Test species',
        ]

        when: 'collectBactopiaInputs is called'
        def result = Bactopia.collectBactopiaInputs(params, 'paired-end')

        then: 'meta should contain all required fields'
        result[0].meta.containsKey('id')
        result[0].meta.containsKey('name')
        result[0].meta.containsKey('runtype')
        result[0].meta.containsKey('genome_size')
        result[0].meta.containsKey('species')
    }
}
