package com.kdudek.rdl.lexer;

import java.util.HashMap;
import java.util.Map;
import java_cup.runtime.Symbol;
import com.kdudek.rdl.parser.sym;

%%

%public
%class lexer
%implements com.kdudek.rdl.parser.sym
%unicode
%cup
%line
%column

%state LINE_COMMENT BLOCK_COMMENT DOC_COMMENT

%{
    private final Map<String, String> macros = new HashMap<>();
    private int blockDepth = 0;
    private StringBuilder docBuf;

    private Symbol sym(int id) {
        return new Symbol(id, yyline + 1, yycolumn + 1);
    }

    private Symbol sym(int id, Object v) {
        return new Symbol(id, yyline + 1, yycolumn + 1, v);
    }

    private String substitute(String txt) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < txt.length()) {
            int open = txt.indexOf("${", i);
            if (open < 0) {
                out.append(txt.substring(i));
                break;
            }
            out.append(txt, i, open);
            int close = txt.indexOf('}', open + 2);
            if (close < 0)
                throw new RuntimeException("Unterminated ${  in line " + (yyline + 1));
            String key = txt.substring(open + 2, close);
            String val = macros.get(key);
            if (val == null)
                throw new RuntimeException("Undefined macro \"" + key + "\" in line " + (yyline + 1));
            out.append(val);
            i = close + 1;
        }
        return out.toString();
    }

    private String unescape(String raw) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\') {
                if (i == raw.length() - 1) throw new RuntimeException("Bad escape seq @ line " + (yyline + 1));
                char n = raw.charAt(++i);
                switch (n) {
                    case 'n':
                        b.append('\n');
                        break;
                    case 'r':
                        b.append('\r');
                        break;
                    case 't':
                        b.append('\t');
                        break;
                    case '"':
                        b.append('\"');
                        break;
                    case '\\':
                        b.append('\\');
                        break;
                    case '{':
                        b.append('{');
                        break;
                    case '}':
                        b.append('}');
                        break;
                    case '$':
                        b.append('$');
                        break;
                    default:
                        b.append(n);
                        break;
                }
            } else b.append(c);
        }
        return b.toString();
    }
%}

LineTerminator   = \r\n|\r|\n
WhiteSpace       = [ \t\f]+ | {LineTerminator}

JavaStart        = [A-Za-z_]
JavaPart         = [A-Za-z0-9_]
Identifier       = {JavaStart}({JavaPart}|"_")*

EscapeSeq        = \\[\"\\nrt{}$]

StringChar       = ([^\"\\\n\r]|{EscapeSeq})
StringLiteral    = \"{StringChar}*\"

PathChar         = ([^\"\\\n\r]|{EscapeSeq})
PathLiteral      = \"\/{PathChar}*\"

MacroDir         = "#var"

%%

<YYINITIAL> {
"package"          {  return sym(PACKAGE); }
"import"           {  return sym(IMPORT);  }
"trait"            {  return sym(TRAIT);   }
"resource"         {  return sym(RESOURCE);}
"uses"             {  return sym(USES);    }
"override"         {  return sym(OVERRIDE);}
"extends"          {  return sym(EXTENDS); }

"GET"              {  return sym(GET);     }
"POST"             {  return sym(POST);    }
"PUT"              {  return sym(PUT);     }
"PATCH"            {  return sym(PATCH);   }
"DELETE"           {  return sym(DELETE);  }
"HEAD"             {  return sym(HEAD);    }
"OPTIONS"          {  return sym(OPTIONS); }

"expects"          {  return sym(EXPECTS); }
"returns"          {  return sym(RETURNS); }
"status"           {  return sym(STATUS);  }
"secure"           {  return sym(SECURE);  }
"transactional"    {  return sym(TRANSACTIONAL); }
"void"             {  return sym(VOID);    }

^{WhiteSpace}*{MacroDir}{WhiteSpace}+{Identifier}{WhiteSpace}+{StringLiteral} {
          String[] parts = yytext().trim().split("\\s+", 4);
          String name= parts[1];
          String rawVal = parts[2];
          String val = unescape(rawVal.substring(1, rawVal.length()-1));
          macros.put(name, val);
      }

"{"                 {  return sym(LBRACE);      }
"}"                 {  return sym(RBRACE);      }
"("                 {  return sym(LPAREN);      }
")"                 {  return sym(RPAREN);      }
"["                 {  return sym(LBRACKET);    }
"]"                 {  return sym(RBRACKET);    }
"<"                 {  return sym(LT);          }
">"                 {  return sym(GT);          }
","                 {  return sym(COMMA);       }
":"                 {  return sym(COLON);       }
";"                 {  return sym(SEMICOLON);   }
"."                 {  return sym(DOT);         }
"?"                 {  return sym(QUESTION);    }
"="                 {  return sym(EQUAL);       }
"@"                 {  return sym(AT);          }

{PathLiteral}       {
                        String raw = yytext();
                        String val = substitute(unescape(raw.substring(1, raw.length()-1)));
                        return sym(PATH_LITERAL, val);
                    }

{StringLiteral}     {
                        String raw = yytext();
                        String val = substitute(unescape(raw.substring(1, raw.length()-1)));
                        return sym(STRING_LITERAL, val);
                    }

{Identifier}        {  return sym(IDENTIFIER, yytext()); }

[0-9]+              {  return sym(INT_LITERAL, Integer.valueOf(yytext())); }

"//"                {  yybegin(LINE_COMMENT); }
"/**"               {  docBuf = new StringBuilder(); blockDepth = 1; yybegin(DOC_COMMENT); }
"/*"                {  blockDepth = 1; yybegin(BLOCK_COMMENT); }

{WhiteSpace}+       { /* skip */ }

.                   {
    throw new RuntimeException(
        "Lexical error: illegal char '" + yytext() +
        "' at line " + (yyline+1) + ", column " + (yycolumn+1));
}
}

<LINE_COMMENT> {
    {LineTerminator} {  yybegin(YYINITIAL); }
    .                { /* skip */ }
    <<EOF>>          { yybegin(YYINITIAL); }
}

<BLOCK_COMMENT> {
    "/*"             { blockDepth++; }
    "*/"             {
                        blockDepth--;
                        if (blockDepth == 0) yybegin(YYINITIAL);
                     }
    {LineTerminator}|. { /* skip */ }
    <<EOF>>          {
        throw new RuntimeException("Unterminated block comment (/*) at EOF");
    }
}

<DOC_COMMENT> {
    "/*"             { blockDepth++; docBuf.append("/*"); }
    "*/"             {
                        blockDepth--;
                        if (blockDepth == 0) {
                            String text = docBuf.toString();
                            yybegin(YYINITIAL);
                            return sym(JAVA_DOC, text);
                        } else docBuf.append("*/");
                     }
    {LineTerminator}|. { docBuf.append(yytext()); }
    <<EOF>>          {
        throw new RuntimeException("Unterminated doc comment (/**) at EOF");
    }
}
