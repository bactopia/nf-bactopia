package bactopia.plugin

import nextflow.script.WorkflowMetadata
import spock.lang.Specification

/**
 * Test class for BactopiaTemplate getLogo() function
 * Tests logo rendering for different workflow types
 * 
 * Note: getWorkflowSummary() tests were attempted but removed due to complex
 * Nextflow internal type mocking requirements (OffsetDateTime vs Date conversions).
 * Similar to BactopiaUtilsParameterTest, some functions are better suited for
 * integration testing rather than unit testing with mocks.
 */
class BactopiaTemplateRenderingTest extends Specification {

    def "getLogo returns bactopia logo with colors"() {
        given: "bactopia workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'bactopia', version: '3.0.0']
        }

        when: "generating logo with colors"
        def result = BactopiaTemplate.getLogo(mockMetadata, false, 'bactopia', 'A comprehensive bacterial genomics workflow')

        then: "returns bactopia logo with ANSI color codes"
        result.contains("bactopia")
        result.contains("3.0.0")
        result.contains("\033[")  // ANSI color code
    }

    def "getLogo returns bactopia logo monochrome"() {
        given: "bactopia workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'bactopia', version: '3.0.0']
        }

        when: "generating monochrome logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, true, 'bactopia', 'A comprehensive bacterial genomics workflow')

        then: "returns bactopia logo without color codes"
        result.contains("bactopia")
        !result.contains("\033[")  // No ANSI codes
    }

    def "getLogo returns staphopia logo with colors"() {
        given: "staphopia workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'staphopia', version: '2.0.0']
        }

        when: "generating logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, false, 'staphopia', 'Staphylococcus aureus analysis')

        then: "returns staphopia logo"
        result.contains("staphopia")
        result.contains("2.0.0")
    }

    def "getLogo returns staphopia logo monochrome"() {
        given: "staphopia workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'staphopia', version: '2.0.0']
        }

        when: "generating monochrome logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, true, 'staphopia', 'Staphylococcus aureus analysis')

        then: "returns monochrome staphopia logo"
        result.contains("staphopia")
    }

    def "getLogo returns enteropia logo with colors"() {
        given: "enteropia workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'enteropia', version: '1.5.0']
        }

        when: "generating logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, false, 'enteropia', 'Enterococcus analysis')

        then: "returns enteropia logo"
        result.contains("enteropia")
        result.contains("1.5.0")
    }

    def "getLogo returns enteropia logo monochrome"() {
        given: "enteropia workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'enteropia', version: '1.5.0']
        }

        when: "generating monochrome logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, true, 'enteropia', 'Enterococcus analysis')

        then: "returns monochrome enteropia logo"
        result.contains("enteropia")
    }

    def "getLogo returns cleanyerreads logo with colors"() {
        given: "cleanyerreads workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'cleanyerreads', version: '1.0.0']
        }

        when: "generating logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, false, 'cleanyerreads', 'Read cleaning and QC')

        then: "returns cleanyerreads logo"
        result.contains("clean-yer-reads")
        result.contains("1.0.0")
    }

    def "getLogo returns cleanyerreads logo monochrome"() {
        given: "cleanyerreads workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'cleanyerreads', version: '1.0.0']
        }

        when: "generating monochrome logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, true, 'cleanyerreads', 'Read cleaning and QC')

        then: "returns monochrome cleanyerreads logo"
        result.contains("clean-yer-reads")
    }

    def "getLogo returns teton logo with colors"() {
        given: "teton workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'teton', version: '1.0.0']
        }

        when: "generating logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, false, 'teton', 'Bacterial genome typing')

        then: "returns teton logo"
        result.contains("teton")
        result.contains("1.0.0")
    }

    def "getLogo returns teton logo monochrome"() {
        given: "teton workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'teton', version: '1.0.0']
        }

        when: "generating monochrome logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, true, 'teton', 'Bacterial genome typing')

        then: "returns monochrome teton logo"
        result.contains("teton")
    }

    def "getLogo returns custom workflow logo with colors"() {
        given: "custom workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'customflow', version: '1.0.0']
        }

        when: "generating logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, false, 'customflow', 'Custom workflow description')

        then: "returns custom workflow logo"
        result.contains("customflow")
        result.contains("1.0.0")
    }

    def "getLogo returns custom workflow logo monochrome"() {
        given: "custom workflow"
        def mockMetadata = Mock(WorkflowMetadata) {
            getManifest() >> [name: 'customflow', version: '1.0.0']
        }

        when: "generating monochrome logo"
        def result = BactopiaTemplate.getLogo(mockMetadata, true, 'customflow', 'Custom workflow description')

        then: "returns monochrome custom workflow logo"
        result.contains("customflow")
    }
}
