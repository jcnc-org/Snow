package org.jcnc.snow.vm;

import org.jcnc.snow.vm.execution.CommandLoader;
import org.jcnc.snow.vm.engine.VMCommandExecutor;
import org.jcnc.snow.vm.engine.VMMode;
import org.jcnc.snow.vm.engine.VirtualMachineEngine;
import org.jcnc.snow.vm.io.FilePathResolver;
import org.jcnc.snow.vm.utils.VMStateLogger;

import java.util.List;

/**
 * Virtual Machine Initialization Class, responsible for the VM startup process, including:
 * <ul>
 *     <li>Retrieving and validating the file path</li>
 *     <li>Loading the instruction set</li>
 *     <li>Executing the instructions</li>
 *     <li>Printing the virtual machine's state</li>
 * </ul>
 * <p>
 * This class contains the main method `main(String[] args)`, which initiates the virtual machine execution flow.
 * </p>
 * <p>The main process:</p>
 * <ol>
 *     <li>Retrieve and parse the file path from command-line arguments</li>
 *     <li>Load the instructions from the specified file path</li>
 *     <li>Execute the loaded instructions using the virtual machine engine</li>
 *     <li>Print the virtual machine's current state</li>
 * </ol>
 */
public class VMInitializer {
    /**
     * Default constructor for creating an instance of VMInitializer.
     * This constructor is empty as no specific initialization is required.
     */
    public VMInitializer() {
        // Empty constructor
    }

    /**
     * Initializes the virtual machine by processing the file path, loading instructions, executing them,
     * and printing the virtual machine's state.
     *
     * @param args   Command-line arguments containing the file path of the virtual machine instructions
     * @param vmMode The mode in which the virtual machine should operate.
     *               This can be used to specify different operational modes (e.g., debug mode, normal mode).
     */
    public static void initializeAndRunVM(String[] args, VMMode vmMode) {
        // Retrieve and validate file path
        String filePath = FilePathResolver.getFilePath(args);
        if (filePath == null) return;

        // Load commands from the file
        List<String> commands = CommandLoader.loadInstructions(filePath);
        if (commands.isEmpty()) return;

        // Execute the commands using the virtual machine engine
        VirtualMachineEngine virtualMachineEngine = new VirtualMachineEngine(vmMode);
        VMCommandExecutor.executeInstructions(virtualMachineEngine, commands);

        // Print the virtual machine's state
        VMStateLogger.printVMState(virtualMachineEngine);
    }
}
