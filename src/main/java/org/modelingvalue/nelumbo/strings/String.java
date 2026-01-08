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

package org.modelingvalue.nelumbo.strings;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Terminal;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class String extends Terminal {

    @Serial
    private static final long             serialVersionUID = 8360866611309554234L;

    private static final java.lang.String DELIM            = "\"";

    private static Functor                FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(String.class, f -> FUNCTOR = f);
    }

    public String(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, parse((java.lang.String) args[0]));
    }

    private String(Functor functor, List<AstElement> elements, java.lang.String val) {
        super(functor, elements, val);
    }

    public static String of(java.lang.String val) {
        return new String(FUNCTOR, List.of(), val);
    }

    public static java.lang.String strip(java.lang.String val) {
        return val != null && val.startsWith(DELIM) ? val.substring(1, val.length() - 1) : null;
    }

    private static java.lang.String parse(java.lang.String string) {
        return strip(string);
    }

    private String(Object[] array, String declaration) {
        super(array, declaration);
    }

    @Override
    protected Terminal struct(Object[] array, Node declaration) {
        return new String(array, (String) declaration);
    }

    @Override
    public String set(int i, Object... a) {
        return (String) super.set(i, a);
    }

    public java.lang.String value() {
        return (java.lang.String) get(0);
    }

    @Override
    public java.lang.String toString(TokenType[] previous) {
        return DELIM + value() + DELIM;
    }

}
