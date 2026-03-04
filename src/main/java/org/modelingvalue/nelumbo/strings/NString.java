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
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class NString extends Node {
    @Serial
    private static final long   serialVersionUID = 8360866611309554234L;

    private static final String DELIM            = "\"";

    private static Functor      FUNCTOR;

    static {
        KnowledgeBase.registerFunctorSetter(NString.class, f -> FUNCTOR = f);
    }

    @NelumboConstructor
    public NString(Functor functor, List<AstElement> elements, Object[] args) {
        super(functor, elements, parse((String) args[0]));
    }

    private NString(Functor functor, List<AstElement> elements, String val) {
        super(functor, elements, val);
    }

    public static NString of(String val) {
        return new NString(FUNCTOR, List.of(), val);
    }

    public static String strip(String val) {
        return val != null && val.startsWith(DELIM) ? val.substring(1, val.length() - 1) : null;
    }

    private static String parse(String string) {
        return strip(string);
    }

    private NString(Object[] array, List<AstElement> elements, NString declaration) {
        super(array, elements, declaration);
    }

    @Override
    protected NString struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new NString(array, elements, (NString) declaration);
    }

    @Override
    public NString set(int i, Object... a) {
        return (NString) super.set(i, a);
    }

    public String value() {
        return (String) get(0);
    }

    @Override
    public String toString(TokenType[] previous) {
        return DELIM + value() + DELIM;
    }

}
