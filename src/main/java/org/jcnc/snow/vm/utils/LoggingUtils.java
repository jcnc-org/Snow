package org.jcnc.snow.vm.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jcnc.snow.common.SnowConfig.print;

/**
 * The LoggingUtils class provides logging functionality, supporting different log levels for output.
 * This class uses Java's built-in logging system for logging, supporting both console output and log file recording.
 * <p>
 * This class offers two methods for logging at different levels:
 * 1. Error logs: Used for recording error messages, outputting them to both the console and log file.
 * 2. Information logs: Used for recording general information, outputting to the console.
 * </p>
 */
public class LoggingUtils {
    // Logger instance using Java's built-in Logger class
    private static final Logger LOGGER = Logger.getLogger(LoggingUtils.class.getName());

    /**
     * Default constructor for creating an instance of LoggingUtils.
     * This constructor is empty as no specific initialization is required.
     */
    public LoggingUtils() {
        // Empty constructor
    }

    /**
     * Logs an error message, recording it to both the console and log file.
     * <p>
     * This method is used to log error-level messages. It outputs the error message to the console and logs it to a file,
     * allowing developers to review error details.
     * </p>
     *
     * @param message The error message detailing the issue.
     */
    public static void logError(String message) {
        // Output the error message to the console
        System.err.println("Error: " + message);
        // Log the error message with SEVERE level to the log file
        LOGGER.log(Level.SEVERE, message);
    }

    /**
     * Logs an informational message.
     * <p>
     * This method is used to log general information. It outputs the specified message to the console for developers to review.
     * </p>
     *
     * @param title   The log title, used to indicate the subject or type of the log.
     * @param message The information content to be logged.
     */
    public static void logInfo(String title, String message) {
        // Output the informational message to the console
        print(title + message);
    }
}
