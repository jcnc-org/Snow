package org.jcnc.snow.vm.io;

import org.jcnc.snow.vm.utils.LoggingUtils;

/**
 * The FilePathResolver class handles command-line arguments to retrieve a valid file path.
 * This class contains a method that extracts the file path from the command-line arguments.
 * If no file path is provided, an error message will be logged.
 * <p>
 * The main functionality of this class is to obtain the file path from the command-line arguments and provide relevant error logging.
 * </p>
 */
public class FilePathResolver {
    /**
     * Default constructor for creating an instance of FilePathResolver.
     * This constructor is empty as no specific initialization is required.
     */
    public FilePathResolver() {
        // Empty constructor
    }

    /**
     * Retrieves the file path.
     * <p>
     * This method calls the method in the FileIOUtils class to obtain the file path from the command-line arguments.
     * If a valid file path is not provided, an error message will be logged.
     * </p>
     *
     * @param args Command-line arguments containing the file path provided by the user
     * @return The file path if a valid path is provided in the command-line arguments; otherwise, returns null.
     */
    public static String getFilePath(String[] args) {
        // Use the FileIOUtils class to obtain the file path
        String filePath = FileIOUtils.getFilePathFromArgs(args);

        // Log an error message if no valid file path is provided
        if (filePath == null) {
            LoggingUtils.logError("No valid file path provided.");
        }

        return filePath;
    }
}
