package org.jcnc.snow.compiler.lexer.core;

public class LexicalError {
    private final String file;
    private final int line;
    private final int column;
    private final String message;

    public LexicalError(String file, int line, int column, String message) {
        this.file = file;
        this.line = line;
        this.column = column;
        this.message = message;
    }

    @Override
    public String toString() {
        return file + ": 行 " + line + ", 列 " + column + ": " + message;
    }
}
