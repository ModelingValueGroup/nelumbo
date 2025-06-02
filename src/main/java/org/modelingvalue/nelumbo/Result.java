//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
//                                                                                                                       ~
//  Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in       ~
//  compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0   ~
//  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on  ~
//  an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the   ~
//  specific language governing permissions and limitations under the License.                                           ~
//                                                                                                                       ~
//  Maintainers:                                                                                                         ~
//      Wim Bast, Tom Brus                                                                                               ~
//                                                                                                                       ~
//  Contributors:                                                                                                        ~
//      Ronald Krijgsheld ✝, Arjan Kok, Carel Bast                                                                       ~
// --------------------------------------------------------------------------------------------------------------------- ~
//  In Memory of Ronald Krijgsheld, 1972 - 2023                                                                          ~
//      Ronald was suddenly and unexpectedly taken from us. He was not only our long-term colleague and team member      ~
//      but also our friend. "He will live on in many of the lines of code you see below."                               ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.Set;
import org.modelingvalue.nelumbo.Logic.Predicate;
import org.modelingvalue.nelumbo.impl.InferResult;
import org.modelingvalue.nelumbo.impl.PredicateImpl;
import org.modelingvalue.nelumbo.impl.StructureImpl;

public final class Result {

    private final InferResult inferResult;

    public Result(InferResult inferResult) {
        this.inferResult = inferResult;
    }

    public Result(Set<Predicate<?>> facts, boolean completeFacts, Set<Predicate<?>> falsehoods, boolean completeFalsehoods) {
        this.inferResult = InferResult.of(unproxy(facts), completeFacts, unproxy(falsehoods), completeFalsehoods, Set.of());
    }

    private static Set<PredicateImpl<?>> unproxy(Set<Predicate<?>> set) {
        return set.replaceAll(StructureImpl::unproxy);
    }

    public Set<Predicate<?>> facts() {
        return inferResult.facts().replaceAll(PredicateImpl::proxyWithVariables);
    }

    public Set<Predicate<?>> falsehoods() {
        return inferResult.falsehoods().replaceAll(PredicateImpl::proxyWithVariables);
    }

    public boolean completeFacts() {
        return inferResult.completeFacts();
    }

    public boolean completeFalsehoods() {
        return inferResult.completeFalsehoods();
    }

    @Override
    public int hashCode() {
        return inferResult.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof Result)) {
            return false;
        } else {
            Result other = (Result) obj;
            return inferResult.equals(other.inferResult);
        }
    }

    @Override
    public String toString() {
        return inferResult.toString();
    }
}
