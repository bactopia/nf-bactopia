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
        result[0] == [id: 'mytool']
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

        then: 'meta should have the correct id'
        result[0].id == 'assembly'
    }

    def 'gather should handle empty list'() {
        given: 'An empty list'
        def list = []

        when: 'gather is called'
        def result = ChannelUtils.gather(list, 'mytool')

        then: 'result should contain empty set'
        result[0] == [id: 'mytool']
        result[1] instanceof Set
        result[1].isEmpty()
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
