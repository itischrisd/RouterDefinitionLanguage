package com.kdudek.rdl.ast;

public sealed interface Literal permits Literal.IntLit, Literal.StringLit {

    record IntLit(int value) implements Literal {
    }

    record StringLit(String value) implements Literal {
    }
}
