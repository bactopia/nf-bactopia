package bactopia.plugin.utils

import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.DataflowVariable
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for SampleUtils class
 * 
 * Note: Channel-based tests are integration tests and should be run
 * in a full Nextflow pipeline context. These unit tests focus on
 * list-based operations.
 */
class SampleUtilsTest extends Specification {

    @Unroll
    def 'formatSamples should return #expected elements when dataTypes is #dataTypes'() {
        given: 'A list with 4-element tuples'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1'],
            [[id: 'sample2'], 'input2', 'extra2', 'extra2_2']
        ]

        when: 'formatSamples is called with specific dataTypes'
        def result = SampleUtils.formatSamples(samples, dataTypes)

        then: 'result should have correct tuple size'
        result.size() == 2
        result[0].size() == expected
        result[1].size() == expected

        where:
        dataTypes | expected
        1         | 2
        2         | 3
        3         | 4
    }

    def 'formatSamples with dataTypes=1 should return [meta, inputs]'() {
        given: 'A list with 4-element tuples'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1'],
            [[id: 'sample2'], 'input2', 'extra2', 'extra2_2']
        ]

        when: 'formatSamples is called with dataTypes=1'
        def result = SampleUtils.formatSamples(samples, 1)

        then: 'only meta and inputs should be returned'
        result[0] == [[id: 'sample1'], 'input1']
        result[1] == [[id: 'sample2'], 'input2']
    }

    def 'formatSamples with dataTypes=2 should return [meta, inputs, extra]'() {
        given: 'A list with 4-element tuples'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1'],
            [[id: 'sample2'], 'input2', 'extra2', 'extra2_2']
        ]

        when: 'formatSamples is called with dataTypes=2'
        def result = SampleUtils.formatSamples(samples, 2)

        then: 'meta, inputs, and extra should be returned'
        result[0] == [[id: 'sample1'], 'input1', 'extra1']
        result[1] == [[id: 'sample2'], 'input2', 'extra2']
    }

    def 'formatSamples with dataTypes=3 should return all 4 elements'() {
        given: 'A list with 4-element tuples'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1'],
            [[id: 'sample2'], 'input2', 'extra2', 'extra2_2']
        ]

        when: 'formatSamples is called with dataTypes=3'
        def result = SampleUtils.formatSamples(samples, 3)

        then: 'all elements should be returned unchanged'
        result[0] == [[id: 'sample1'], 'input1', 'extra1', 'extra2_1']
        result[1] == [[id: 'sample2'], 'input2', 'extra2', 'extra2_2']
    }

    @Unroll
    def 'formatSamples should throw exception for invalid dataTypes: #invalidValue'() {
        given: 'A list with tuples'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1']
        ]

        when: 'formatSamples is called with invalid dataTypes'
        SampleUtils.formatSamples(samples, invalidValue)

        then: 'IllegalArgumentException should be thrown'
        def exception = thrown(IllegalArgumentException)
        exception.message.contains('dataTypes must be 1, 2, or 3')

        where:
        invalidValue << [0, -1, 4, 5, 100]
    }

    def 'formatSamples should handle empty list'() {
        given: 'An empty list'
        def samples = []

        when: 'formatSamples is called'
        def result = SampleUtils.formatSamples(samples, 2)

        then: 'result should be empty'
        result.isEmpty()
    }

    // Note: Channel-based formatSamples() tests require a full Nextflow pipeline context
    // and should be tested as integration tests (e.g., test-format.nf)

    def 'formatSamples should preserve complex meta maps'() {
        given: 'A list with complex meta information'
        def samples = [
            [[id: 'sample1', single_end: true, strandedness: 'forward', depth: 30], 
             'input1', 'extra1', 'extra2_1']
        ]

        when: 'formatSamples is called with dataTypes=1'
        def result = SampleUtils.formatSamples(samples, 1)

        then: 'meta should be preserved'
        result[0][0] == [id: 'sample1', single_end: true, strandedness: 'forward', depth: 30]
        result[0][1] == 'input1'
    }

    def 'formatSamples should handle various data types in tuples'() {
        given: 'A list with different data types'
        def samples = [
            [[id: 'sample1'], ['file1.txt', 'file2.txt'], [1, 2, 3], null]
        ]

        when: 'formatSamples is called with dataTypes=2'
        def result = SampleUtils.formatSamples(samples, 2)

        then: 'data types should be preserved'
        result[0][0] == [id: 'sample1']
        result[0][1] == ['file1.txt', 'file2.txt']
        result[0][2] == [1, 2, 3]
    }

    def 'formatSamples should handle null values in extra fields'() {
        given: 'A list with null extra fields'
        def samples = [
            [[id: 'sample1'], 'input1', null, null]
        ]

        when: 'formatSamples is called with dataTypes=3'
        def result = SampleUtils.formatSamples(samples, 3)

        then: 'nulls should be preserved'
        result[0] == [[id: 'sample1'], 'input1', null, null]
    }

    def 'formatSamples should handle single sample'() {
        given: 'A list with single tuple'
        def samples = [
            [[id: 'only_sample'], 'input', 'extra', 'extra2']
        ]

        when: 'formatSamples is called with dataTypes=2'
        def result = SampleUtils.formatSamples(samples, 2)

        then: 'result should contain one properly formatted tuple'
        result.size() == 1
        result[0] == [[id: 'only_sample'], 'input', 'extra']
    }

    def 'formatSamples should handle large number of samples'() {
        given: 'A list with many tuples'
        def samples = (1..100).collect { i ->
            [[id: "sample${i}"], "input${i}", "extra${i}", "extra2_${i}"]
        }

        when: 'formatSamples is called with dataTypes=1'
        def result = SampleUtils.formatSamples(samples, 1)

        then: 'all samples should be processed correctly'
        result.size() == 100
        result.every { it.size() == 2 }
        result[0] == [[id: 'sample1'], 'input1']
        result[99] == [[id: 'sample100'], 'input100']
    }

    def 'formatSamples dataTypes=1 should discard extra fields'() {
        given: 'A list with important data in extra fields'
        def samples = [
            [[id: 'sample1'], 'keep_this', 'discard_this', 'discard_this_too']
        ]

        when: 'formatSamples is called with dataTypes=1'
        def result = SampleUtils.formatSamples(samples, 1)

        then: 'only first two elements should remain'
        result[0].size() == 2
        !result[0].contains('discard_this')
        !result[0].contains('discard_this_too')
    }

    def 'formatSamples dataTypes=2 should discard second extra field'() {
        given: 'A list with data in all fields'
        def samples = [
            [[id: 'sample1'], 'input', 'keep_extra', 'discard_extra2']
        ]

        when: 'formatSamples is called with dataTypes=2'
        def result = SampleUtils.formatSamples(samples, 2)

        then: 'only first three elements should remain'
        result[0].size() == 3
        result[0][2] == 'keep_extra'
        !result[0].contains('discard_extra2')
    }

    // Error handling tests

    def 'formatSamples should throw IllegalArgumentException when samples is null'() {
        when: 'formatSamples is called with null samples'
        SampleUtils.formatSamples(null, 1)

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'samples cannot be null'
    }

    def 'formatSamples should throw IllegalArgumentException when dataTypes is null'() {
        given: 'A valid samples list'
        def samples = [[[id: 'sample1'], 'input1', 'extra1', 'extra2_1']]

        when: 'formatSamples is called with null dataTypes'
        SampleUtils.formatSamples(samples, null)

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'dataTypes cannot be null'
    }

    // DataflowVariable support tests

    def 'formatSamples should handle DataflowVariable with value 1'() {
        given: 'A valid samples list'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1'],
            [[id: 'sample2'], 'input2', 'extra2', 'extra2_2']
        ]
        and: 'A DataflowVariable containing dataTypes value'
        def dataTypesVar = new DataflowVariable()
        dataTypesVar.bind(1)

        when: 'formatSamples is called with DataflowVariable'
        def result = SampleUtils.formatSamples(samples, dataTypesVar)

        then: 'result should have 2 elements per tuple'
        result.size() == 2
        result[0].size() == 2
        result[0] == [[id: 'sample1'], 'input1']
        result[1] == [[id: 'sample2'], 'input2']
    }

    def 'formatSamples should handle DataflowVariable with value 2'() {
        given: 'A valid samples list'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1']
        ]
        and: 'A DataflowVariable containing dataTypes value'
        def dataTypesVar = new DataflowVariable()
        dataTypesVar.bind(2)

        when: 'formatSamples is called with DataflowVariable'
        def result = SampleUtils.formatSamples(samples, dataTypesVar)

        then: 'result should have 3 elements per tuple'
        result.size() == 1
        result[0].size() == 3
        result[0] == [[id: 'sample1'], 'input1', 'extra1']
    }

    def 'formatSamples should handle DataflowVariable with value 3'() {
        given: 'A valid samples list'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1']
        ]
        and: 'A DataflowVariable containing dataTypes value'
        def dataTypesVar = new DataflowVariable()
        dataTypesVar.bind(3)

        when: 'formatSamples is called with DataflowVariable'
        def result = SampleUtils.formatSamples(samples, dataTypesVar)

        then: 'result should return all 4 elements unchanged'
        result.size() == 1
        result[0].size() == 4
        result[0] == [[id: 'sample1'], 'input1', 'extra1', 'extra2_1']
    }

    def 'formatSamples should throw IllegalArgumentException for DataflowVariable with invalid value'() {
        given: 'A valid samples list'
        def samples = [[[id: 'sample1'], 'input1', 'extra1', 'extra2_1']]
        and: 'A DataflowVariable containing invalid dataTypes value'
        def dataTypesVar = new DataflowVariable()
        dataTypesVar.bind(0)

        when: 'formatSamples is called with DataflowVariable containing 0'
        SampleUtils.formatSamples(samples, dataTypesVar)

        then: 'an IllegalArgumentException should be thrown'
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('dataTypes must be 1, 2, or 3')
        ex.message.contains('0')
    }

    def 'formatSamples should extract value from DataflowVariable and validate'() {
        given: 'A valid samples list'
        def samples = [[[id: 'sample1'], 'input1', 'extra1', 'extra2_1']]
        and: 'A DataflowVariable containing invalid dataTypes value'
        def dataTypesVar = new DataflowVariable()
        dataTypesVar.bind(5)

        when: 'formatSamples is called with DataflowVariable containing 5'
        SampleUtils.formatSamples(samples, dataTypesVar)

        then: 'an IllegalArgumentException should be thrown with correct value in message'
        def ex = thrown(IllegalArgumentException)
        ex.message == 'dataTypes must be 1, 2, or 3 (received: 5)'
    }
}
