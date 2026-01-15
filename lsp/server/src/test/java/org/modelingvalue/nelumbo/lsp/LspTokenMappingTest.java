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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.SemanticTokenModifiers;
import org.eclipse.lsp4j.SemanticTokenTypes;
import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class LspTokenMappingTest {
    @Test
    void testAllAllDefinedTokenTypesAreMapped() {
        List<TokenType> mappedTokenTypes = LspTokenMapping.allDefinedTokenTypes();
        for (TokenType t : TokenType.values()) {
            assertTrue(mappedTokenTypes.contains(t), "Token type " + t + " not mapped in LspTokenMapping");
        }
    }

    @Test
    void testAllLSPTypesAreValid() {
        final Set<String> VALID_TOKEN_TYPES = getStaticStringFields(SemanticTokenTypes.class);
        for (String type : LspTokenMapping.allLSPTypes()) {
            assertTrue(VALID_TOKEN_TYPES.contains(type), "Invalid token type: '" + type + "' (not in SemanticTokenTypes)");
        }
    }

    @Test
    void testAllLSPModifiersAreValid() {
        final Set<String> VALID_TOKEN_MODIFIERS = getStaticStringFields(SemanticTokenModifiers.class);
        for (String modifier : LspTokenMapping.allLSPModifiers()) {
            assertTrue(VALID_TOKEN_MODIFIERS.contains(modifier), "Invalid token modifier: '" + modifier + "' (not in SemanticTokenModifiers)");
        }
    }

    private static Set<String> getStaticStringFields(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields()).filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class).map(f -> {
            try {
                return (String) f.get(null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }
}
