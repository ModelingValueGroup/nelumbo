//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo.lsp;

import static org.eclipse.lsp4j.SemanticTokenTypes.Comment;
import static org.eclipse.lsp4j.SemanticTokenTypes.Decorator;
import static org.eclipse.lsp4j.SemanticTokenTypes.Modifier;
import static org.eclipse.lsp4j.SemanticTokenTypes.Number;
import static org.eclipse.lsp4j.SemanticTokenTypes.Operator;
import static org.eclipse.lsp4j.SemanticTokenTypes.Property;
import static org.eclipse.lsp4j.SemanticTokenTypes.String;
import static org.eclipse.lsp4j.SemanticTokenTypes.Type;
import static org.eclipse.lsp4j.SemanticTokenTypes.Variable;
import static org.modelingvalue.nelumbo.syntax.TokenType.BEGINOFFILE;
import static org.modelingvalue.nelumbo.syntax.TokenType.COMMA;
import static org.modelingvalue.nelumbo.syntax.TokenType.DECIMAL;
import static org.modelingvalue.nelumbo.syntax.TokenType.ENDOFFILE;
import static org.modelingvalue.nelumbo.syntax.TokenType.ENDOFLINE;
import static org.modelingvalue.nelumbo.syntax.TokenType.END_LINE_COMMENT;
import static org.modelingvalue.nelumbo.syntax.TokenType.ERROR;
import static org.modelingvalue.nelumbo.syntax.TokenType.HSPACE;
import static org.modelingvalue.nelumbo.syntax.TokenType.IN_LINE_COMMENT;
import static org.modelingvalue.nelumbo.syntax.TokenType.KEYWORD;
import static org.modelingvalue.nelumbo.syntax.TokenType.LEFT;
import static org.modelingvalue.nelumbo.syntax.TokenType.META_OPERATOR;
import static org.modelingvalue.nelumbo.syntax.TokenType.NAME;
import static org.modelingvalue.nelumbo.syntax.TokenType.NEWLINE;
import static org.modelingvalue.nelumbo.syntax.TokenType.NUMBER;
import static org.modelingvalue.nelumbo.syntax.TokenType.OPERATOR;
import static org.modelingvalue.nelumbo.syntax.TokenType.RIGHT;
import static org.modelingvalue.nelumbo.syntax.TokenType.SEMICOLON;
import static org.modelingvalue.nelumbo.syntax.TokenType.SINGLEQUOTE;
import static org.modelingvalue.nelumbo.syntax.TokenType.STRING;
import static org.modelingvalue.nelumbo.syntax.TokenType.TYPE;
import static org.modelingvalue.nelumbo.syntax.TokenType.VARIABLE;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.modelingvalue.nelumbo.syntax.TokenType;

public final class LspTokenMapping {
    private static List<Mapping> makeMapping() {
        return List.of(//
                new Mapping(STRING, String), //
                new Mapping(NUMBER, Number), //
                new Mapping(DECIMAL, Number), //
                new Mapping(NAME, Property), //
                new Mapping(TYPE, Type), //
                new Mapping(META_OPERATOR, Decorator), //
                new Mapping(OPERATOR, Operator), //
                new Mapping(END_LINE_COMMENT, Comment), //
                new Mapping(IN_LINE_COMMENT, Comment), //
                new Mapping(VARIABLE, Variable), //
                new Mapping(KEYWORD, Modifier), //
                //
                // Currently not mapped:
                new Mapping(SINGLEQUOTE, null), //
                new Mapping(SEMICOLON, null), //
                new Mapping(COMMA, null), //
                new Mapping(LEFT, null), //
                new Mapping(RIGHT, null), //
                new Mapping(NEWLINE, null), //
                new Mapping(HSPACE, null), //
                new Mapping(ERROR, null), //
                new Mapping(BEGINOFFILE, null), //
                new Mapping(ENDOFFILE, null), //
                new Mapping(ENDOFLINE, null) //
                      );
    }

    private record Mapping(TokenType tokenType,
                           String lspTokenType,
                           String... lspTokenModifiers) {
    }

    private static final List<Mapping> MAPPING           = makeMapping();
    private static final List<String>  ALL_TYPE_LIST     = makeTypeList();
    private static final List<String>  ALL_MODIFIER_LIST = makeModifierList();
    private static final String[]      TYPE_NAME_MAP     = makeTypeNameTable();
    private static final String[]      MODIFIER_NAME_MAP = makeModifierNameTable();
    private static final int[]         TYPE_NUM_MAP      = makeTypeIndexTable();
    private static final int[]         MODIFIER_MASK_MAP = makeModifierMaskTable();

    public static List<String> allLSPTypes() {
        return ALL_TYPE_LIST;
    }

    public static List<String> allLSPModifiers() {
        return ALL_MODIFIER_LIST;
    }

    public static int toLspTokenTypeNum(TokenType type) {
        return TYPE_NUM_MAP[type.ordinal()];
    }

    public static int toLspTokenModifierMask(TokenType type) {
        return MODIFIER_MASK_MAP[type.ordinal()];
    }

    public static String toLspTokenTypeName(TokenType type) {
        return TYPE_NAME_MAP[type.ordinal()];
    }

    public static String toLspTokenModifierName(TokenType type) {
        return MODIFIER_NAME_MAP[type.ordinal()];
    }

    public static List<TokenType> allDefinedTokenTypes() {
        return MAPPING.stream()//
                       .map(Mapping::tokenType)//
                       .distinct()//
                       .filter(Objects::nonNull)//
                       .toList();
    }

    //========================================================================================================================

    private static List<String> makeTypeList() {
        return MAPPING.stream()//
                       .map(Mapping::lspTokenType)//
                       .distinct()//
                       .filter(Objects::nonNull)//
                       .toList();
    }

    private static List<String> makeModifierList() {
        return MAPPING.stream()//
                       .flatMap(mapping -> Arrays.stream(mapping.lspTokenModifiers()))//
                       .distinct()//
                       .toList();
    }

    private static String[] makeTypeNameTable() {
        String[] table = new String[TokenType.values().length];
        for (Mapping mapping : MAPPING) {
            int    i = mapping.tokenType().ordinal();
            String t = mapping.lspTokenType();
            table[i] = t;
        }
        return table;
    }

    private static String[] makeModifierNameTable() {
        String[] table = new String[TokenType.values().length];
        Arrays.fill(table, "");
        for (Mapping mapping : MAPPING) {
            int i = mapping.tokenType().ordinal();
            Arrays.stream(mapping.lspTokenModifiers()).forEach(m -> table[i] += m + ",");
        }
        return table;
    }

    private static int[] makeTypeIndexTable() {
        int[] table = new int[TokenType.values().length];
        for (Mapping mapping : MAPPING) {
            int    i = mapping.tokenType().ordinal();
            String t = mapping.lspTokenType();
            table[i] = t == null ? -1 : ALL_TYPE_LIST.indexOf(t);
        }
        return table;
    }

    private static int[] makeModifierMaskTable() {
        int[] table = new int[TokenType.values().length];
        for (Mapping mapping : MAPPING) {
            int i = mapping.tokenType().ordinal();
            Arrays.stream(mapping.lspTokenModifiers()).forEach(m -> table[i] |= 1 << ALL_MODIFIER_LIST.indexOf(m));
        }
        return table;
    }
}
