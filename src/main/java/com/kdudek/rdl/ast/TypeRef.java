package com.kdudek.rdl.ast;

import java.util.List;

public sealed interface TypeRef permits TypeRef.Simple, TypeRef.Generic, TypeRef.Array, TypeRef.Wild {

    record Simple(String fqcn) implements TypeRef {
    }

    record Generic(Simple raw, List<TypeRef> args) implements TypeRef {
    }

    record Array(TypeRef of) implements TypeRef {
    }

    record Wild(TypeRef bound, boolean plus) implements TypeRef {
    }
}
