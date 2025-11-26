package bactopia.plugin

import nextflow.util.Duration
import nextflow.util.MemoryUnit
import spock.lang.Specification

/**
 * Test class for BactopiaSchema cleanParameters method
 * Tests parameter cleaning and type conversion functionality
 * 
 * Note: validateBactopiaToolParams() and validateBactopiaParams() are complex validation
 * methods with many external dependencies that are better suited for integration testing.
 * This test focuses on the cleanParameters() method which handles type conversions.
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

        then: "result is a new instance"
        !result.is(params)
        result == params
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
}
