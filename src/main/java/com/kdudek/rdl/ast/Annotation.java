package com.kdudek.rdl.ast;

import java.util.Map;

public record Annotation(String name, Map<String, Literal> args) {
}
