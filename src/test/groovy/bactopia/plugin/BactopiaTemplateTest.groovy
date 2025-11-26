package bactopia.plugin

import groovy.util.logging.Slf4j
import spock.lang.Specification

/**
 * Tests for BactopiaTemplate methods
 */
@Slf4j
class BactopiaTemplateTest extends Specification {

    def 'should return color codes when monochrome disabled' () {
        when:
        def colors = BactopiaTemplate.getLogColors(false)
        
        then:
        colors.reset == "\033[0m"
        colors.bold == "\033[1m"
        colors.red == "\033[0;31m"
        colors.green == "\033[0;32m"
        colors.yellow == "\033[0;33m"
        colors.blue == "\033[0;34m"
        colors.cyan == "\033[0;36m"
        colors.white == "\033[0;37m"
        colors.bgreen == "\033[1;32m"
        colors.bred == "\033[1;31m"
    }

    def 'should return empty strings when monochrome enabled' () {
        when:
        def colors = BactopiaTemplate.getLogColors(true)
        
        then:
        colors.reset == ''
        colors.bold == ''
        colors.red == ''
        colors.green == ''
        colors.yellow == ''
        colors.blue == ''
        colors.cyan == ''
        colors.white == ''
        colors.bgreen == ''
        colors.bred == ''
    }

    def 'should generate dashed line with colors' () {
        when:
        def line = BactopiaTemplate.dashedLine(false)
        
        then:
        line.startsWith('-')
        line.endsWith('-')
        line.contains("\033[") // Contains ANSI codes
    }

    def 'should generate dashed line without colors in monochrome mode' () {
        when:
        def line = BactopiaTemplate.dashedLine(true)
        
        then:
        line.startsWith('-')
        line.endsWith('-')
        !line.contains("\033[") // No ANSI codes
    }

}
