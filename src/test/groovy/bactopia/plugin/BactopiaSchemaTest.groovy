package bactopia.plugin

import nextflow.util.Duration
import nextflow.util.MemoryUnit
import spock.lang.Specification
import spock.lang.TempDir
import java.nio.file.Path
import java.nio.file.Files

/**
 * Test class for BactopiaSchema
 */
class BactopiaSchemaTest extends Specification {

    BactopiaSchema schema

    def setup() {
        def config = new BactopiaConfig()
        schema = new BactopiaSchema(config)
    }

    def "cleanParameters removes null values"() {
        given: "parameters with null values"
        def params = [
            validParam: 'value',
            nullParam: null,
            anotherValid: 'test'
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "null values are removed"
        result.containsKey('validParam')
        result.containsKey('anotherValid')
        !result.containsKey('nullParam')
        result.validParam == 'value'
        result.anotherValid == 'test'
    }

    def "cleanParameters removes empty string values"() {
        given: "parameters with empty strings"
        def params = [
            validParam: 'value',
            emptyParam: '',
            anotherValid: 'test'
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "empty strings are removed"
        result.containsKey('validParam')
        result.containsKey('anotherValid')
        !result.containsKey('emptyParam')
    }

    def "cleanParameters removes false boolean values"() {
        given: "parameters with false boolean"
        def params = [
            validParam: 'value',
            falseParam: false,
            trueParam: true
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "false values are removed but true values remain"
        result.containsKey('validParam')
        result.containsKey('trueParam')
        !result.containsKey('falseParam')
        result.trueParam == true
    }

    def "cleanParameters preserves zero integer values"() {
        given: "parameters with zero value"
        def params = [
            zeroParam: 0,
            oneParam: 1,
            negativeParam: -1
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "zero value is preserved"
        result.containsKey('zeroParam')
        result.containsKey('oneParam')
        result.containsKey('negativeParam')
        result.zeroParam == 0
        result.oneParam == 1
        result.negativeParam == -1
    }

    def "cleanParameters converts MemoryUnit to string"() {
        given: "parameters with MemoryUnit"
        def params = [
            memory: new MemoryUnit('4 GB'),
            otherParam: 'value'
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "MemoryUnit is converted to string"
        result.memory instanceof String
        result.memory == '4 GB'
        result.otherParam == 'value'
    }

    def "cleanParameters converts Duration to string"() {
        given: "parameters with Duration"
        def params = [
            timeout: Duration.of('1h'),
            otherParam: 'value'
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "Duration is converted to string"
        result.timeout instanceof String
        result.timeout == '1h'
        result.otherParam == 'value'
    }

    def "cleanParameters processes LinkedHashMap as nested Map"() {
        given: "parameters with LinkedHashMap"
        def params = [
            mapParam: new LinkedHashMap([key1: 'value1', key2: 'value2', nullKey: null]),
            otherParam: 'value'
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "LinkedHashMap is processed recursively as Map"
        result.mapParam instanceof Map
        result.mapParam.key1 == 'value1'
        result.mapParam.key2 == 'value2'
        !result.mapParam.containsKey('nullKey')
        result.otherParam == 'value'
    }

    def "cleanParameters recursively cleans nested Map parameters"() {
        given: "parameters with nested maps"
        def params = [
            topLevel: 'value',
            nested: [
                innerParam: 'test',
                innerNull: null,
                innerMemory: new MemoryUnit('2 GB')
            ] as Map
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "nested maps are recursively cleaned"
        result.topLevel == 'value'
        result.nested instanceof Map
        result.nested.innerParam == 'test'
        !result.nested.containsKey('innerNull')
        result.nested.innerMemory instanceof String
        result.nested.innerMemory == '2 GB'
    }

    def "cleanParameters converts integer sample names to string"() {
        given: "parameters with integer sample value"
        def params = [
            sample: 12345,
            otherParam: 'value'
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "sample integer is converted to string"
        result.sample instanceof String
        result.sample == '12345'
        result.otherParam == 'value'
    }

    def "cleanParameters preserves string sample names"() {
        given: "parameters with string sample value"
        def params = [
            sample: 'sample123',
            otherParam: 'value'
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "sample string is preserved"
        result.sample == 'sample123'
        result.otherParam == 'value'
    }

    def "cleanParameters handles multiple type conversions in same map"() {
        given: "parameters with multiple types"
        def params = [
            memory: new MemoryUnit('8 GB'),
            timeout: Duration.of('30m'),
            validString: 'test',
            nullValue: null,
            zeroValue: 0,
            sample: 999
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "all conversions are applied correctly"
        result.memory instanceof String
        result.memory == '8 GB'
        result.timeout instanceof String
        result.timeout == '30m'
        result.validString == 'test'
        !result.containsKey('nullValue')
        result.zeroValue == 0
        result.sample instanceof String
        result.sample == '999'
    }

    def "cleanParameters handles deeply nested structures"() {
        given: "parameters with deep nesting"
        def params = [
            level1: [
                level2: [
                    level3: 'value',
                    level3Null: null,
                    level3Memory: new MemoryUnit('1 GB')
                ] as Map
            ] as Map
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "deep nesting is properly cleaned"
        result.level1 instanceof Map
        result.level1.level2 instanceof Map
        result.level1.level2.level3 == 'value'
        !result.level1.level2.containsKey('level3Null')
        result.level1.level2.level3Memory instanceof String
        result.level1.level2.level3Memory == '1 GB'
    }

    def "cleanParameters returns new map instance"() {
        given: "original parameters"
        def params = [param1: 'value1', param2: 'value2']

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "result is a new instance with same content"
        !result.is(params)
        result.param1 == 'value1'
        result.param2 == 'value2'
    }

    def "cleanParameters handles empty map"() {
        given: "empty parameters map"
        def params = [:]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "returns empty map"
        result.isEmpty()
    }

    def "cleanParameters handles mixed valid and invalid values"() {
        given: "parameters with mix of valid and invalid"
        def params = [
            valid1: 'test',
            invalid1: null,
            valid2: 100,
            invalid2: false,
            valid3: true,
            invalid3: '',
            valid4: 0
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "only valid values remain"
        result.size() == 4
        result.containsKey('valid1')
        result.containsKey('valid2')
        result.containsKey('valid3')
        result.containsKey('valid4')
        result.valid1 == 'test'
        result.valid2 == 100
        result.valid3 == true
        result.valid4 == 0
    }

    def "cleanParameters handles list values"() {
        given: "parameters with list values"
        def params = [
            listParam: ['item1', 'item2', 'item3'],
            otherParam: 'value'
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "lists are preserved"
        result.listParam instanceof List
        result.listParam == ['item1', 'item2', 'item3']
        result.otherParam == 'value'
    }

    def "cleanParameters handles negative numbers"() {
        given: "parameters with negative numbers"
        def params = [
            negativeInt: -10,
            negativeDouble: -3.14,
            positiveInt: 5
        ]

        when: "cleaning parameters"
        def result = schema.cleanParameters(params)

        then: "negative numbers are preserved"
        result.negativeInt == -10
        result.negativeDouble == -3.14
        result.positiveInt == 5
    }

    // ========================================================================
    // Tests for validateBactopiaToolParams() - scrubber workflow
    // ========================================================================

    private Map baseToolParams(Map overrides = [:]) {
        def params = [
            bactopia: 's3://bucket/bactopia-results',
            workflow: [name: 'scrubber'],
            include: null,
            exclude: null,
        ]
        params.putAll(overrides)
        return params
    }

    // -- deacon (first priority) --

    def "validateBactopiaToolParams scrubber with cloud deacon_db succeeds"() {
        given:
        def params = baseToolParams(
            deacon_db: 's3://bucket/deacon-db',
            download_deacon: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams scrubber with download_deacon bypasses validation"() {
        given:
        def params = baseToolParams(
            deacon_db: '/fake/nonexistent/path',
            download_deacon: true,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams scrubber with local deacon_db validates file"() {
        given:
        GroovySpy(BactopiaUtils, global: true)
        def params = baseToolParams(
            deacon_db: '/path/to/deacon.db',
            download_deacon: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        1 * BactopiaUtils.fileExists('/path/to/deacon.db') >> true
        result == "success"
    }

    // -- nohuman (second priority) --

    def "validateBactopiaToolParams scrubber with cloud nohuman_db succeeds"() {
        given:
        def params = baseToolParams(
            use_nohuman: true,
            nohuman_db: 's3://bucket/nohuman-db',
            download_nohuman: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams scrubber with download_nohuman bypasses validation"() {
        given:
        def params = baseToolParams(
            use_nohuman: true,
            nohuman_db: '/fake/nonexistent/path',
            download_nohuman: true,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams scrubber with use_nohuman but no db errors"() {
        given:
        def params = baseToolParams(
            use_nohuman: true,
            nohuman_db: null,
            download_nohuman: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams scrubber with local tar.gz nohuman_db validates file"() {
        given:
        GroovySpy(BactopiaUtils, global: true)
        def params = baseToolParams(
            use_nohuman: true,
            nohuman_db: '/path/to/nohuman.tar.gz',
            download_nohuman: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        1 * BactopiaUtils.isLocal('/path/to/nohuman.tar.gz') >> true
        1 * BactopiaUtils.fileNotFound('/path/to/nohuman.tar.gz', 'nohuman_db') >> 0
        result == "success"
    }

    def "validateBactopiaToolParams scrubber with local directory nohuman_db validates hash.k2d"() {
        given:
        GroovySpy(BactopiaUtils, global: true)
        def params = baseToolParams(
            use_nohuman: true,
            nohuman_db: '/path/to/nohuman_dir',
            download_nohuman: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        1 * BactopiaUtils.isLocal('/path/to/nohuman_dir') >> true
        1 * BactopiaUtils.fileNotFound('/path/to/nohuman_dir/hash.k2d', 'nohuman_db') >> 0
        result == "success"
    }

    // -- srascrubber (third priority) --

    def "validateBactopiaToolParams scrubber with use_srascrubber succeeds"() {
        given:
        def params = baseToolParams(
            use_srascrubber: true,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    // -- no scrubber configured --

    def "validateBactopiaToolParams scrubber without any scrubber option errors"() {
        given:
        def params = baseToolParams(
            use_srascrubber: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams with include and exclude together errors"() {
        given:
        def params = baseToolParams(
            include: 's3://bucket/include.txt',
            exclude: 's3://bucket/exclude.txt',
            nohuman_db: 's3://bucket/db',
            use_srascrubber: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams missing bactopia param reports error"() {
        given:
        def params = baseToolParams(
            bactopia: null,
            nohuman_db: 's3://bucket/db',
            use_srascrubber: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams ariba without ariba_db errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'ariba'],
            ariba_db: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams ariba with ariba_db succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'ariba'],
            ariba_db: 's3://bucket/ariba-db',
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams bakta with cloud bakta_db succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'bakta'],
            bakta_db: 's3://bucket/bakta-db',
            download_bakta: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams bakta without bakta_db errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'bakta'],
            bakta_db: null,
            download_bakta: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams bakta with download_bakta skips validation"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'bakta'],
            bakta_db: '/fake/path/bakta.tar.gz',
            download_bakta: true,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams blastn without blastn_query errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'blastn'],
            blastn_query: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams blastn with cloud blastn_query succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'blastn'],
            blastn_query: 's3://bucket/query.fasta',
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams blastp without blastp_query errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'blastp'],
            blastp_query: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams blastx without blastx_query errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'blastx'],
            blastx_query: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams eggnog with cloud eggnog_db succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'eggnog'],
            eggnog_db: 's3://bucket/eggnog-db',
            download_eggnog: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams eggnog without eggnog_db errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'eggnog'],
            eggnog_db: null,
            download_eggnog: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams kraken2 with cloud kraken2_db succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'kraken2'],
            kraken2_db: 's3://bucket/kraken2-db',
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams kraken2 without kraken2_db errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'kraken2'],
            kraken2_db: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams gtdb with cloud gtdb succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'gtdb'],
            gtdb: 's3://bucket/gtdb',
            download_gtdb: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams gtdb without gtdb errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'gtdb'],
            gtdb: null,
            download_gtdb: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams snippy with cloud reference succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'snippy'],
            reference: 's3://bucket/reference.fasta',
            accession: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams snippy with accession succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'snippy'],
            accession: 'GCF_000009045',
            reference: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams snippy without accession or reference errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'snippy'],
            accession: null,
            reference: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams snippy with both accession and reference errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'snippy'],
            accession: 'GCF_000009045',
            reference: 's3://bucket/reference.fasta',
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams sylph with cloud sylph_db succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'sylph'],
            sylph_db: 's3://bucket/sylph.syldb',
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams sylph without sylph_db errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'sylph'],
            sylph_db: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams mykrobe without mykrobe_species errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'mykrobe'],
            mykrobe_species: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams mykrobe with mykrobe_species succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'mykrobe'],
            mykrobe_species: 'staph',
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams mashdist without mash_sketch errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'mashdist'],
            mash_sketch: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams tblastn without tblastn_query errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'tblastn'],
            tblastn_query: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams tblastx without tblastx_query errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'tblastx'],
            tblastx_query: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams gamma without gamma_db errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'gamma'],
            gamma_db: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams midas with cloud midas_db succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'midas'],
            midas_db: 's3://bucket/midas-db',
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams midas without midas_db errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'midas'],
            midas_db: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams fastani with cloud reference succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'fastani'],
            fastani_reference: 's3://bucket/ref.fasta',
            fastani_pairwise: false,
            accession: null,
            accessions: null,
            species: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams fastani pairwise succeeds without reference"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'fastani'],
            fastani_pairwise: true,
            fastani_reference: null,
            accession: null,
            accessions: null,
            species: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams fastani without reference or pairwise errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'fastani'],
            fastani_pairwise: false,
            fastani_reference: null,
            accession: null,
            accessions: null,
            species: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams scoary without scoary_traits errors"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'scoary'],
            scoary_traits: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    def "validateBactopiaToolParams pangenome with cloud scoary_traits succeeds"() {
        given:
        def params = baseToolParams(
            workflow: [name: 'pangenome'],
            scoary_traits: 's3://bucket/traits.csv',
        )

        when:
        def result = BactopiaSchema.validateBactopiaToolParams(params)

        then:
        result == "success"
    }

    // ========================================================================
    // Tests for validateBactopiaParams() - teton workflow
    // ========================================================================

    private Map baseBactopiaParams(Map overrides = [:]) {
        def params = [
            accession: 'SRR1234567',
            workflow: [name: 'bactopia'],
            max_downloads: 3,
            check_samples: false,
            genome_size: null,
            samples: null,
            r1: null,
            r2: null,
            se: null,
            ont: null,
            assembly: null,
            accessions: null,
            use_bakta: false,
            bakta_db: null,
            download_bakta: false,
            nohuman_db: null,
            use_srascrubber: false,
            download_nohuman: false,
            kraken2_db: null,
            short_polish: null,
            hybrid: null,
            sample: null,
        ]
        params.putAll(overrides)
        return params
    }

    def "validateBactopiaParams accession returns is_accession"() {
        given:
        def params = baseBactopiaParams()

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "is_accession"
    }

    def "validateBactopiaParams paired-end with cloud paths returns paired-end"() {
        given:
        def params = baseBactopiaParams(
            accession: null,
            r1: 's3://bucket/reads_R1.fastq.gz',
            r2: 's3://bucket/reads_R2.fastq.gz',
            sample: 'test_sample',
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "paired-end"
    }

    def "validateBactopiaParams ont with cloud path returns ont"() {
        given:
        def params = baseBactopiaParams(
            accession: null,
            ont: 's3://bucket/reads.fastq.gz',
            sample: 'test_sample',
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "ont"
    }

    def "validateBactopiaParams single-end with cloud path returns single-end"() {
        given:
        def params = baseBactopiaParams(
            accession: null,
            se: 's3://bucket/reads.fastq.gz',
            sample: 'test_sample',
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "single-end"
    }

    def "validateBactopiaParams assembly with cloud path returns assembly"() {
        given:
        def params = baseBactopiaParams(
            accession: null,
            assembly: 's3://bucket/assembly.fasta.gz',
            sample: 'test_sample',
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "assembly"
    }

    def "validateBactopiaParams missing all inputs returns empty string"() {
        given:
        def params = baseBactopiaParams(accession: null)

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == ""
    }

    def "validateBactopiaParams r1 r2 and se together errors"() {
        given:
        def params = baseBactopiaParams(
            accession: null,
            r1: 's3://bucket/r1.fq.gz',
            r2: 's3://bucket/r2.fq.gz',
            se: 's3://bucket/se.fq.gz',
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == ""
    }

    def "validateBactopiaParams use_bakta without bakta_db errors"() {
        given:
        def params = baseBactopiaParams(
            use_bakta: true,
            bakta_db: null,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "is_accession"
    }

    // -- teton: deacon (first priority) --

    def "validateBactopiaParams teton with cloud deacon_db succeeds"() {
        given:
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: 's3://bucket/kraken2-db',
            deacon_db: 's3://bucket/deacon-db',
            download_deacon: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "is_accession"
    }

    def "validateBactopiaParams teton with download_deacon bypasses validation"() {
        given:
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: 's3://bucket/kraken2-db',
            deacon_db: '/fake/path',
            download_deacon: true,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "is_accession"
    }

    def "validateBactopiaParams teton with local deacon_db validates file"() {
        given:
        GroovySpy(BactopiaUtils, global: true)
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: 's3://bucket/kraken2-db',
            deacon_db: '/path/to/deacon.db',
            download_deacon: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        1 * BactopiaUtils.fileExists('/path/to/deacon.db') >> true
        result == "is_accession"
    }

    // -- teton: nohuman (second priority) --

    def "validateBactopiaParams teton with cloud nohuman_db succeeds"() {
        given:
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: 's3://bucket/kraken2-db',
            use_nohuman: true,
            nohuman_db: 's3://bucket/nohuman-db',
            download_nohuman: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "is_accession"
    }

    def "validateBactopiaParams teton with download_nohuman bypasses nohuman_db validation"() {
        given:
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: 's3://bucket/kraken2-db',
            use_nohuman: true,
            nohuman_db: '/fake/path',
            download_nohuman: true,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "is_accession"
    }

    def "validateBactopiaParams teton with use_nohuman but no db errors"() {
        given:
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: 's3://bucket/kraken2-db',
            use_nohuman: true,
            nohuman_db: null,
            download_nohuman: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "is_accession"
    }

    def "validateBactopiaParams teton with local nohuman_db tar.gz validates file"() {
        given:
        GroovySpy(BactopiaUtils, global: true)
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: 's3://bucket/kraken2-db',
            use_nohuman: true,
            nohuman_db: '/path/to/nohuman.tar.gz',
            download_nohuman: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        1 * BactopiaUtils.isLocal('/path/to/nohuman.tar.gz') >> true
        1 * BactopiaUtils.fileNotFound('/path/to/nohuman.tar.gz', 'nohuman_db') >> 0
        result == "is_accession"
    }

    def "validateBactopiaParams teton with local nohuman_db directory validates hash.k2d"() {
        given:
        GroovySpy(BactopiaUtils, global: true)
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: 's3://bucket/kraken2-db',
            use_nohuman: true,
            nohuman_db: '/path/to/nohuman_dir',
            download_nohuman: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        1 * BactopiaUtils.isLocal('/path/to/nohuman_dir') >> true
        1 * BactopiaUtils.fileNotFound('/path/to/nohuman_dir/hash.k2d', 'nohuman_db') >> 0
        result == "is_accession"
    }

    // -- teton: srascrubber (third priority) --

    def "validateBactopiaParams teton with use_srascrubber succeeds"() {
        given:
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: 's3://bucket/kraken2-db',
            use_srascrubber: true,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "is_accession"
    }

    // -- teton: no scrubber configured --

    def "validateBactopiaParams teton without any scrubber option errors"() {
        given:
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: 's3://bucket/kraken2-db',
            use_srascrubber: false,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "is_accession"
    }

    // -- teton: missing kraken2_db --

    def "validateBactopiaParams teton without kraken2_db errors"() {
        given:
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: null,
            use_srascrubber: true,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        result == "is_accession"
    }

    // -- teton: local kraken2_db validation --

    def "validateBactopiaParams teton with local kraken2_db tar.gz validates file"() {
        given:
        GroovySpy(BactopiaUtils, global: true)
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: '/path/to/kraken2.tar.gz',
            use_srascrubber: true,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        1 * BactopiaUtils.isLocal('/path/to/kraken2.tar.gz') >> true
        1 * BactopiaUtils.fileNotFound('/path/to/kraken2.tar.gz', 'kraken2_db') >> 0
        result == "is_accession"
    }

    def "validateBactopiaParams teton with local kraken2_db directory validates hash.k2d"() {
        given:
        GroovySpy(BactopiaUtils, global: true)
        def params = baseBactopiaParams(
            workflow: [name: 'teton'],
            kraken2_db: '/path/to/kraken2_dir',
            use_srascrubber: true,
        )

        when:
        def result = BactopiaSchema.validateBactopiaParams(params)

        then:
        1 * BactopiaUtils.isLocal('/path/to/kraken2_dir') >> true
        1 * BactopiaUtils.fileNotFound('/path/to/kraken2_dir/hash.k2d', 'kraken2_db') >> 0
        result == "is_accession"
    }
}
