package org.jcnc.snow.vm;

import org.jcnc.snow.vm.engine.VMMode;

import static org.jcnc.snow.vm.VMInitializer.initializeAndRunVM;

/**
 * The {@code VMLauncher} class is responsible for initiating the virtual machine (VM) execution process.
 * It processes command-line arguments, loads the instruction set, and invokes the virtual machine engine to
 * execute the instructions. It then prints the virtual machine's current state, allowing users to observe
 * the VM's progress and results.
 *
 * <p>This class provides the entry point to launch the virtual machine. The main method retrieves the file path
 * of the VM instructions from the command-line arguments, initializes the VM engine, and runs the VM in the
 * specified mode (e.g., {@link VMMode#DEBUG}).</p>
 */
public class VMLauncher {

    /**
     * Default constructor for creating an instance of {@code VMLauncher}.
     * <p>This constructor is empty as no specific initialization is required
     * for the {@code VMLauncher} class.</p>
     */
    public VMLauncher() {
        // Empty constructor
    }

    /**
     * The main method serves as the entry point to start the virtual machine execution process.
     * It processes the command-line arguments to retrieve the file path for the virtual machine's
     * instruction set, loads the instructions from the specified file, and executes them using
     * the virtual machine engine.
     *
     * <p>The sequence of operations in this method is as follows:</p>
     * <ol>
     *     <li>Retrieve and validate the file path from the command-line arguments.</li>
     *     <li>Load the instruction set from the specified file.</li>
     *     <li>Execute the instructions using the virtual machine engine.</li>
     *     <li>Output the current state of the virtual machine.</li>
     * </ol>
     *
     * @param args Command-line arguments passed to the program. The primary argument expected
     *             is the file path pointing to the virtual machine's instruction set.
     */
    public static void main(String[] args) {
        // Call the method that initializes and runs the VM in DEBUG mode
        initializeAndRunVM(args, VMMode.DEBUG);
    }
}
