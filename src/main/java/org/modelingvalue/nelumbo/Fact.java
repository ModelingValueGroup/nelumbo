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

package org.modelingvalue.nelumbo;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.logic.Predicate;
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseExceptionHandler;

public final class Fact extends Node implements Evaluatable {
    @Serial
    private static final long serialVersionUID = 6226473785860814115L;

    public Fact(Functor functor, List<AstElement> elements, Object... args) {
        super(functor, elements, args);
    }

    private Fact(Object[] array, Fact declaration) {
        super(array, declaration);
    }

    @Override
    protected Fact struct(Object[] array, Node declaration) {
        return new Fact(array, (Fact) declaration);
    }

    @Override
    public Fact set(int i, Object... a) {
        return (Fact) super.set(i, a);
    }

    public Predicate predicate() {
        return Predicate.predicate((Node) get(0));
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, ParseExceptionHandler handler) throws ParseException {
        Predicate predicate = predicate();
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

    @Override
    public Node init(KnowledgeBase knowledgeBase) throws ParseException {
        Predicate predicate = predicate();
        Functor nodeFunctor = predicate.functor();
        Functor literalFunctor = knowledgeBase.literal(nodeFunctor);
        if (literalFunctor != null) {
            predicate = predicate.setFunctor(literalFunctor);
        }
        if (predicate.isFact() && predicate.isFullyBound()) {
            knowledgeBase.addFact(predicate);
        }
        return this;
    }

}
