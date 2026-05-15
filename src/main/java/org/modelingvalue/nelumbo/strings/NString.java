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
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.NelumboFunctorField;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.lang.Functor;
import org.modelingvalue.nelumbo.lang.FunctorOrType;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.TokenType;

public final class NString extends Node {
    @Serial
    private static final long serialVersionUID = 8360866611309554234L;

    private static final String DELIM = "\"";

    @NelumboFunctorField
    private static Functor FUNCTOR;

    @NelumboConstructor
    public NString(FunctorOrType functorOrType, List<AstElement> elements, Node declaration, Object... args) {
        super(functorOrType, elements, declaration, args);
    }

    private NString(Functor functor, List<AstElement> elements, String val) {
        super(functor, elements, null, val);
    }

    public static NString of(String val) {
        return new NString(FUNCTOR, List.of(), val);
    }

    private static String strip(String val) {
        return val.substring(1, val.length() - 1);
    }

    @Override
    protected NString set(FunctorOrType functorOrType, List<AstElement> elements, Node declaration, Object[] args) {
        return new NString(functorOrType, elements, declaration, args);
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

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing && get(0) instanceof String val && val.startsWith(DELIM)) {
            return set(0, strip(val));
        }
        return this;

    }

}
