import org.jcnc.snow.cli.api.CLICommand;

module org.jcnc.snow.compiler {
    uses CLICommand;
    requires java.desktop;
    requires java.logging;
    exports org.jcnc.snow.compiler.ir.core;
    exports org.jcnc.snow.compiler.ir.instruction;
}
