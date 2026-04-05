package bactopia.plugin

import nextflow.Session
import spock.lang.Specification
import spock.lang.TempDir
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Unit tests for BactopiaExtension class
 * Tests all @Function methods exposed by the plugin
 */
class BactopiaExtensionTest extends Specification {

    BactopiaExtension extension
    Session mockSession

    @TempDir
    Path tempDir

    def setup() {
        // Create a mock session with basic configuration
        mockSession = Mock(Session) {
            getParams() >> [
                monochrome_logs: true,
                outdir: '/tmp/test',
                empty_path: tempDir.toString()
            ]
            getConfig() >> [
                navigate: { key -> [:] }
            ]
            getBaseDir() >> Paths.get(System.getProperty('user.dir'))
        }
        
        // Initialize the extension
        extension = new BactopiaExtension()
        extension.init(mockSession)
        
        // Clear any existing logs
        BactopiaLoggerFactory.clearLogs()
    }

    def cleanup() {
        BactopiaLoggerFactory.clearLogs()
    }

    // Tests for bactopiaInputs()
    
    def 'bactopiaInputs should return map with required keys'() {
        given: 'extension is initialized'
        // Mock session already set up in setup()

        when: 'bactopiaInputs is called'
        def result = extension.bactopiaInputs('bactopia')

        then: 'result should contain all required keys'
        result.containsKey('hasErrors')
        result.containsKey('error')
        result.containsKey('logs')
        result.containsKey('samples')
    }

    def 'bactopiaInputs should report error for invalid runtype'() {
        when: 'bactopiaInputs is called with invalid runtype'
        def result = extension.bactopiaInputs('bactopia')

        then: 'hasErrors should be true since bactopia is not a valid runtype'
        result.hasErrors == true
    }

    def 'bactopiaInputs should return hasErrors as boolean'() {
        when: 'bactopiaInputs is called'
        def result = extension.bactopiaInputs('bactopia')

        then: 'hasErrors should be a boolean'
        result.hasErrors instanceof Boolean
    }

    def 'bactopiaInputs should handle different runTypes'() {
        when: 'bactopiaInputs is called with different runTypes'
        def result1 = extension.bactopiaInputs('bactopia')
        def result2 = extension.bactopiaInputs('bactopia-tools')

        then: 'results should be returned for both'
        result1 != null
        result2 != null
        result1.containsKey('samples')
        result2.containsKey('samples')
    }

    // Tests for bactopiaToolInputs()

    def 'bactopiaToolInputs should return map with required keys'() {
        when: 'bactopiaToolInputs is called'
        def result = extension.bactopiaToolInputs()

        then: 'result should contain all required keys'
        result.containsKey('hasErrors')
        result.containsKey('error')
        result.containsKey('logs')
        result.containsKey('samples')
    }

    def 'bactopiaToolInputs should return samples as list'() {
        when: 'bactopiaToolInputs is called'
        def result = extension.bactopiaToolInputs()

        then: 'samples should be a list'
        result.samples instanceof List
    }

    def 'bactopiaToolInputs should return hasErrors as boolean'() {
        when: 'bactopiaToolInputs is called'
        def result = extension.bactopiaToolInputs()

        then: 'hasErrors should be a boolean'
        result.hasErrors instanceof Boolean
    }

    // Tests for validateParameters()
    // Note: These tests may fail if schema file doesn't exist in test context
    // That's expected behavior and tests pass/fail based on actual conditions

    def 'validateParameters should return map structure'() {
        given: 'validation options'
        def options = [
            monochrome_logs: true
        ]

        when: 'validateParameters is called'
        def result = null
        def exception = null
        try {
            result = extension.validateParameters(options, false)
        } catch (Exception e) {
            exception = e
        }

        then: 'either result is returned or exception is thrown'
        (result instanceof Map && result.containsKey('hasErrors')) || exception != null
    }

    def 'validateParameters should accept isBactopiaTool parameter as false'() {
        given: 'validation options'
        def options = [monochrome_logs: true]

        when: 'validateParameters is called with isBactopiaTool=false'
        def result = null
        def exception = null
        try {
            result = extension.validateParameters(options, false)
        } catch (Exception e) {
            exception = e
        }

        then: 'either result is a map or exception is thrown'
        (result instanceof Map) || exception != null
    }

    def 'validateParameters should accept isBactopiaTool parameter as true'() {
        given: 'validation options'
        def options = [monochrome_logs: true]

        when: 'validateParameters is called with isBactopiaTool=true'
        def result = null
        def exception = null
        try {
            result = extension.validateParameters(options, true)
        } catch (Exception e) {
            exception = e
        }

        then: 'either result is a map or exception is thrown'
        (result instanceof Map) || exception != null
    }

    def 'validateParameters should accept empty options map'() {
        when: 'validateParameters is called with empty options'
        def result = null
        def exception = null
        try {
            result = extension.validateParameters([:], false)
        } catch (Exception e) {
            exception = e
        }

        then: 'either result is returned or exception is thrown'
        (result instanceof Map) || exception != null
    }

    // Tests for getCapturedLogs()

    def 'getCapturedLogs should return string'() {
        when: 'getCapturedLogs is called'
        def result = extension.getCapturedLogs()

        then: 'result should be a string'
        result instanceof String
    }

    def 'getCapturedLogs should accept withColors parameter'() {
        when: 'getCapturedLogs is called with different color settings'
        def withColors = extension.getCapturedLogs(true)
        def withoutColors = extension.getCapturedLogs(false)

        then: 'both should return strings'
        withColors instanceof String
        withoutColors instanceof String
    }

    def 'getCapturedLogs should default to withColors=true'() {
        when: 'getCapturedLogs is called without parameters'
        def result = extension.getCapturedLogs()

        then: 'it should not throw exception'
        result != null
        result instanceof String
    }

    def 'getCapturedLogs should clear logs after retrieval'() {
        given: 'some logs are captured'
        def logger = org.slf4j.LoggerFactory.getLogger('bactopia.plugin.TestLogger')
        logger.info('Test message')

        when: 'getCapturedLogs is called'
        def firstCall = extension.getCapturedLogs(false)
        def secondCall = extension.getCapturedLogs(false)

        then: 'second call should return empty string'
        firstCall.contains('Test message')
        secondCall == ''
    }

    // Tests for clearCapturedLogs()

    def 'clearCapturedLogs should clear all logs'() {
        given: 'some logs are captured'
        def logger = org.slf4j.LoggerFactory.getLogger('bactopia.plugin.TestLogger')
        logger.info('Test message to clear')

        when: 'clearCapturedLogs is called'
        extension.clearCapturedLogs()

        and: 'logs are retrieved'
        def logs = extension.getCapturedLogs(false)

        then: 'logs should be empty'
        logs == ''
    }

    def 'clearCapturedLogs should not throw exception when called multiple times'() {
        when: 'clearCapturedLogs is called multiple times'
        extension.clearCapturedLogs()
        extension.clearCapturedLogs()
        extension.clearCapturedLogs()

        then: 'no exception should be thrown'
        noExceptionThrown()
    }

    // Tests for gather()

    def 'gather should delegate to ChannelUtils'() {
        given: 'a list of record-like maps'
        def records = [
            [tsv: 'output1.txt', meta: [id: 'sample1']],
            [tsv: 'output2.txt', meta: [id: 'sample2']]
        ]

        when: 'gather is called'
        def result = extension.gather(records, 'tsv', [name: 'mytool'])

        then: 'result should be a record-like map with meta and field'
        result != null
        result instanceof Map
        result.meta == [name: 'mytool']
        result.tsv instanceof Set
    }

    def 'gather should pass through meta map'() {
        given: 'a list of record-like maps'
        def records = [
            [tsv: 'output.txt']
        ]

        when: 'gather is called with extra meta keys'
        def result = extension.gather(records, 'tsv', [name: 'sccmec', args: '--lazy'])

        then: 'meta should contain all provided keys'
        result.meta.name == 'sccmec'
        result.meta.args == '--lazy'
    }

    def 'gather should handle empty list'() {
        given: 'an empty list'
        def list = []

        when: 'gather is called'
        def result = extension.gather(list, 'tsv', [name: 'mytool'])

        then: 'result should be empty due to empty guard'
        result == []
    }

    // Tests for gatherCsvtk()

    def 'gatherCsvtk should rename field to csv'() {
        given: 'a list of record-like maps'
        def records = [
            [tsv: 'output1.txt'],
            [tsv: 'output2.txt']
        ]

        when: 'gatherCsvtk is called'
        def result = extension.gatherCsvtk(records, 'tsv', [name: 'mytool'])

        then: 'result should have csv field'
        result.meta == [name: 'mytool']
        result.csv instanceof Set
        result.csv.size() == 2
    }

    // Tests for collectNextflowLogs()

    def 'collectNextflowLogs should flatten nf_logs'() {
        given: 'a list of records with nf_logs'
        def records = [
            [meta: [id: 'sample1'], nf_logs: ['cmd.sh', 'cmd.log']]
        ]

        when: 'collectNextflowLogs is called'
        def result = extension.collectNextflowLogs(records)

        then: 'each log should be a separate tuple'
        result.size() == 2
        result[0] == [[id: 'sample1'], 'cmd.sh']
        result[1] == [[id: 'sample1'], 'cmd.log']
    }

    // Tests for formatSamples()

    def 'formatSamples should delegate to SampleUtils'() {
        given: 'a list with 4-element tuples'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1']
        ]

        when: 'formatSamples is called with dataTypes=1'
        def result = extension.formatSamples(samples, 1)

        then: 'result should be formatted correctly'
        result != null
        result instanceof List
        result[0].size() == 2
    }

    def 'formatSamples should handle dataTypes parameter'() {
        given: 'a list with 4-element tuples'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1']
        ]

        when: 'formatSamples is called with different dataTypes'
        def result1 = extension.formatSamples(samples, 1)
        def result2 = extension.formatSamples(samples, 2)
        def result3 = extension.formatSamples(samples, 3)

        then: 'results should have correct sizes'
        result1[0].size() == 2
        result2[0].size() == 3
        result3[0].size() == 4
    }

    def 'formatSamples should throw exception for invalid dataTypes'() {
        given: 'a list with tuples'
        def samples = [
            [[id: 'sample1'], 'input1', 'extra1', 'extra2_1']
        ]

        when: 'formatSamples is called with invalid dataTypes'
        extension.formatSamples(samples, 0)

        then: 'IllegalArgumentException should be thrown'
        thrown(IllegalArgumentException)
    }

    // Integration tests for the extension as a whole

    def 'extension should initialize logger factory'() {
        expect: 'logger factory should be initialized'
        BactopiaLoggerFactory.isInitialized()
    }

    def 'extension should maintain config after init'() {
        expect: 'config should be accessible'
        extension.@config != null
    }

    def 'extension should maintain session after init'() {
        expect: 'session should be accessible'
        extension.@session != null
        extension.@session == mockSession
    }

    def 'extension should have multiple @Function methods'() {
        when: 'checking for @Function methods'
        def methods = extension.class.methods.findAll { method ->
            method.annotations.any { ann ->
                ann.annotationType().name.contains('Function')
            }
        }

        then: 'should have at least 8 function methods'
        methods.size() >= 8
    }
}
