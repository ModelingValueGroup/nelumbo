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

import static org.eclipse.lsp4j.SemanticTokenModifiers.Static;
import static org.eclipse.lsp4j.SemanticTokenTypes.Comment;
import static org.eclipse.lsp4j.SemanticTokenTypes.Number;
import static org.eclipse.lsp4j.SemanticTokenTypes.Operator;
import static org.eclipse.lsp4j.SemanticTokenTypes.String;
import static org.eclipse.lsp4j.SemanticTokenTypes.Type;
import static org.eclipse.lsp4j.SemanticTokenTypes.Variable;
import static org.modelingvalue.nelumbo.syntax.TokenType.COMMA;
import static org.modelingvalue.nelumbo.syntax.TokenType.DECIMAL;
import static org.modelingvalue.nelumbo.syntax.TokenType.END_LINE_COMMENT;
import static org.modelingvalue.nelumbo.syntax.TokenType.ERROR;
import static org.modelingvalue.nelumbo.syntax.TokenType.HSPACE;
import static org.modelingvalue.nelumbo.syntax.TokenType.IN_LINE_COMMENT;
import static org.modelingvalue.nelumbo.syntax.TokenType.NAME;
import static org.modelingvalue.nelumbo.syntax.TokenType.NEWLINE;
import static org.modelingvalue.nelumbo.syntax.TokenType.NUMBER;
import static org.modelingvalue.nelumbo.syntax.TokenType.OPERATOR;
import static org.modelingvalue.nelumbo.syntax.TokenType.SEMICOLON;
import static org.modelingvalue.nelumbo.syntax.TokenType.STRING;
import static org.modelingvalue.nelumbo.syntax.TokenType.TYPE;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.modelingvalue.nelumbo.syntax.TokenType;

public final class SemanticMapping {
    private record Mapping(TokenType tokenType,
                           String... typesAndModifiers) {
        public String semanticTokenType() {
            return typesAndModifiers.length == 0 ? null : typesAndModifiers[0];
        }

        public Stream<String> semanticTokenModifier() {
            return typesAndModifiers.length == 0 ? Stream.of() : Stream.of(typesAndModifiers).skip(1);
        }
    }

    private static List<Mapping> makeMappings() {
        return List.of(//
                       new Mapping(COMMA),//
                       new Mapping(DECIMAL, Number),//
                       new Mapping(END_LINE_COMMENT, Comment),//
                       new Mapping(ERROR),//
                       new Mapping(HSPACE),//
                       new Mapping(IN_LINE_COMMENT, Comment),//
                       new Mapping(NAME, Variable, Static),//
                       new Mapping(NEWLINE),//
                       new Mapping(NUMBER, Number),//
                       new Mapping(OPERATOR, Operator),//
                       new Mapping(SEMICOLON),//
                       new Mapping(STRING, String),//
                       new Mapping(TYPE, Type)//
                      );
    }

    private static final List<Mapping> MAPPINGS      = makeMappings();
    private static final List<String>  TYPE_LIST     = makeTypeList();
    private static final List<String>  MODIFIER_LIST = makeModifierList();
    private static final int[]         TYPE_MAP      = makeTypeIndexTable();
    private static final int[]         MODIFIER_MAP  = makeModifierMaskTable();

    public static int toSemanticTokenType(TokenType type) {
        return TYPE_MAP[type.ordinal()];
    }

    public static int toSemanticTokenModifier(TokenType type) {
        return MODIFIER_MAP[type.ordinal()];
    }

    public static List<String> allSemanticTypes() {
        return TYPE_LIST;
    }

    public static List<String> allSemanticModifiers() {
        return MODIFIER_LIST;
    }

    //========================================================================================================================

    private static List<String> makeTypeList() {
        return MAPPINGS.stream()//
                       .map(Mapping::semanticTokenType)//
                       .distinct()//
                       .filter(Objects::nonNull).toList();
    }

    private static List<String> makeModifierList() {
        return MAPPINGS.stream()//
                       .flatMap(Mapping::semanticTokenModifier)//
                       .distinct()//
                       .toList();
    }

    private static int[] makeTypeIndexTable() {
        int[] table = new int[TokenType.values().length];
        for (Mapping mapping : MAPPINGS) {
            int i = mapping.tokenType().ordinal();
            table[i] = mapping.semanticTokenType() == null ? -1 : TYPE_LIST.indexOf(mapping.semanticTokenType());
        }
        return table;
    }

    private static int[] makeModifierMaskTable() {
        int[] table = new int[TokenType.values().length];
        for (Mapping mapping : MAPPINGS) {
            int i = mapping.tokenType().ordinal();
            mapping.semanticTokenModifier().forEach(m -> table[i] |= 1 << MODIFIER_LIST.indexOf(m));
        }
        return table;
    }
}
