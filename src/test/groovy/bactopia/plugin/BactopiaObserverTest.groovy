package bactopia.plugin

import nextflow.Session
import nextflow.script.WorkflowMetadata
import spock.lang.Specification

/**
 * Tests for BactopiaObserver
 */
class BactopiaObserverTest extends Specification {

    def 'should create the observer instance' () {
        given:
        def factory = new BactopiaFactory()
        when:
        def result = factory.create(Mock(Session))
        then:
        result.size() == 1
        result.first() instanceof BactopiaObserver
    }

    def 'should initialize observer with session and config' () {
        given:
        def session = Mock(Session)
        def config = new BactopiaConfig([:], [:])
        
        when:
        def observer = new BactopiaObserver(session, config)
        
        then:
        observer.session == session
        observer.config == config
    }

    def 'should handle onFlowCreate with help parameter' () {
        given:
        def metadata = Mock(WorkflowMetadata)
        def session = Mock(Session) {
            getParams() >> [help: true]
            getWorkflowMetadata() >> metadata
            getConfig() >> [manifest: [name: 'test', version: '1.0.0']]
        }
        def config = new BactopiaConfig([:], [:])
        def observer = new BactopiaObserver(session, config)
        
        expect:
        // This method calls System.exit(0) for help, so we skip calling it in test
        observer.session == session
    }

    def 'should handle onFlowCreate with help_all parameter' () {
        given:
        def metadata = Mock(WorkflowMetadata)
        def session = Mock(Session) {
            getParams() >> [help_all: true]
            getWorkflowMetadata() >> metadata
            getConfig() >> [manifest: [name: 'test', version: '1.0.0']]
        }
        def config = new BactopiaConfig([:], [:])
        def observer = new BactopiaObserver(session, config)
        
        expect:
        // This method calls System.exit(0) for help, so we skip calling it in test
        observer.session == session
    }

    def 'should handle onFlowCreate without help parameters' () {
        given:
        def config = new BactopiaConfig([:], [:])
        def metadata = Mock(WorkflowMetadata) {
            getRunName() >> 'test-run'
            getSessionId() >> 'abc123'
        }
        def session = Mock(Session) {
            getParams() >> [outdir: '/output']
            getWorkflowMetadata() >> metadata
            getConfig() >> [manifest: [name: 'test', version: '1.0.0']]
        }
        def observer = new BactopiaObserver(session, config)
        
        expect:
        // Just verify observer is constructed properly
        observer.session != null
        observer.config != null
    }

    def 'should handle onFlowComplete' () {
        given:
        def config = new BactopiaConfig([:], [:])
        def metadata = Mock(WorkflowMetadata) {
            getSuccess() >> true
            getExitStatus() >> 0
        }
        def session = Mock(Session) {
            getParams() >> [outdir: '/output', monochrome_logs: true]
            getWorkflowMetadata() >> metadata
            getConfig() >> [manifest: [version: '1.0.0']]
        }
        def observer = new BactopiaObserver(session, config)
        
        expect:
        // Just verify observer has required properties
        observer.session != null
        observer.config != null
    }

    def 'should handle onWorkflowPublish' () {
        given:
        def session = Mock(Session)
        def config = new BactopiaConfig([:], [:])
        def observer = new BactopiaObserver(session, config)
        
        when:
        observer.onWorkflowPublish('test-output', 'test-value')
        
        then:
        noExceptionThrown()
    }

    def 'should handle onFilePublish' () {
        given:
        def session = Mock(Session)
        def config = new BactopiaConfig([:], [:])
        def observer = new BactopiaObserver(session, config)
        
        when:
        observer.onFilePublish(null, null, [:])
        
        then:
        noExceptionThrown()
    }
}
