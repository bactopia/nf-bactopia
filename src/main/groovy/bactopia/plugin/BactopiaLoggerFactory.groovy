package bactopia.plugin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import groovy.transform.CompileStatic
import org.slf4j.LoggerFactory
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Factory class that sets up global log capturing for all classes in the plugin.
 * This intercepts ALL SLF4J logs and captures them for later retrieval.
 * Uses CopyOnWriteArrayList for thread-safe iteration without synchronization.
 */
@CompileStatic
class BactopiaLoggerFactory {
    
    private static final List<LogEntry> globalLogs = new CopyOnWriteArrayList<LogEntry>()
    private static boolean initialized = false
    private static Map colors
    private static boolean monochrome = false
    
    static class LogEntry {
        String loggerName
        Level level
        String message
        Long timestamp
        
        LogEntry(String loggerName, Level level, String message) {
            this.loggerName = loggerName
            this.level = level
            this.message = message
            this.timestamp = System.currentTimeMillis()
        }
    }
    
    private static class GlobalCaptureAppender extends AppenderBase<ILoggingEvent> {
        @Override
        protected void append(ILoggingEvent event) {
            // Only capture logs from bactopia package
            if (event.getLoggerName().startsWith("bactopia.plugin")) {
                globalLogs.add(new LogEntry(
                    event.getLoggerName(),
                    event.getLevel(),
                    event.getFormattedMessage()
                ))
            }
        }
    }
    
    /**
     * Initialize global log capturing. Call this once at plugin startup.
     *
     * @param monochromeLogs Whether to use monochrome (no color) output
     */
    static void initialize(Boolean monochromeLogs = false) {
        if (initialized) {
            return
        }
        
        monochrome = monochromeLogs
        colors = BactopiaTemplate.getLogColors(monochromeLogs)
        
        // Get the root logger
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        LoggerContext context = rootLogger.getLoggerContext()
        
        // Create and add our global appender
        GlobalCaptureAppender appender = new GlobalCaptureAppender()
        appender.setName("BactopiaGlobalCapture")
        appender.setContext(context)
        appender.start()
        
        rootLogger.addAppender(appender)
        initialized = true
    }
    
    /**
     * Get all captured logs.
     *
     * @param withColors Whether to include ANSI color codes in output
     * @return List of captured log messages
     */
    static List<String> getCapturedLogs(Boolean withColors = true) {
        List<String> result = []
        for (LogEntry entry : globalLogs) {
            // Check if message already contains ANSI color codes
            boolean hasColors = entry.message.contains('\033[')
            
            if (withColors && !monochrome && !hasColors) {
                switch (entry.level) {
                    case Level.ERROR:
                        result.add("${colors.red}${entry.message}${colors.reset}".toString())
                        break
                    case Level.WARN:
                        result.add("${colors.yellow}${entry.message}${colors.reset}".toString())
                        break
                    case Level.INFO:
                        result.add(entry.message.toString())
                        break
                    case Level.DEBUG:
                        result.add("${colors.blue}${entry.message}${colors.reset}".toString())
                        break
                    default:
                        result.add(entry.message.toString())
                }
            } else {
                result.add(entry.message.toString())
            }
        }
        return result
    }
    
    /**
     * Get captured logs as a single string.
     *
     * @param withColors Whether to include ANSI color codes in output
     * @return All captured logs joined with newlines
     */
    static String getCapturedLogsAsString(Boolean withColors = true) {
        return getCapturedLogs(withColors).join("\n")
    }
    
    /**
     * Get logs from a specific logger.
     *
     * @param loggerName The name of the logger to filter by
     * @param withColors Whether to include ANSI color codes in output
     * @return List of log messages from the specified logger
     */
    static List<String> getLogsFromLogger(String loggerName, Boolean withColors = true) {
        List<String> result = []
        for (LogEntry entry : globalLogs) {
            if (entry.loggerName.contains(loggerName)) {
                // Check if message already contains ANSI color codes
                boolean hasColors = entry.message.contains('\033[')
                
                if (withColors && !monochrome && !hasColors) {
                    switch (entry.level) {
                        case Level.ERROR:
                            result.add("${colors.red}${entry.message}${colors.reset}".toString())
                            break
                        case Level.WARN:
                            result.add("${colors.yellow}${entry.message}${colors.reset}".toString())
                            break
                        case Level.INFO:
                            result.add(entry.message.toString())
                            break
                        case Level.DEBUG:
                            result.add("${colors.blue}${entry.message}${colors.reset}".toString())
                            break
                        default:
                            result.add(entry.message)
                    }
                } else {
                    result.add(entry.message)
                }
            }
        }
        return result
    }
    
    /**
     * Get logs by level.
     *
     * @param level The log level to filter by (ERROR, WARN, INFO, DEBUG)
     * @return List of LogEntry objects matching the specified level
     */
    static List<LogEntry> getLogsByLevel(String level) {
        Level logLevel = Level.toLevel(level.toUpperCase())
        return globalLogs.findAll { it.level == logLevel }
    }
    
    /**
     * Clear all captured logs.
     */
    static void clearLogs() {
        globalLogs.clear()
    }
    
    /**
     * Check if the logger factory has been initialized.
     *
     * @return True if initialized, false otherwise
     */
    static boolean isInitialized() {
        return initialized
    }
    
    /**
     * Check if there are any error level logs.
     *
     * @return True if any ERROR level logs exist, false otherwise
     */
    static boolean hasErrors() {
        return globalLogs.any { it.level == Level.ERROR }
    }

    /**
     * Get the logs using getCapturedLogsAsString and clear them.
     *
     * @param data Optional data string to include in the returned map
     * @param withColors Whether to include ANSI color codes in output
     * @return Map containing data, logs, hasErrors flag, and error message
     */
    static Map captureAndClearLogs(String data = "", Boolean withColors = true) {
        Boolean hasErrors = hasErrors()
        String logs = getCapturedLogsAsString(withColors)
        clearLogs()
        return [
            data: data,
            logs: logs,
            hasErrors: hasErrors,
            error: hasErrors ? "${colors.red}!! ERROR !!${colors.reset}\n\n" + logs : ""
        ]
    }
}
