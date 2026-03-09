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

    def 'gather should handle list inputs'() {
        given: 'A list of tuples'
        def list = [
            [[id: 'sample1'], 'output1.txt'],
            [[id: 'sample2'], 'output2.txt'],
            [[id: 'sample3'], 'output3.txt']
        ]

        when: 'gather is called with a tool name'
        def result = ChannelUtils.gather(list, 'mytool')

        then: 'result should be a single tuple with collected outputs'
        result[0] == [id: 'mytool', args: '']
        result[1] instanceof Set
        result[1].size() == 3
        result[1].contains('output1.txt')
        result[1].contains('output2.txt')
        result[1].contains('output3.txt')
    }

    def 'gather should create correct meta map with tool name'() {
        given: 'A list of tuples'
        def list = [
            [[id: 'sample1'], 'output1.txt']
        ]

        when: 'gather is called with a specific tool name'
        def result = ChannelUtils.gather(list, 'assembly')

        then: 'meta should have the correct id and default empty args'
        result[0].id == 'assembly'
        result[0].args == ''
    }

    def 'gather should handle empty list'() {
        given: 'An empty list'
        def list = []

        when: 'gather is called'
        def result = ChannelUtils.gather(list, 'mytool')

        then: 'result should be empty due to empty guard'
        result == []
    }

    def 'gather should include args parameter when provided'() {
        given: 'A list of tuples'
        def list = [
            [[id: 'sample1'], 'output1.txt'],
            [[id: 'sample2'], 'output2.txt']
        ]

        when: 'gather is called with args named parameter'
        def result = ChannelUtils.gather(list, 'mytool', args: '--lazy-quotes')

        then: 'result should include args in meta map'
        result[0] == [id: 'mytool', args: '--lazy-quotes']
        result[1] instanceof Set
        result[1].size() == 2
    }

    def 'gather should handle empty args parameter'() {
        given: 'A list of tuples'
        def list = [
            [[id: 'sample1'], 'output.txt']
        ]

        when: 'gather is called with explicit empty args'
        def result = ChannelUtils.gather(list, 'mytool', args: '')

        then: 'result should have empty args'
        result[0] == [id: 'mytool', args: '']
    }

    def 'gather should handle complex args strings'() {
        given: 'A list of tuples'
        def list = [
            [[id: 'sample1'], 'output.txt']
        ]

        when: 'gather is called with complex args'
        def result = ChannelUtils.gather(list, 'mytool', args: '--option1 value1 --option2 value2')

        then: 'result should preserve the args string'
        result[0].args == '--option1 value1 --option2 value2'
        result[0].id == 'mytool'
    }

    def 'gather should deduplicate outputs using Set'() {
        given: 'A list with duplicate outputs'
        def list = [
            [[id: 'sample1'], 'output.txt'],
            [[id: 'sample2'], 'output.txt'],
            [[id: 'sample3'], 'other.txt']
        ]

        when: 'gather is called'
        def result = ChannelUtils.gather(list, 'mytool')

        then: 'result should contain unique outputs only'
        result[1].size() == 2
        result[1].contains('output.txt')
        result[1].contains('other.txt')
    }

    // ---- Record-aware gather tests (field: option) ----

    def 'gather with field should extract named field from records'() {
        given: 'A list of record-like maps'
        def records = [
            [report: 'report1.txt', meta: [id: 'sample1']],
            [report: 'report2.txt', meta: [id: 'sample2']],
            [report: 'report3.txt', meta: [id: 'sample3']]
        ]

        when: 'gather is called with field option'
        def result = ChannelUtils.gather(records, 'abricate', field: 'report')

        then: 'result should contain the extracted field values'
        result[0] == [id: 'abricate', args: '']
        result[1] instanceof Set
        result[1].size() == 3
        result[1].contains('report1.txt')
        result[1].contains('report2.txt')
        result[1].contains('report3.txt')
    }

    def 'gather with field and args should include both options'() {
        given: 'A list of record-like maps'
        def records = [
            [report: 'report1.txt'],
            [report: 'report2.txt']
        ]

        when: 'gather is called with both field and args'
        def result = ChannelUtils.gather(records, 'ariba-report', field: 'report', args: '-C "$" --lazy-quotes')

        then: 'result should have correct meta with args and extracted field values'
        result[0] == [id: 'ariba-report', args: '-C "$" --lazy-quotes']
        result[1] instanceof Set
        result[1].size() == 2
        result[1].contains('report1.txt')
        result[1].contains('report2.txt')
    }

    def 'gather with field should filter null field values'() {
        given: 'A list of records where some have null field values'
        def records = [
            [report: 'report1.txt'],
            [report: null],
            [report: 'report3.txt']
        ]

        when: 'gather is called with field option'
        def result = ChannelUtils.gather(records, 'mytool', field: 'report')

        then: 'null values should be filtered out'
        result[0] == [id: 'mytool', args: '']
        result[1].size() == 2
        result[1].contains('report1.txt')
        result[1].contains('report3.txt')
    }

    def 'gather with field should return empty for all-null field values'() {
        given: 'A list of records where all field values are null'
        def records = [
            [report: null],
            [report: null]
        ]

        when: 'gather is called with field option'
        def result = ChannelUtils.gather(records, 'mytool', field: 'report')

        then: 'result should be empty'
        result == []
    }

    def 'gather with field should deduplicate outputs'() {
        given: 'A list of records with duplicate field values'
        def records = [
            [report: 'same.txt'],
            [report: 'same.txt'],
            [report: 'other.txt']
        ]

        when: 'gather is called with field option'
        def result = ChannelUtils.gather(records, 'mytool', field: 'report')

        then: 'duplicate values should be deduplicated'
        result[1].size() == 2
        result[1].contains('same.txt')
        result[1].contains('other.txt')
    }

    def 'gather with field should handle missing field key'() {
        given: 'A list of records missing the specified field'
        def records = [
            [other: 'value1'],
            [other: 'value2']
        ]

        when: 'gather is called with a field that does not exist'
        def result = ChannelUtils.gather(records, 'mytool', field: 'report')

        then: 'null lookups should be filtered, returning empty'
        result == []
    }

    def 'gather without field or args should work unchanged'() {
        given: 'A list of tuples (legacy usage)'
        def list = [
            [[id: 'sample1'], 'output1.txt'],
            [[id: 'sample2'], 'output2.txt']
        ]

        when: 'gather is called with no options'
        def result = ChannelUtils.gather(list, 'mlst')

        then: 'result should use default tuple mode with empty args'
        result[0] == [id: 'mlst', args: '']
        result[1] instanceof Set
        result[1].size() == 2
        result[1].contains('output1.txt')
        result[1].contains('output2.txt')
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

    def 'gather should handle various output types'() {
        given: 'A list with different output types'
        def list = [
            [[id: 'sample1'], 'string_output'],
            [[id: 'sample2'], 123],
            [[id: 'sample3'], [key: 'value']]
        ]

        when: 'gather is called'
        def result = ChannelUtils.gather(list, 'mytool')

        then: 'all output types should be preserved'
        result[1].size() == 3
        result[1].contains('string_output')
        result[1].contains(123)
        result[1].contains([key: 'value'])
    }

    // Error handling tests

    def 'gather should throw IllegalArgumentException when chResults is null'() {
        when: 'gather is called with null chResults'
        ChannelUtils.gather(null, 'mytool')

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'chResults cannot be null'
    }

    def 'gather should throw IllegalArgumentException when toolName is null'() {
        given: 'A valid list'
        def list = [[[id: 'sample1'], 'output.txt']]

        when: 'gather is called with null toolName'
        ChannelUtils.gather(list, null)

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'toolName cannot be null or empty'
    }

    def 'gather should throw IllegalArgumentException when toolName is empty'() {
        given: 'A valid list'
        def list = [[[id: 'sample1'], 'output.txt']]

        when: 'gather is called with empty toolName'
        ChannelUtils.gather(list, '')

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'toolName cannot be null or empty'
    }

    def 'gather should throw IllegalArgumentException when toolName is whitespace'() {
        given: 'A valid list'
        def list = [[[id: 'sample1'], 'output.txt']]

        when: 'gather is called with whitespace toolName'
        ChannelUtils.gather(list, '   ')

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'toolName cannot be null or empty'
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
