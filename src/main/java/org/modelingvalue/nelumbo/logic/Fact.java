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

package org.modelingvalue.nelumbo.logic;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseExceptionHandler;

public final class Fact extends Node implements Evaluatable {
    @Serial
    private static final long serialVersionUID = 6226473785860814115L;

    @NelumboConstructor
    public Fact(Functor functor, List<AstElement> elements, Object... args) {
        super(functor, elements, args);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx) throws ParseException {
        NList facts = new NList(astElements().sublist(0, 1), Type.ROOT);
        for (int i = 0; i < length(); i++) {
            Predicate pred = getVal(i);
            Fact fact = new Fact(functor(), List.of(pred), pred);
            facts = new NList(List.of(), facts, fact);
        }
        return facts;
    }

    private Fact(Object[] array, List<AstElement> elements, Fact declaration) {
        super(array, elements, declaration);
    }

    @Override
    protected Fact struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new Fact(array, elements, (Fact) declaration);
    }

    @Override
    public Fact set(int i, Object... a) {
        return (Fact) super.set(i, a);
    }

    public Predicate predicate() {
        return (Predicate) get(0);
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, ParseExceptionHandler handler) throws ParseException {
        Predicate predicate = predicate();
        if (!predicate.isFact()) {
            Functor nodeFunctor = predicate.functor();
            Functor literalFunctor = knowledgeBase.literal(nodeFunctor);
            if (literalFunctor != null) {
                predicate = predicate.setFunctor(literalFunctor);
            }
        }
        if (!predicate.isFact()) {
            handler.addException(new ParseException("The type of " + predicate + " is not FactType.", predicate));
            return;
        }
        if (!predicate.isFullyBound()) {
            handler.addException(new ParseException("Fact " + predicate + " has variables.", predicate));
            return;
        }
        knowledgeBase.addFact(predicate);
    }

}
