package com.kdudek.rdl.ast;

import java.util.List;

public record Endpoint(HttpVerb verb,
                       List<String> paths,
                       List<Annotation> ann,
                       List<Param> params,
                       List<Modifier> modifiers,
                       TypeRef returns,
                       boolean override) implements RcContent {
}
