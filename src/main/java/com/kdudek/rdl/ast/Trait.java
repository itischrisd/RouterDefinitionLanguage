package com.kdudek.rdl.ast;

import java.util.List;

public record Trait(String name,
                    List<String> generics,
                    List<TraitMember> members,
                    String javadoc) {
}
