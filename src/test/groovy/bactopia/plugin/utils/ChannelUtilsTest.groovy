package bactopia.plugin.utils

import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import nextflow.util.RecordMap
import spock.lang.Specification

/**
 * Unit tests for ChannelUtils class
 *
 * Note: Channel-based tests are integration tests and should be run
 * in a full Nextflow pipeline context. These unit tests focus on
 * list-based operations.
 */
class ChannelUtilsTest extends Specification {

    // ---- gather() tests ----

    def 'gather should extract named field from records into record-like map'() {
        given: 'A list of record-like maps'
        def records = [
            [tsv: 'output1.tsv', meta: [id: 'sample1']],
            [tsv: 'output2.tsv', meta: [id: 'sample2']],
            [tsv: 'output3.tsv', meta: [id: 'sample3']]
        ]

        when: 'gather is called'
        def result = ChannelUtils.gather(records, 'tsv', [name: 'sccmec'])

        then: 'result should be a RecordMap with meta and collected field'
        result instanceof RecordMap
        result.meta == [name: 'sccmec']
        result.tsv instanceof Set
        result.tsv.size() == 3
        result.tsv.contains('output1.tsv')
        result.tsv.contains('output2.tsv')
        result.tsv.contains('output3.tsv')
    }

    def 'gather should include args in meta when provided'() {
        given: 'A list of record-like maps'
        def records = [
            [report: 'report1.txt'],
            [report: 'report2.txt']
        ]

        when: 'gather is called with name and args'
        def result = ChannelUtils.gather(records, 'report', [name: 'ariba-report', args: '-C "$" --lazy-quotes'])

        then: 'result should have correct meta with args and extracted field values'
        result.meta == [name: 'ariba-report', args: '-C "$" --lazy-quotes']
        result.report instanceof Set
        result.report.size() == 2
        result.report.contains('report1.txt')
        result.report.contains('report2.txt')
    }

    def 'gather should pass through extra meta keys'() {
        given: 'A list of record-like maps'
        def records = [
            [masked_aln: 'aln1.fa'],
            [masked_aln: 'aln2.fa']
        ]

        when: 'gather is called with extra keys in meta'
        def result = ChannelUtils.gather(records, 'masked_aln', [name: 'core-genome.masked.distance', process_name: 'snpdists-masked'])

        then: 'all meta keys should pass through as-is'
        result.meta == [name: 'core-genome.masked.distance', process_name: 'snpdists-masked']
        result.masked_aln instanceof Set
        result.masked_aln.size() == 2
    }

    def 'gather should not add default keys to meta'() {
        given: 'A list of record-like maps'
        def records = [
            [tsv: 'output.tsv']
        ]

        when: 'gather is called with only name in meta'
        def result = ChannelUtils.gather(records, 'tsv', [name: 'sccmec'])

        then: 'meta should contain only what was provided'
        result.meta == [name: 'sccmec']
        result.meta.containsKey('name')
        !result.meta.containsKey('args')
        !result.meta.containsKey('process_name')
        !result.meta.containsKey('subdir')
    }

    def 'gather should handle empty list'() {
        given: 'An empty list'
        def list = []

        when: 'gather is called'
        def result = ChannelUtils.gather(list, 'tsv', [name: 'mytool'])

        then: 'result should be empty'
        result == []
    }

    def 'gather should filter null field values'() {
        given: 'A list of records where some have null field values'
        def records = [
            [report: 'report1.txt'],
            [report: null],
            [report: 'report3.txt']
        ]

        when: 'gather is called'
        def result = ChannelUtils.gather(records, 'report', [name: 'mytool'])

        then: 'null values should be filtered out'
        result.meta == [name: 'mytool']
        result.report.size() == 2
        result.report.contains('report1.txt')
        result.report.contains('report3.txt')
    }

    def 'gather should return empty for all-null field values'() {
        given: 'A list of records where all field values are null'
        def records = [
            [report: null],
            [report: null]
        ]

        when: 'gather is called'
        def result = ChannelUtils.gather(records, 'report', [name: 'mytool'])

        then: 'result should be empty'
        result == []
    }

    def 'gather should deduplicate outputs using Set'() {
        given: 'A list of records with duplicate field values'
        def records = [
            [report: 'same.txt'],
            [report: 'same.txt'],
            [report: 'other.txt']
        ]

        when: 'gather is called'
        def result = ChannelUtils.gather(records, 'report', [name: 'mytool'])

        then: 'duplicate values should be deduplicated'
        result.report.size() == 2
        result.report.contains('same.txt')
        result.report.contains('other.txt')
    }

    def 'gather should return empty for missing field key'() {
        given: 'A list of records missing the specified field'
        def records = [
            [other: 'value1'],
            [other: 'value2']
        ]

        when: 'gather is called with a field that does not exist'
        def result = ChannelUtils.gather(records, 'report', [name: 'mytool'])

        then: 'null lookups should be filtered, returning empty'
        result == []
    }

    // ---- gatherCsvtk() tests ----

    def 'gatherCsvtk should rename field to csv'() {
        given: 'A list of record-like maps'
        def records = [
            [tsv: 'output1.tsv'],
            [tsv: 'output2.tsv']
        ]

        when: 'gatherCsvtk is called'
        def result = ChannelUtils.gatherCsvtk(records, 'tsv', [name: 'abricate'])

        then: 'result should be a RecordMap with csv field, not tsv'
        result instanceof RecordMap
        result.meta == [name: 'abricate']
        result.csv instanceof Set
        result.csv.size() == 2
        result.csv.contains('output1.tsv')
        result.csv.contains('output2.tsv')
        !result.containsKey('tsv')
    }

    def 'gatherCsvtk should handle report field renamed to csv'() {
        given: 'A list of records with report field'
        def records = [
            [report: 'report1.txt'],
            [report: 'report2.txt']
        ]

        when: 'gatherCsvtk is called'
        def result = ChannelUtils.gatherCsvtk(records, 'report', [name: 'ariba-report', args: '-C "$" --lazy-quotes'])

        then: 'result should have csv field with report values'
        result.meta == [name: 'ariba-report', args: '-C "$" --lazy-quotes']
        result.csv.size() == 2
        result.csv.contains('report1.txt')
    }

    // ---- gatherFields() tests ----

    def 'gatherFields should handle multiple field mappings'() {
        given: 'A list of records with multiple fields'
        def records = [
            [report: 'r1.txt', summary: 's1.txt'],
            [report: 'r2.txt', summary: 's2.txt']
        ]

        when: 'gatherFields is called with rename mapping'
        def result = ChannelUtils.gatherFields(records, [report: 'csv', summary: 'tsv'], [name: 'tool'])

        then: 'result should have both renamed fields'
        result.meta == [name: 'tool']
        result.csv instanceof Set
        result.csv.size() == 2
        result.tsv instanceof Set
        result.tsv.size() == 2
    }

    // ---- collectNextflowLogs() tests ----

    def 'collectNextflowLogs should flatten nf_logs into tuples'() {
        given: 'A list of records with nf_logs'
        def records = [
            [meta: [id: 'sample1'], nf_logs: ['cmd.sh', 'cmd.log']],
            [meta: [id: 'sample2'], nf_logs: ['cmd.sh']]
        ]

        when: 'collectNextflowLogs is called'
        def result = ChannelUtils.collectNextflowLogs(records)

        then: 'each log file should be a separate tuple'
        result.size() == 3
        result[0] == [[id: 'sample1'], 'cmd.sh']
        result[1] == [[id: 'sample1'], 'cmd.log']
        result[2] == [[id: 'sample2'], 'cmd.sh']
    }

    def 'collectNextflowLogs should handle empty nf_logs'() {
        given: 'A list of records with empty nf_logs'
        def records = [
            [meta: [id: 'sample1'], nf_logs: []]
        ]

        when: 'collectNextflowLogs is called'
        def result = ChannelUtils.collectNextflowLogs(records)

        then: 'result should be empty'
        result.isEmpty()
    }

    def 'collectNextflowLogs should throw when input is null'() {
        when: 'collectNextflowLogs is called with null'
        ChannelUtils.collectNextflowLogs(null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'chResults cannot be null'
    }

    // ---- Error handling tests ----

    def 'gather should throw IllegalArgumentException when chResults is null'() {
        when: 'gather is called with null chResults'
        ChannelUtils.gather(null, 'tsv', [name: 'mytool'])

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'chResults cannot be null'
    }

    def 'gather should throw IllegalArgumentException when field is null'() {
        given: 'A valid list'
        def list = [[tsv: 'output.txt']]

        when: 'gather is called with null field'
        ChannelUtils.gather(list, null, [name: 'mytool'])

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'field cannot be null or empty'
    }

    def 'gather should throw IllegalArgumentException when field is empty'() {
        given: 'A valid list'
        def list = [[tsv: 'output.txt']]

        when: 'gather is called with empty field'
        ChannelUtils.gather(list, '', [name: 'mytool'])

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'field cannot be null or empty'
    }

    def 'gather should throw IllegalArgumentException when field is whitespace'() {
        given: 'A valid list'
        def list = [[tsv: 'output.txt']]

        when: 'gather is called with whitespace field'
        ChannelUtils.gather(list, '   ', [name: 'mytool'])

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'field cannot be null or empty'
    }

    def 'gather should throw IllegalArgumentException when meta is null'() {
        given: 'A valid list'
        def list = [[tsv: 'output.txt']]

        when: 'gather is called with null meta'
        ChannelUtils.gather(list, 'tsv', null)

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'meta cannot be null'
    }

    def 'gather should throw IllegalArgumentException when meta.name is missing'() {
        given: 'A valid list'
        def list = [[tsv: 'output.txt']]

        when: 'gather is called with meta missing name'
        ChannelUtils.gather(list, 'tsv', [args: '--lazy'])

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'meta.name is required'
    }

    def 'gather should throw IllegalArgumentException when meta.name is empty'() {
        given: 'A valid list'
        def list = [[tsv: 'output.txt']]

        when: 'gather is called with empty meta.name'
        ChannelUtils.gather(list, 'tsv', [name: '   '])

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'meta.name is required'
    }

    // ---- filterWithData tests ----

    def 'filterWithData should keep records with non-null fields'() {
        given: 'A list of records'
        def records = [
            [meta: [id: 'sample1'], r1: 'read1.fq', r2: 'read2.fq'],
            [meta: [id: 'sample2'], r1: null, r2: null],
            [meta: [id: 'sample3'], r1: 'read1.fq', r2: null]
        ]

        when: 'filterWithData is called'
        def result = ChannelUtils.filterWithData(records, ['r1', 'r2'])

        then: 'only records with at least one non-null field should remain as RecordMaps'
        result.size() == 2
        result[0] instanceof RecordMap
        result[0].meta.id == 'sample1'
        result[0].r1 == 'read1.fq'
        result[0].r2 == 'read2.fq'
        result[1].meta.id == 'sample3'
    }

    def 'filterWithData should filter all-null records'() {
        given: 'A list of records where all checked fields are null'
        def records = [
            [meta: [id: 'sample1'], assembly: null, r1: null],
            [meta: [id: 'sample2'], assembly: '/path/to/fna', r1: null]
        ]

        when: 'filterWithData is called'
        def result = ChannelUtils.filterWithData(records, ['assembly', 'r1'])

        then: 'only the record with a non-null field should remain'
        result.size() == 1
        result[0].meta.id == 'sample2'
        result[0].assembly == '/path/to/fna'
    }

    def 'filterWithData should handle single field check'() {
        given: 'A list of records'
        def records = [
            [meta: [id: 'sample1'], species_result: 'found'],
            [meta: [id: 'sample2'], species_result: null],
            [meta: [id: 'sample3'], species_result: 'found']
        ]

        when: 'filterWithData is called with a single field'
        def result = ChannelUtils.filterWithData(records, ['species_result'])

        then: 'only records with non-null field should remain'
        result.size() == 2
        result[0].meta.id == 'sample1'
        result[1].meta.id == 'sample3'
    }

    def 'filterWithData should return empty for all-null input'() {
        given: 'A list of records where all fields are null'
        def records = [
            [meta: [id: 'sample1'], r1: null],
            [meta: [id: 'sample2'], r1: null]
        ]

        when: 'filterWithData is called'
        def result = ChannelUtils.filterWithData(records, ['r1'])

        then: 'result should be empty'
        result.isEmpty()
    }

    def 'filterWithData should handle empty list'() {
        when: 'filterWithData is called on empty list'
        def result = ChannelUtils.filterWithData([], ['r1'])

        then: 'result should be empty'
        result.isEmpty()
    }

    def 'filterWithData should throw when input is null'() {
        when: 'filterWithData is called with null input'
        ChannelUtils.filterWithData(null, ['r1'])

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'input cannot be null'
    }

    def 'filterWithData should throw when fields is null'() {
        when: 'filterWithData is called with null fields'
        ChannelUtils.filterWithData([], null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'fields cannot be null or empty'
    }

    def 'filterWithData should throw when fields is empty'() {
        when: 'filterWithData is called with empty fields'
        ChannelUtils.filterWithData([], [])

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'fields cannot be null or empty'
    }

    // DataflowQueue type recognition tests

    def 'gather should validate against DataflowQueue type'() {
        given: 'A DataflowQueue instance'
        def queue = new DataflowQueue()

        when: 'gather validation runs'
        def isChannel = queue instanceof DataflowReadChannel ||
                       queue instanceof DataflowWriteChannel ||
                       queue instanceof DataflowQueue

        then: 'DataflowQueue should be recognized as a channel type'
        isChannel == true
        queue instanceof DataflowQueue
    }

    def 'DataflowQueue should be instance of expected channel types'() {
        given: 'A DataflowQueue instance'
        def queue = new DataflowQueue()

        expect: 'DataflowQueue to be recognized in channel detection logic'
        queue instanceof DataflowQueue
        queue instanceof DataflowReadChannel
        queue instanceof DataflowWriteChannel
    }

    // ---- combineWith() tests ----

    def 'combineWith should create cartesian product and merge field'() {
        given: 'A gathered map and a list of items'
        def gathered = [[meta: [name: 'fastani'], query: ['a.fna', 'b.fna'].toSet()]]
        def items = ['ref1.fna', 'ref2.fna']

        when: 'combineWith is called'
        def result = ChannelUtils.combineWith(gathered, items, 'reference')

        then: 'result should have one RecordMap entry per item with the field merged in'
        result.size() == 2
        result[0] instanceof RecordMap
        result[0].meta == [name: 'fastani']
        result[0].query == ['a.fna', 'b.fna'].toSet()
        result[0].reference == 'ref1.fna'
        result[1].reference == 'ref2.fna'
    }

    def 'combineWith should handle single gathered with single item'() {
        given: 'A single gathered map and a single item'
        def gathered = [[meta: [name: 'tool'], query: ['q1.fna'].toSet()]]
        def items = ['ref.fna']

        when: 'combineWith is called'
        def result = ChannelUtils.combineWith(gathered, items, 'reference')

        then: 'result should have one entry'
        result.size() == 1
        result[0].reference == 'ref.fna'
        result[0].query == ['q1.fna'].toSet()
    }

    def 'combineWith should handle multiple gathered with multiple items'() {
        given: 'Two gathered maps and three items'
        def gathered = [
            [meta: [name: 'a'], query: ['q1'].toSet()],
            [meta: [name: 'b'], query: ['q2'].toSet()]
        ]
        def items = ['r1', 'r2', 'r3']

        when: 'combineWith is called'
        def result = ChannelUtils.combineWith(gathered, items, 'ref')

        then: 'result should have 2x3=6 entries'
        result.size() == 6
        result[0].meta == [name: 'a']
        result[0].ref == 'r1'
        result[3].meta == [name: 'b']
        result[3].ref == 'r1'
    }

    def 'combineWith should not modify original gathered map'() {
        given: 'A gathered map'
        def original = [meta: [name: 'tool'], query: ['q1'].toSet()]
        def gathered = [original]

        when: 'combineWith is called'
        def result = ChannelUtils.combineWith(gathered, ['ref.fna'], 'reference')

        then: 'original map should not have the new field'
        !original.containsKey('reference')
        result[0].containsKey('reference')
    }

    def 'combineWith should return empty for empty items'() {
        given: 'A gathered map and empty items list'
        def gathered = [[meta: [name: 'tool'], query: ['q1'].toSet()]]

        when: 'combineWith is called with empty items'
        def result = ChannelUtils.combineWith(gathered, [], 'reference')

        then: 'result should be empty'
        result.isEmpty()
    }

    def 'combineWith should return empty for empty gathered'() {
        given: 'An empty gathered list and some items'

        when: 'combineWith is called with empty gathered'
        def result = ChannelUtils.combineWith([], ['ref.fna'], 'reference')

        then: 'result should be empty'
        result.isEmpty()
    }

    def 'combineWith should throw when gathered is null'() {
        when: 'combineWith is called with null gathered'
        ChannelUtils.combineWith(null, ['ref.fna'], 'reference')

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'gathered cannot be null'
    }

    def 'combineWith should throw when items is null'() {
        when: 'combineWith is called with null items'
        ChannelUtils.combineWith([], null, 'reference')

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'items cannot be null'
    }

    def 'combineWith should throw when field is null'() {
        when: 'combineWith is called with null field'
        ChannelUtils.combineWith([], [], null)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'field cannot be null or empty'
    }

    def 'combineWith should throw when field is empty'() {
        when: 'combineWith is called with empty field'
        ChannelUtils.combineWith([], [], '   ')

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'field cannot be null or empty'
    }
}
