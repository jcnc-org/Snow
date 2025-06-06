package org.jcnc.snow.vm.gui;


import org.jcnc.snow.vm.module.LocalVariableStore;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Vector;

/**
 * The LocalVariableStoreSwing class provides a graphical user interface (GUI) to display the virtual machine's local variable store.
 * This class displays the local variable table in a JTable within a Swing window for users to view the current local variables and their values.
 *
 * <p>The class provides static methods to create windows, generate tables, and configure the table display, among other functionalities.</p>
 */
public class LocalVariableStoreSwing {
    /**
     * Default constructor for creating an instance of LocalVariableStoreSwing.
     * This constructor is empty as no specific initialization is required.
     */
    public LocalVariableStoreSwing() {
        // Empty constructor
    }

    /**
     * Displays the local variable table in a window.
     *
     * <p>This method opens a new Swing window containing a table that shows the content of the current local variable store.</p>
     *
     * @param localVariableStore The current local variable store.
     * @param title              The title of the window.
     */
    public static void display(LocalVariableStore localVariableStore, String title) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = createFrame(title);
            JTable table = createTableFromLocalVariables(localVariableStore);
            frame.add(new JScrollPane(table), BorderLayout.CENTER);
            frame.setVisible(true);
        });
    }

    /**
     * Creates and configures a new JFrame window.
     *
     * @param title The title of the window.
     * @return The created and configured JFrame window.
     */
    private static JFrame createFrame(String title) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(null);  // Center the window on the screen
        return frame;
    }

    /**
     * Creates a JTable based on the local variable store.
     *
     * <p>This method retrieves the local variable data from the store and converts it into a non-editable JTable.</p>
     *
     * @param localVariableStore The current local variable store.
     * @return The created JTable displaying the local variable table content.
     */
    private static JTable createTableFromLocalVariables(LocalVariableStore localVariableStore) {
        List<Object> variables = localVariableStore.getLocalVariables();

        // Create column names
        Vector<String> columnNames = createColumnNames();

        // Create table data
        Vector<Vector<String>> data = createTableData(variables);

        // Create a non-editable table model
        DefaultTableModel model = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;  // Disable cell editing
            }
        };

        JTable table = new JTable(model);  // Create the table

        // Configure the table selection mode
        configureTableSelection(table);

        return table;
    }

    /**
     * Creates the column names for the table.
     *
     * @return A Vector containing the column names.
     */
    private static Vector<String> createColumnNames() {
        Vector<String> columnNames = new Vector<>();
        columnNames.add("Index");
        columnNames.add("Local Variable Value");
        return columnNames;
    }

    /**
     * Creates the table data based on the local variable list.
     *
     * @param variables The list of local variables.
     * @return The table data, formatted as a Vector.
     */
    private static Vector<Vector<String>> createTableData(List<Object> variables) {
        Vector<Vector<String>> data = new Vector<>(variables.size());
        for (int i = 0; i < variables.size(); i++) {
            Vector<String> row = new Vector<>(2);
            row.add(String.valueOf(i));
            row.add(String.valueOf(variables.get(i)));
            data.add(row);
        }
        return data;
    }

    /**
     * Configures the table's selection mode, disabling the selection of cells, rows, and columns.
     *
     * @param table The JTable to configure.
     */
    private static void configureTableSelection(JTable table) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setCellSelectionEnabled(false);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
    }
}
