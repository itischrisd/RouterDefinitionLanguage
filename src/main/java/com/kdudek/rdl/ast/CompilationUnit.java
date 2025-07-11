package com.kdudek.rdl.ast;

import java.util.List;

public record CompilationUnit(String pkg,
                              List<String> imports,
                              List<Trait> traits,
                              List<Resource> resources) {
}
