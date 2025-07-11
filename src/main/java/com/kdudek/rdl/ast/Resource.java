package com.kdudek.rdl.ast;

import java.util.List;

public record Resource(String path,
                       List<Annotation> ann,
                       List<Param> params,
                       List<RcContent> body) implements RcContent {
}
