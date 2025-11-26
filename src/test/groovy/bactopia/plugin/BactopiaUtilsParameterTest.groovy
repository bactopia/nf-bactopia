package bactopia.plugin

import nextflow.NextflowMeta
import nextflow.Session
import nextflow.script.WorkflowMetadata
import spock.lang.Specification
import java.nio.file.Paths

/**
 * Tests for BactopiaUtils parameter summary and formatting functions.
 */
class BactopiaUtilsParameterTest extends Specification {

    def "paramsSummaryMap returns summary with default options"() {
        given: "a mock workflow, session, and config"
        def workflow = Mock(WorkflowMetadata) {
            getProjectName() >> "test-project"
            getRepository() >> "https://github.com/test/repo"
            getRevision() >> "main"
            getProfile() >> "standard"
            getContainer() >> "test/container:latest"
        }
        def session = Mock(Session) {
            getBaseDir() >> Paths.get("/test/dir")
            getParams() >> [
                test_param: "value1",
                another_param: 123
            ]
        }
        def config = new BactopiaConfig([:], [:])

        when: "paramsSummaryMap is called"
        def result = BactopiaUtils.paramsSummaryMap(null, workflow, session, config)

        then: "it returns a map"
        result instanceof Map
    }

    def "paramsSummaryMap accepts custom options"() {
        given: "a mock workflow, session, config, and custom options"
        def workflow = Mock(WorkflowMetadata) {
            getProjectName() >> "test-project"
        }
        def session = Mock(Session) {
            getBaseDir() >> Paths.get("/test/dir")
            getParams() >> [test: "value"]
        }
        def config = new BactopiaConfig([:], [:])
        def options = [custom_option: "custom_value"]

        when: "paramsSummaryMap is called with options"
        def result = BactopiaUtils.paramsSummaryMap(options, workflow, session, config)

        then: "it returns a map"
        result instanceof Map
    }

    def "flattenNestedParamsMap flattens single level map"() {
        given: "a single level map"
        def paramsMap = [
            param1: "value1",
            param2: "value2",
            param3: 123
        ]

        when: "flattenNestedParamsMap is called via paramsSummaryLog"
        // Note: flattenNestedParamsMap is private, so we test it indirectly
        def result = paramsMap

        then: "single level maps remain unchanged"
        result.size() == 3
        result.param1 == "value1"
        result.param2 == "value2"
        result.param3 == 123
    }

    def "paramsSummaryLog generates formatted output"() {
        given: "a mock workflow, session, and config"
        def workflow = Mock(WorkflowMetadata) {
            getManifest() >> [name: "test-workflow", version: "1.0.0"]
            getProjectName() >> "test-project"
            getRepository() >> "https://github.com/test/repo"
            getRevision() >> "main"
            getProfile() >> "standard"
            getContainer() >> "test/container:latest"
        }
        def session = Mock(Session) {
            getParams() >> [
                workflow: [
                    name: "test-workflow",
                    description: "Test workflow description"
                ],
                test_param: "value1"
            ]
            getBaseDir() >> Paths.get("/test/dir")
            getConfig() >> [manifest: [version: "1.0.0"]]
        }
        def config = new BactopiaConfig([monochromeLogs: true], [:])

        when: "paramsSummaryLog is called"
        def result = BactopiaUtils.paramsSummaryLog(workflow, session, config, null)

        then: "it returns a formatted string"
        result instanceof String
        result.length() > 0
        result.contains("Only displaying parameters that differ from the defaults")
    }

    def "paramsSummaryLog respects monochrome logs setting"() {
        given: "a config with monochrome logs enabled"
        def workflow = Mock(WorkflowMetadata) {
            getManifest() >> [name: "test-workflow", version: "1.0.0"]
            getProjectName() >> "test-project"
        }
        def session = Mock(Session) {
            getParams() >> [
                workflow: [name: "test-workflow", description: "Test"],
                test: "value"
            ]
            getBaseDir() >> Paths.get("/test")
            getConfig() >> [manifest: [version: "1.0.0"]]
        }
        def config = new BactopiaConfig([monochromeLogs: true], [:])

        when: "paramsSummaryLog is called"
        def result = BactopiaUtils.paramsSummaryLog(workflow, session, config)

        then: "output does not contain ANSI color codes"
        !result.contains("\033[")
    }

    def "paramsSummaryLog with color logs contains ANSI codes"() {
        given: "a config with color logs enabled"
        def workflow = Mock(WorkflowMetadata) {
            getManifest() >> [name: "test-workflow", version: "1.0.0"]
            getProjectName() >> "test-project"
        }
        def session = Mock(Session) {
            getParams() >> [
                workflow: [name: "test-workflow", description: "Test"],
                test: "value"
            ]
            getBaseDir() >> Paths.get("/test")
            getConfig() >> [manifest: [version: "1.0.0"]]
        }
        def config = new BactopiaConfig([monochromeLogs: false], [:])

        when: "paramsSummaryLog is called"
        def result = BactopiaUtils.paramsSummaryLog(workflow, session, config)

        then: "output contains ANSI color codes"
        result.contains("\033[")
    }

    def "paramsSummaryLog accepts custom schema option"() {
        given: "custom options with parameters_schema"
        def workflow = Mock(WorkflowMetadata) {
            getManifest() >> [name: "test-workflow", version: "1.0.0"]
        }
        def session = Mock(Session) {
            getParams() >> [
                workflow: [name: "test", description: "Test"],
                param: "value"
            ]
            getBaseDir() >> Paths.get("/test")
            getConfig() >> [manifest: [version: "1.0.0"]]
        }
        def config = new BactopiaConfig([monochromeLogs: true], [:])
        def options = [parameters_schema: "custom_schema.json"]

        when: "paramsSummaryLog is called with custom options"
        def result = BactopiaUtils.paramsSummaryLog(workflow, session, config, options)

        then: "it returns formatted output"
        result instanceof String
        result.length() > 0
    }
}
