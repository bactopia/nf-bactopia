package bactopia.plugin

import spock.lang.Specification

/**
 * Tests for BactopiaConfig
 */
class BactopiaConfigTest extends Specification {

    def 'should create config with default values' () {
        when:
        def config = new BactopiaConfig()
        
        then:
        config.monochromeLogs == false
        config.parametersSchema == "nextflow_schema.json"
        config.ignoreParams.isEmpty()
    }

    def 'should create config with custom monochromeLogs' () {
        given:
        def configMap = [monochromeLogs: true]
        
        when:
        def config = new BactopiaConfig(configMap, [:])
        
        then:
        config.monochromeLogs == true
    }

    def 'should create config with custom parametersSchema' () {
        given:
        def configMap = [parametersSchema: "custom_schema.json"]
        
        when:
        def config = new BactopiaConfig(configMap, [:])
        
        then:
        config.parametersSchema == "custom_schema.json"
    }

    def 'should create config with custom ignoreParams' () {
        given:
        def configMap = [ignoreParams: ["param1", "param2"]]
        
        when:
        def config = new BactopiaConfig(configMap, [:])
        
        then:
        config.ignoreParams.contains("param1")
        config.ignoreParams.contains("param2")
    }



    def 'should create config with all custom values' () {
        given:
        def configMap = [
            monochromeLogs: true,
            parametersSchema: "my_schema.json",
            ignoreParams: ["skip1", "skip2", "skip3"]
        ]
        
        when:
        def config = new BactopiaConfig(configMap, [:])
        
        then:
        config.monochromeLogs == true
        config.parametersSchema == "my_schema.json"
        config.ignoreParams.size() == 3
        config.ignoreParams.containsAll(["skip1", "skip2", "skip3"])
    }

    def 'should handle null config map' () {
        when:
        def config = new BactopiaConfig(null, [:])
        
        then:
        config.monochromeLogs == false
        config.parametersSchema == "nextflow_schema.json"
        config.ignoreParams.isEmpty()
    }

    def 'should handle empty config map' () {
        when:
        def config = new BactopiaConfig([:], [:])
        
        then:
        config.monochromeLogs == false
        config.parametersSchema == "nextflow_schema.json"
        config.ignoreParams.isEmpty()
    }
}
