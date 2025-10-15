//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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
import org.modelingvalue.nelumbo.patterns.Functor;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParserResult;

public final class Fact extends Node implements Evaluatable {
    @Serial
    private static final long serialVersionUID = 6226473785860814115L;

    public Fact(Functor functor, List<AstElement> elements, Object... args) {
        super(functor, elements, args);
    }

    private Fact(Object[] array) {
        super(array);
    }

    @Override
    protected Fact struct(Object[] array) {
        return new Fact(array);
    }

    @Override
    public Fact set(int i, Object... a) {
        return (Fact) super.set(i, a);
    }

    public Predicate predicate() {
        return (Predicate) get(0);
    }

    @Override
    public void evaluate(KnowledgeBase knowledgeBase, ParserResult result) throws ParseException {
        Predicate predicate = predicate();
        if (!predicate.isRelation()) {
            result.addException(new ParseException("Fact " + predicate + " is not a relation.", predicate));
            return;
        }
        if (!predicate.isFullyBound()) {
            result.addException(new ParseException("Fact " + predicate + " has variables.", predicate));
            return;
        }
        knowledgeBase.addFact(predicate);
    }

}
