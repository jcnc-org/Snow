module org.jcnc.snow.compiler {
    uses org.jcnc.snow.cli.CLICommand;
    requires java.desktop;
    requires java.logging;
    exports org.jcnc.snow.compiler.ir.core;
    exports org.jcnc.snow.compiler.ir.instruction;
}
