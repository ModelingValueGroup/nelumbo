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

import static org.eclipse.lsp4j.SemanticTokenTypes.*;
import static org.modelingvalue.nelumbo.syntax.TokenType.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.modelingvalue.nelumbo.syntax.TokenType;

public final class LspTokenMapping {
    private static List<Mapping> makeMappings() {
        return List.of(//
                new Mapping(SINGLEQUOTE, null), //
                new Mapping(SEMICOLON, null), //
                new Mapping(COMMA, null), //
                new Mapping(LEFT, null), //
                new Mapping(RIGHT, null), //
                new Mapping(STRING, String), //
                new Mapping(NUMBER, Number), //
                new Mapping(DECIMAL, Number), //
                new Mapping(NAME, Property), //
                new Mapping(TYPE, Type), //
                new Mapping(META_OPERATOR, Type), //
                new Mapping(OPERATOR, Operator), //
                new Mapping(NEWLINE, null), //
                new Mapping(HSPACE, null), //
                new Mapping(END_LINE_COMMENT, Comment), //
                new Mapping(IN_LINE_COMMENT, Comment), //
                new Mapping(ERROR, null), //
                new Mapping(BEGINOFFILE, null), //
                new Mapping(ENDOFFILE, null), //
                new Mapping(ENDOFLINE, null), //
                new Mapping(VARIABLE, Variable), //
                new Mapping(KEYWORD, Keyword)//
        );
    }

    private record Mapping(TokenType tokenType, String lspTokenType, String... lspTokenModifiers) {
    }

    private static final List<Mapping> MAPPINGS      = makeMappings();
    private static final List<String>  TYPE_LIST     = makeTypeList();
    private static final List<String>  MODIFIER_LIST = makeModifierList();
    private static final int[]         TYPE_MAP      = makeTypeIndexTable();
    private static final int[]         MODIFIER_MAP  = makeModifierMaskTable();

    public static int toLspTokenType(TokenType type) {
        return TYPE_MAP[type.ordinal()];
    }

    public static int toLspTokenModifier(TokenType type) {
        return MODIFIER_MAP[type.ordinal()];
    }

    public static List<String> lspTypes() {
        return TYPE_LIST;
    }

    public static List<String> lspModifiers() {
        return MODIFIER_LIST;
    }

    public static List<TokenType> tokenTypes() {
        return MAPPINGS.stream()//
                .map(Mapping::tokenType)//
                .distinct()//
                .filter(Objects::nonNull)//
                .toList();
    }

    //========================================================================================================================

    private static List<String> makeTypeList() {
        return MAPPINGS.stream()//
                .map(Mapping::lspTokenType)//
                .distinct()//
                .filter(Objects::nonNull)//
                .toList();
    }

    private static List<String> makeModifierList() {
        return MAPPINGS.stream()//
                .flatMap(mapping -> Arrays.stream(mapping.lspTokenModifiers()))//
                .distinct()//
                .toList();
    }

    private static int[] makeTypeIndexTable() {
        int[] table = new int[TokenType.values().length];
        for (Mapping mapping : MAPPINGS) {
            int i = mapping.tokenType().ordinal();
            String t = mapping.lspTokenType();
            table[i] = t == null ? -1 : TYPE_LIST.indexOf(t);
        }
        return table;
    }

    private static int[] makeModifierMaskTable() {
        int[] table = new int[TokenType.values().length];
        for (Mapping mapping : MAPPINGS) {
            int i = mapping.tokenType().ordinal();
            Arrays.stream(mapping.lspTokenModifiers()).forEach(m -> table[i] |= 1 << MODIFIER_LIST.indexOf(m));
        }
        return table;
    }
}
