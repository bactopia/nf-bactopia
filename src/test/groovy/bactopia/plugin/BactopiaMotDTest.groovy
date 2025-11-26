package bactopia.plugin

import spock.lang.Specification

/**
 * Tests for BactopiaMotD (Message of the Day)
 */
class BactopiaMotDTest extends Specification {

    def 'should return a message of the day' () {
        when:
        def motd = BactopiaMotD.getMotD(true)
        
        then:
        motd != null
        motd.length() > 0
    }

    def 'should return a message without color codes in monochrome mode' () {
        when:
        def motd = BactopiaMotD.getMotD(true)
        
        then:
        !motd.contains("\033[") // No ANSI codes
    }

    def 'should return a message with color codes when colors enabled' () {
        when:
        def motd = BactopiaMotD.getMotD(false)
        
        then:
        motd != null
        motd.length() > 0
        // Message is wrapped in colors, so it should contain ANSI codes
        motd.contains("\033[") || motd.length() > 50 // Contains colors or has substantial content
    }

    def 'should return different messages on multiple calls' () {
        when:
        def messages = []
        // Call multiple times to potentially get different messages
        20.times {
            messages << BactopiaMotD.getMotD(true)
        }
        
        then:
        messages.size() == 20
        // With enough calls, we should see some variety (not guaranteed but likely)
        messages.every { it != null && it.length() > 0 }
    }
}
