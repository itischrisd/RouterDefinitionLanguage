package com.kdudek.rdl.ast;

public sealed interface Modifier permits Modifier.Expects, Modifier.Status, Modifier.Secure, Modifier.Tx {

    record Expects(TypeRef t) implements Modifier {
    }

    record Status(int code) implements Modifier {
    }

    record Secure(String role) implements Modifier {
    }

    record Tx() implements Modifier {
    }
}
