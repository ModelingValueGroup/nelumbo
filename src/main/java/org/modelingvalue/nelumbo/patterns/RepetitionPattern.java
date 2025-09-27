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

package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.Patterns;

public class RepetitionPattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 7257418785045060245L;

    public RepetitionPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected RepetitionPattern(Object[] args) {
        super(args);
    }

    @Override
    protected RepetitionPattern struct(Object[] array) {
        return new RepetitionPattern(array);
    }

    public Pattern repeated() {
        return (Pattern) get(0);
    }

    @Override
    public List<Type> args() {
        return repeated().args().map(t -> t.list()).asList();
    }

    @Override
    public String toString() {
        return "r(" + repeated() + ")";
    }

    @Override
    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        return set(0, repeated().setPresedence(precedence, p));
    }

    @Override
    public Patterns patterns(Patterns nextPatterns, NodeTypePattern left) {
        Integer leftPrecedence = left != null ? left.leftPrecedence() : null;
        return repeated().patterns(new Patterns(this), left).merge(new Patterns(this, leftPrecedence)).merge(nextPatterns);
    }
}
