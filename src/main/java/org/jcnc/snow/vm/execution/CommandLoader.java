package org.jcnc.snow.vm.execution;


import org.jcnc.snow.vm.io.FileIOUtils;
import org.jcnc.snow.vm.utils.LoggingUtils;

import java.util.List;

/**
 * Command Loader (CommandLoader)
 * <p>
 * This class is used to load instructions from a specified file path. It reads instruction data from the file
 * using {@link FileIOUtils}, and returns a list of instructions. If the file reading fails or the instruction list is empty,
 * an error log will be recorded using {@link LoggingUtils}.
 * </p>
 */
public class CommandLoader {
    /**
     * Default constructor for creating an instance of CommandLoader.
     * This constructor is empty as no specific initialization is required.
     */
    public CommandLoader() {
        // Empty constructor
    }

    /**
     * Loads instructions from a file.
     * <p>
     * This method loads instructions from the given file path and returns them as a list. If the instruction reading fails or the list is empty,
     * an error log will be recorded and an empty instruction list will be returned.
     * </p>
     *
     * @param filePath The file path pointing to the file containing the instructions.
     * @return A list of instructions. If reading fails, an empty list will be returned.
     */
    public static List<String> loadInstructions(String filePath) {
        // Read instructions from the file
        List<String> instructions = FileIOUtils.readInstructionsFromFile(filePath);

        // If the instruction list is empty, log an error message
        if (instructions.isEmpty()) {
            LoggingUtils.logError("Failed to read instructions or the instruction list is empty");
        }

        // Return the instruction list
        return instructions;
    }
}
