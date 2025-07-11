package com.kdudek.rdl;


import com.kdudek.rdl.ast.CompilationUnit;
import com.kdudek.rdl.lexer.lexer;
import com.kdudek.rdl.parser.parser;
import com.kdudek.rdl.semantic.ControllerGenerator;
import java_cup.runtime.Symbol;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[]{
                    "src/main/resources/full-showcase.dsl",
                    "src/main/java"
            };
        }
        if (args.length < 2) {
            System.err.println("usage: rdl <input.rdl> <outputDir>");
            System.exit(1);
        }
        try (Reader r = new FileReader(args[0])) {
            lexer lx = new lexer(r);
            parser ps = new parser(lx);
            Symbol s = ps.parse();
            CompilationUnit cu = (CompilationUnit) s.value;
            Path out = Path.of(args[1]);
            ControllerGenerator.generate(cu, out);
            System.out.println("✓ Controllers generated in: " + out.toAbsolutePath());
        }
    }
}