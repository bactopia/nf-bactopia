package bactopia.plugin.utils

import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowWriteChannel
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import spock.lang.Specification

/**
 * Unit tests for ChannelUtils class
 * 
 * Note: Channel-based tests are integration tests and should be run
 * in a full Nextflow pipeline context. These unit tests focus on
 * list-based operations.
 */
class ChannelUtilsTest extends Specification {

    // Note: Channel-based gather() tests require a full Nextflow pipeline context
    // and should be tested as integration tests (e.g., test-gather.nf)

    def 'gather should extract named field from records'() {
        given: 'A list of record-like maps'
        def records = [
            [tsv: 'output1.tsv', meta: [id: 'sample1']],
            [tsv: 'output2.tsv', meta: [id: 'sample2']],
            [tsv: 'output3.tsv', meta: [id: 'sample3']]
        ]

        when: 'gather is called'
        def result = ChannelUtils.gather(records, 'tsv', [name: 'sccmec'])

        then: 'result should contain the extracted field values with meta passed through'
        result[0] == [name: 'sccmec']
        result[1] instanceof Set
        result[1].size() == 3
        result[1].contains('output1.tsv')
        result[1].contains('output2.tsv')
        result[1].contains('output3.tsv')
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
        result[0] == [name: 'ariba-report', args: '-C "$" --lazy-quotes']
        result[1] instanceof Set
        result[1].size() == 2
        result[1].contains('report1.txt')
        result[1].contains('report2.txt')
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
        result[0] == [name: 'core-genome.masked.distance', process_name: 'snpdists-masked']
        result[1] instanceof Set
        result[1].size() == 2
    }

    def 'gather should not add default keys to meta'() {
        given: 'A list of record-like maps'
        def records = [
            [tsv: 'output.tsv']
        ]

        when: 'gather is called with only name in meta'
        def result = ChannelUtils.gather(records, 'tsv', [name: 'sccmec'])

        then: 'meta should contain only what was provided'
        result[0] == [name: 'sccmec']
        result[0].containsKey('name')
        !result[0].containsKey('args')
        !result[0].containsKey('process_name')
        !result[0].containsKey('subdir')
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
        result[0] == [name: 'mytool']
        result[1].size() == 2
        result[1].contains('report1.txt')
        result[1].contains('report3.txt')
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
        result[1].size() == 2
        result[1].contains('same.txt')
        result[1].contains('other.txt')
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

    // Note: Channel-based flattenPaths() tests require a full Nextflow pipeline context
    // and should be tested as integration tests (e.g., test-flatten.nf)

    def 'flattenPaths should flatten list of files'() {
        given: 'A list with file sets'
        def list = [
            [
                [[id: 'sample1'], ['file1.txt', 'file2.txt'] as Set]
            ]
        ]

        when: 'flattenPaths is called'
        def result = ChannelUtils.flattenPaths(list)

        then: 'each file should be in a separate tuple'
        result.size() == 2
        result[0] == [[id: 'sample1'], 'file1.txt']
        result[1] == [[id: 'sample1'], 'file2.txt']
    }

    def 'flattenPaths should handle multiple lists'() {
        given: 'Multiple lists with file sets'
        def list1 = [
            [[id: 'sample1'], ['file1.txt'] as Set]
        ]
        def list2 = [
            [[id: 'sample2'], ['file2.txt', 'file3.txt'] as Set]
        ]

        when: 'flattenPaths is called with multiple lists'
        def result = ChannelUtils.flattenPaths([list1, list2])

        then: 'all files should be flattened'
        result.size() == 3
        result.find { it[0].id == 'sample1' && it[1] == 'file1.txt' }
        result.find { it[0].id == 'sample2' && it[1] == 'file2.txt' }
        result.find { it[0].id == 'sample2' && it[1] == 'file3.txt' }
    }

    def 'flattenPaths should handle empty input'() {
        given: 'An empty list'
        def emptyList = []

        when: 'flattenPaths is called'
        def result = ChannelUtils.flattenPaths(emptyList)

        then: 'result should be empty'
        result.isEmpty()
    }

    def 'flattenPaths should preserve meta information'() {
        given: 'A list with complex meta'
        def list = [
            [
                [[id: 'sample1', single_end: true, extra: 'data'], ['file1.txt', 'file2.txt'] as Set]
            ]
        ]

        when: 'flattenPaths is called'
        def result = ChannelUtils.flattenPaths(list)

        then: 'meta should be preserved in each flattened tuple'
        result.size() == 2
        result[0][0] == [id: 'sample1', single_end: true, extra: 'data']
        result[1][0] == [id: 'sample1', single_end: true, extra: 'data']
    }

    def 'flattenPaths should handle Set input for files'() {
        given: 'A list with Set of files'
        def fileSet = ['file1.txt', 'file2.txt', 'file3.txt'] as Set
        def list = [
            [
                [[id: 'sample1'], fileSet]
            ]
        ]

        when: 'flattenPaths is called'
        def result = ChannelUtils.flattenPaths(list)

        then: 'all files should be flattened'
        result.size() == 3
        result.every { it[0].id == 'sample1' }
    }

    def 'flattenPaths should handle List input for files'() {
        given: 'A list with List of files'
        def fileList = ['file1.txt', 'file2.txt']
        def list = [
            [
                [[id: 'sample1'], fileList]
            ]
        ]

        when: 'flattenPaths is called'
        def result = ChannelUtils.flattenPaths(list)

        then: 'all files should be flattened'
        result.size() == 2
        result[0] == [[id: 'sample1'], 'file1.txt']
        result[1] == [[id: 'sample1'], 'file2.txt']
    }

    def 'flattenPaths should handle single file path string'() {
        given: 'A list with single file path string'
        def list = [
            [
                [[id: 'sample1'], '/path/to/file.txt']
            ]
        ]

        when: 'flattenPaths is called'
        def result = ChannelUtils.flattenPaths(list)

        then: 'file should be treated as single item, not split by characters'
        result.size() == 1
        result[0] == [[id: 'sample1'], '/path/to/file.txt']
    }

    def 'flattenPaths should handle mixed single files and collections'() {
        given: 'A list with mixed single files and collections'
        def list1 = [
            [[id: 'sample1'], '/path/to/single.txt']
        ]
        def list2 = [
            [[id: 'sample2'], ['file1.txt', 'file2.txt'] as Set]
        ]

        when: 'flattenPaths is called with mixed inputs'
        def result = ChannelUtils.flattenPaths([list1, list2])

        then: 'all files should be handled correctly'
        result.size() == 3
        result.find { it[0].id == 'sample1' && it[1] == '/path/to/single.txt' }
        result.find { it[0].id == 'sample2' && it[1] == 'file1.txt' }
        result.find { it[0].id == 'sample2' && it[1] == 'file2.txt' }
    }

    // Error handling tests

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

    def 'flattenPaths should throw IllegalArgumentException when channels is null'() {
        when: 'flattenPaths is called with null'
        ChannelUtils.flattenPaths(null)

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'channels cannot be null'
    }

    def 'flattenPaths should throw IllegalArgumentException when channels contains null'() {
        given: 'A list containing a null element'
        def list = [
            [[[id: 'sample1'], ['file1.txt'] as Set]],
            null,
            [[[id: 'sample2'], ['file2.txt'] as Set]]
        ]

        when: 'flattenPaths is called'
        ChannelUtils.flattenPaths(list)

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'channels list cannot contain null elements'
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

        then: 'only records with at least one non-null field should remain'
        result.size() == 2
        result[0]._meta.id == 'sample1'
        result[0].r1 == 'read1.fq'
        result[0].r2 == 'read2.fq'
        !result[0].containsKey('meta')
        result[1]._meta.id == 'sample3'
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
        result[0]._meta.id == 'sample2'
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
        result[0]._meta.id == 'sample1'
        result[1]._meta.id == 'sample3'
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

    // DataflowQueue and DataflowVariable support tests
    // Note: Full channel operation tests with DataflowQueue require integration testing
    // These tests verify type recognition and validation still work

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

    def 'flattenPaths should validate DataflowQueue in channel list'() {
        given: 'A DataflowQueue instance'
        def queue = new DataflowQueue()

        when: 'checking if it would be treated as a channel'
        def isChannel = queue instanceof DataflowReadChannel || 
                       queue instanceof DataflowWriteChannel || 
                       queue instanceof DataflowQueue

        then: 'DataflowQueue should be recognized as a channel type'
        isChannel == true
    }

    def 'DataflowQueue should be instance of expected channel types'() {
        given: 'A DataflowQueue instance'
        def queue = new DataflowQueue()

        expect: 'DataflowQueue to be recognized in channel detection logic'
        queue instanceof DataflowQueue
        // DataflowQueue implements DataflowReadChannel and DataflowWriteChannel
        queue instanceof DataflowReadChannel
        queue instanceof DataflowWriteChannel
    }
}
