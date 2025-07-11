package com.kdudek.rdl.semantic;

import com.kdudek.rdl.ast.Annotation;
import com.kdudek.rdl.ast.CompilationUnit;
import com.kdudek.rdl.ast.Endpoint;
import com.kdudek.rdl.ast.Literal;
import com.kdudek.rdl.ast.Modifier;
import com.kdudek.rdl.ast.Param;
import com.kdudek.rdl.ast.RcContent;
import com.kdudek.rdl.ast.Resource;
import com.kdudek.rdl.ast.Trait;
import com.kdudek.rdl.ast.TraitMember;
import com.kdudek.rdl.ast.TraitUse;
import com.kdudek.rdl.ast.TypeRef;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ControllerGenerator {

    private ControllerGenerator() {
    }

    public static void generate(CompilationUnit cu, Path root) throws IOException {

        Path outDir = cu.pkg() == null || cu.pkg().isBlank()
                ? root
                : root.resolve(cu.pkg().replace('.', '/'));
        Files.createDirectories(outDir);

        Map<String, Trait> traitMap = cu.traits().stream()
                .collect(Collectors.toMap(Trait::name, t -> t));

        for (Resource r : cu.resources())
            emitResourceClass(r, cu, traitMap, outDir, List.of());
    }

    private static void emitResourceClass(Resource res,
                                          CompilationUnit cu,
                                          Map<String, Trait> traits,
                                          Path dir,
                                          List<String> parentPaths) throws IOException {

        String cls = toJavaId(cap(lastSegment(res.path()))) + "Controller";
        Path file = dir.resolve(cls + ".java");

        try (var w = Files.newBufferedWriter(file)) {

            if (cu.pkg() != null && !cu.pkg().isBlank())
                w.write("package " + cu.pkg() + ";\n\n");

            for (String imp : cu.imports())
                w.write("import " + imp + ";\n");
            w.write("""
                    import org.springframework.web.bind.annotation.*;
                    import org.springframework.http.HttpStatus;
                    import java.util.*;
                    
                    """);

            writeJavadoc(res.ann(), w, "");

            for (Annotation a : res.ann())
                w.write(renderAnn(a, "") + "\n");

            String absPath = joinPaths(parentPaths, res.path());
//            w.write("@RequestMapping(\""+absPath+"\")\n");
            w.write("public class " + cls + " {\n\n");

            for (Param p : res.params())
                w.write("    // resource param " + p.name() + " : " + renderType(p.type()) + "\n");

            for (RcContent c : res.body())
                emitContent(c, w, "    ", traits,
                        merge(parentPaths, res.path()));
            w.write("}\n");
        }

        for (RcContent c : res.body())
            if (c instanceof Resource sub)
                emitResourceClass(sub, cu, traits, dir,
                        merge(parentPaths, res.path()));
    }

    private static void emitContent(RcContent c,
                                    java.io.Writer w,
                                    String ind,
                                    Map<String, Trait> traits,
                                    List<String> prefix) throws IOException {

        if (c instanceof Endpoint ep) {
            writeEndpoint(ep, w, ind, prefix);
        } else if (c instanceof TraitUse tu) {
            Trait tr = traits.get(tu.name());
            if (tr == null) throw new IllegalStateException("No trait " + tu.name());
            for (TraitMember tm : tr.members())
                writeEndpoint(applyOverride(tm, substitute(tm.ep(), tu)), w, ind, prefix);
        }
    }

    private static void writeEndpoint(Endpoint ep,
                                      java.io.Writer w,
                                      String ind,
                                      List<String> prefix) throws IOException {

        writeJavadoc(ep.ann(), w, ind);
        for (Annotation a : ep.ann())
            w.write(ind + renderAnn(a, ind) + "\n");

        for (Modifier m : ep.modifiers()) {
            if (m instanceof Modifier.Tx)
                w.write(ind + "@Transactional\n");
            if (m instanceof Modifier.Secure(String role))
                w.write(ind + "@PreAuthorize(\"hasRole('" + role + "')\")\n");
            if (m instanceof Modifier.Status(int code))
                w.write(ind + "@ResponseStatus(HttpStatus.valueOf(" + code + "))\n");
        }

        String mapping = switch (ep.verb()) {
            case GET -> "@GetMapping";
            case POST -> "@PostMapping";
            case PUT -> "@PutMapping";
            case PATCH -> "@PatchMapping";
            case DELETE -> "@DeleteMapping";
            case HEAD, OPTIONS -> "@RequestMapping(method = RequestMethod." + ep.verb() + ")";
        };
        w.write(ind + mapping + "(\"");
        w.write(String.join("", prefix));
        w.write(ep.paths().getFirst());
        w.write("\")\n");

        String retType = ep.returns() == null ? "void" : renderType(ep.returns());
        String mName = toJavaId(ep.paths().getFirst().replace("/", "_"));
        List<String> params = new ArrayList<>();

        ep.modifiers().stream()
                .filter(m -> m instanceof Modifier.Expects)
                .map(m -> (Modifier.Expects) m)
                .findFirst()
                .ifPresent(ex -> params.add("@RequestBody " + renderType(ex.t()) + " body"));

        Set<String> pathVars = extractVars(prefix, ep.paths().getFirst());
        for (Param p : ep.params()) {
            String a = pathVars.contains(p.name())
                    ? "@PathVariable" : "@RequestParam";
            if (!pathVars.contains(p.name()) && p.optional())
                a += "(required = false)";
            params.add(a + " " + renderType(p.type()) + " " + p.name());
        }

        w.write(ind + "public " + retType + " " + mName + "(" +
                String.join(", ", params) + ") {\n");
        if (retType.equals("void"))
            w.write(ind + "    // TODO\n");
        else
            w.write(ind + "    return null; // TODO\n");
        w.write(ind + "}\n\n");
    }

    private static Endpoint substitute(Endpoint ep, TraitUse tu) {
        return ep;
    }

    private static Endpoint applyOverride(TraitMember tm, Endpoint ep) {
        return tm.override() ? new Endpoint(ep.verb(), ep.paths(), ep.ann(),
                ep.params(), ep.modifiers(), ep.returns(), true) : ep;
    }

    private static void writeJavadoc(List<Annotation> anns,
                                     java.io.Writer w,
                                     String ind) throws IOException {
        var jd = anns.stream()
                .filter(a -> a.name().equals("Doc"))
                .map(a -> a.args().get(""))
                .filter(Literal.StringLit.class::isInstance)
                .map(a -> ((Literal.StringLit) a).value())
                .findFirst();
        if (jd.isPresent()) {
            w.write(ind + "/**\n");
            for (String ln : jd.get().split("\n"))
                w.write(ind + " * " + ln + "\n");
            w.write(ind + " */\n");
        }
    }

    private static String renderAnn(Annotation a, String ind) {
        if (a.name().equals("Doc")) return "";
        if (a.args().isEmpty()) return ind + "@" + a.name();
        if (a.args().size() == 1 && a.args().containsKey(""))
            return ind + "@" + a.name() + "(\"" + lit(a.args().get("")) + "\")";
        return ind + "@" + a.name() + "(" + a.args().entrySet().stream()
                .map(e -> (e.getKey().isEmpty() ? "" : e.getKey() + "=") + lit(e.getValue()))
                .collect(Collectors.joining(", ")) + ")";
    }

    private static String lit(Literal l) {
        return switch (l) {
            case Literal.IntLit i -> String.valueOf(i.value());
            case Literal.StringLit s -> "\"" + s.value() + "\"";
        };
    }

    private static String renderType(TypeRef t) {
        return switch (t) {
            case TypeRef.Simple s -> s.fqcn();
            case TypeRef.Array a -> renderType(a.of()) + "[]";
            case TypeRef.Generic g -> g.raw().fqcn() + "<" +
                    g.args().stream().map(ControllerGenerator::renderType)
                            .collect(Collectors.joining(",")) + ">";
            case TypeRef.Wild w -> "?" + (w.bound() == null ? "" :
                    (w.plus() ? " extends " : " super ") + renderType(w.bound()));
        };
    }

    private static String cap(String s) {
        return s.isEmpty() ? s :
                Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String toJavaId(String s) {
        String c = s.replaceAll("[^A-Za-z0-9]", "_");
        return Character.isJavaIdentifierStart(c.charAt(0)) ? c : "_" + c;
    }

    private static String lastSegment(String p) {
        int i = p.lastIndexOf('/');
        return i < 0 ? p : p.substring(i + 1);
    }

    private static List<String> merge(List<String> pref, String more) {
        List<String> n = new ArrayList<>(pref);
        n.add(more);
        return n;
    }

    private static String joinPaths(List<String> pref, String more) {
        return String.join("", pref) + more;
    }

    private static Set<String> extractVars(List<String> pref, String p) {
        Pattern var = Pattern.compile("\\{([A-Za-z0-9_]+)}");
        Set<String> s = new HashSet<>();
        for (String seg : pref) {
            Matcher m = var.matcher(seg);
            while (m.find()) s.add(m.group(1));
        }
        Matcher m = var.matcher(p);
        while (m.find()) s.add(m.group(1));
        return s;
    }
}
