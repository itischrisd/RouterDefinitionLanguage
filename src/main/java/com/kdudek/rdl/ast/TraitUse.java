package com.kdudek.rdl.ast;

import java.util.List;

public record TraitUse(String name, List<TypeRef> actuals) implements RcContent {
}
