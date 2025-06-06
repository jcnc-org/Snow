package org.jcnc.snow.vm.io;

import org.jcnc.snow.vm.utils.LoggingUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * The FileIOUtils class handles file input and output operations, primarily for reading instructions from files.
 * This class provides various methods to handle file paths, read file contents, clean up unnecessary lines in files, etc.
 * <p>
 * The primary functionality of this class is to read instructions from files and remove comments, empty lines, and other irrelevant content.
 * </p>
 */
public class FileIOUtils {
    /**
     * Default constructor for creating an instance of FileIOUtils.
     * This constructor is empty as no specific initialization is required.
     */
    public FileIOUtils() {
        // Empty constructor
    }

    /**
     * Retrieves the file path from the command line arguments.
     * <p>
     * This method checks the number of command line arguments and extracts the file path from the arguments.
     * </p>
     *
     * @param args Command line arguments
     * @return The file path if the command line arguments contain a valid file path; otherwise, returns null.
     */
    public static String getFilePathFromArgs(String[] args) {
        if (args.length != 1) {
            LoggingUtils.logError("Please provide a valid file path.");
            return null;
        }
        return args[0];
    }

    /**
     * Reads and processes instructions from the specified file path.
     * <p>
     * This method reads each line of the file, removes comments and empty lines, and returns a list of valid instructions.
     * </p>
     *
     * @param filePath The file path
     * @return A list of processed instructions from the file. If an error occurs while reading the file, returns an empty list.
     */
    public static List<String> readInstructionsFromFile(String filePath) {
        try (Stream<String> lines = Files.lines(Path.of(filePath))) {
            return lines.map(FileIOUtils::cleanLine)  // Remove comments from the line
                    .filter(FileIOUtils::isNotEmpty) // Filter out empty lines
                    .collect(toList()); // Collect into a list
        } catch (IOException e) {
            LoggingUtils.logError("Error reading file: " + e.getMessage());
            return List.of(); // Return an empty list if an error occurs
        }
    }

    /**
     * Cleans up the content of each line by removing comments and trimming whitespace.
     * <p>
     * This method removes comments (sections starting with "//") from the line and trims any unnecessary whitespace.
     * </p>
     *
     * @param line The original line
     * @return The cleaned-up line with comments removed and whitespace trimmed.
     */
    public static String cleanLine(String line) {
        if (line == null || line.isEmpty()) {
            return "";  // Return an empty string if the line is null or empty
        }
        return line.replaceAll("//.*", "").trim(); // Remove comments and trim whitespace
    }

    /**
     * Checks if the line is empty.
     * <p>
     * This method checks if the line is an empty string or contains only whitespace characters.
     * </p>
     *
     * @param line The line to check
     * @return True if the line is not empty or does not contain only spaces; otherwise, returns false.
     */
    public static boolean isNotEmpty(String line) {
        return !line.isEmpty();  // Return whether the line is not empty
    }
}
