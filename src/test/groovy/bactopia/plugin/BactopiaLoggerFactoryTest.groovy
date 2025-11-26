package bactopia.plugin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

/**
 * Unit tests for BactopiaLoggerFactory class
 */
class BactopiaLoggerFactoryTest extends Specification {

    def setup() {
        // Clear logs before each test
        BactopiaLoggerFactory.clearLogs()
    }

    def 'initialize should set up global log capturing'() {
        when: 'initialize is called'
        BactopiaLoggerFactory.initialize(false)

        then: 'factory should be marked as initialized'
        BactopiaLoggerFactory.isInitialized()
    }

    def 'initialize should be idempotent'() {
        when: 'initialize is called multiple times'
        BactopiaLoggerFactory.initialize(false)
        BactopiaLoggerFactory.initialize(false)
        BactopiaLoggerFactory.initialize(false)

        then: 'no exception should be thrown'
        BactopiaLoggerFactory.isInitialized()
    }

    def 'clearLogs should remove all captured logs'() {
        given: 'some logs are captured'
        BactopiaLoggerFactory.initialize(false)
        def logger = LoggerFactory.getLogger('bactopia.plugin.TestLogger')
        logger.info('Test message 1')
        logger.info('Test message 2')

        when: 'clearLogs is called'
        BactopiaLoggerFactory.clearLogs()

        then: 'captured logs should be empty'
        def logs = BactopiaLoggerFactory.getCapturedLogs()
        logs.isEmpty()
    }

    def 'getCapturedLogs should return list of log messages'() {
        given: 'logger is initialized'
        BactopiaLoggerFactory.initialize(false)
        def logger = LoggerFactory.getLogger('bactopia.plugin.TestLogger')

        when: 'log messages are created'
        logger.info('Info message')
        logger.warn('Warning message')
        logger.error('Error message')

        then: 'captured logs should contain the messages'
        def logs = BactopiaLoggerFactory.getCapturedLogs(false)
        logs.size() >= 3
        logs.any { it.contains('Info message') }
        logs.any { it.contains('Warning message') }
        logs.any { it.contains('Error message') }
    }

    def 'getCapturedLogsAsString should return logs as single string'() {
        given: 'logger is initialized and logs are created'
        BactopiaLoggerFactory.initialize(false)
        def logger = LoggerFactory.getLogger('bactopia.plugin.TestLogger')
        logger.info('First log')
        logger.info('Second log')

        when: 'getCapturedLogsAsString is called'
        def result = BactopiaLoggerFactory.getCapturedLogsAsString(false)

        then: 'result should be a single string with newlines'
        result instanceof String
        result.contains('First log')
        result.contains('Second log')
        result.contains('\n')
    }

    def 'getLogsFromLogger should filter by logger name'() {
        given: 'logger is initialized'
        BactopiaLoggerFactory.initialize(false)
        def logger1 = LoggerFactory.getLogger('bactopia.plugin.Logger1')
        def logger2 = LoggerFactory.getLogger('bactopia.plugin.Logger2')

        when: 'different loggers create messages'
        logger1.info('Logger1 message')
        logger2.info('Logger2 message')

        and: 'logs are filtered by logger name'
        def logs1 = BactopiaLoggerFactory.getLogsFromLogger('Logger1', false)
        def logs2 = BactopiaLoggerFactory.getLogsFromLogger('Logger2', false)

        then: 'each filter should return only matching logs'
        logs1.size() >= 1
        logs1.any { it.contains('Logger1 message') }
        !logs1.any { it.contains('Logger2 message') }
        
        logs2.size() >= 1
        logs2.any { it.contains('Logger2 message') }
        !logs2.any { it.contains('Logger1 message') }
    }

    def 'getLogsByLevel should filter by log level'() {
        given: 'logger is initialized'
        BactopiaLoggerFactory.initialize(false)
        def logger = LoggerFactory.getLogger('bactopia.plugin.TestLogger')

        when: 'different level logs are created'
        logger.info('Info message')
        logger.warn('Warning message')
        logger.error('Error message')
        logger.debug('Debug message')

        then: 'getLogsByLevel should filter correctly'
        def errorLogs = BactopiaLoggerFactory.getLogsByLevel('ERROR')
        def warnLogs = BactopiaLoggerFactory.getLogsByLevel('WARN')
        def infoLogs = BactopiaLoggerFactory.getLogsByLevel('INFO')

        errorLogs.size() >= 1
        errorLogs.every { it.level == Level.ERROR }
        
        warnLogs.size() >= 1
        warnLogs.every { it.level == Level.WARN }
        
        infoLogs.size() >= 1
        infoLogs.every { it.level == Level.INFO }
    }

    def 'hasErrors should return true when error logs exist'() {
        given: 'logger is initialized'
        BactopiaLoggerFactory.initialize(false)
        BactopiaLoggerFactory.clearLogs()
        def logger = LoggerFactory.getLogger('bactopia.plugin.TestLogger')

        when: 'only info logs exist'
        logger.info('Info message')

        then: 'hasErrors should return false'
        !BactopiaLoggerFactory.hasErrors()

        when: 'error log is added'
        logger.error('Error message')

        then: 'hasErrors should return true'
        BactopiaLoggerFactory.hasErrors()
    }

    def 'captureAndClearLogs should return map with logs and clear them'() {
        given: 'logger is initialized with logs'
        BactopiaLoggerFactory.initialize(false)
        BactopiaLoggerFactory.clearLogs()
        def logger = LoggerFactory.getLogger('bactopia.plugin.TestLogger')
        logger.info('Test message')

        when: 'captureAndClearLogs is called'
        def result = BactopiaLoggerFactory.captureAndClearLogs('test data', false)

        then: 'result should contain expected keys'
        result.data == 'test data'
        result.logs instanceof String
        result.logs.contains('Test message')
        result.hasErrors instanceof Boolean
        result.error instanceof String

        and: 'logs should be cleared'
        BactopiaLoggerFactory.getCapturedLogs().isEmpty()
    }

    def 'captureAndClearLogs should set error field when errors exist'() {
        given: 'logger is initialized with error log'
        BactopiaLoggerFactory.initialize(false)
        BactopiaLoggerFactory.clearLogs()
        def logger = LoggerFactory.getLogger('bactopia.plugin.TestLogger')
        logger.error('Error message')

        when: 'captureAndClearLogs is called'
        def result = BactopiaLoggerFactory.captureAndClearLogs('', false)

        then: 'hasErrors should be true and error field populated'
        result.hasErrors == true
        result.error != ''
        result.error.contains('Error message')
    }

    def 'captureAndClearLogs should not set error field when no errors'() {
        given: 'logger is initialized with info log only'
        BactopiaLoggerFactory.initialize(false)
        BactopiaLoggerFactory.clearLogs()
        def logger = LoggerFactory.getLogger('bactopia.plugin.TestLogger')
        logger.info('Info message')

        when: 'captureAndClearLogs is called'
        def result = BactopiaLoggerFactory.captureAndClearLogs('', false)

        then: 'hasErrors should be false and error field empty'
        result.hasErrors == false
        result.error == ''
    }

    def 'logger should only capture logs from bactopia.plugin package'() {
        given: 'logger is initialized'
        BactopiaLoggerFactory.initialize(false)
        BactopiaLoggerFactory.clearLogs()
        def bactopiaLogger = LoggerFactory.getLogger('bactopia.plugin.MyClass')
        def otherLogger = LoggerFactory.getLogger('some.other.package.MyClass')

        when: 'logs are created from different packages'
        bactopiaLogger.info('Bactopia log')
        otherLogger.info('Other package log')

        then: 'only bactopia logs should be captured'
        def logs = BactopiaLoggerFactory.getCapturedLogs(false)
        logs.any { it.contains('Bactopia log') }
        // Other package logs should not be captured
    }

    def 'monochrome mode should affect color output'() {
        given: 'logger is initialized in monochrome mode'
        BactopiaLoggerFactory.initialize(true)
        BactopiaLoggerFactory.clearLogs()
        def logger = LoggerFactory.getLogger('bactopia.plugin.TestLogger')

        when: 'colored output is requested for error'
        logger.error('Error message')
        def logsWithColors = BactopiaLoggerFactory.getCapturedLogs(true)

        then: 'logs should not contain ANSI codes in monochrome mode'
        logsWithColors.every { !it.contains('\033[') || it.contains('Error message') }
    }

    def 'getCapturedLogs should preserve log message order'() {
        given: 'logger is initialized'
        BactopiaLoggerFactory.initialize(false)
        BactopiaLoggerFactory.clearLogs()
        def logger = LoggerFactory.getLogger('bactopia.plugin.TestLogger')

        when: 'multiple logs are created in order'
        logger.info('First')
        logger.info('Second')
        logger.info('Third')

        then: 'logs should maintain order'
        def logs = BactopiaLoggerFactory.getCapturedLogsAsString(false)
        def firstIndex = logs.indexOf('First')
        def secondIndex = logs.indexOf('Second')
        def thirdIndex = logs.indexOf('Third')
        
        firstIndex < secondIndex
        secondIndex < thirdIndex
    }
}
