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

public class NodeTypePattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 6828401544789430678L;

    public NodeTypePattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected NodeTypePattern(Object[] args) {
        super(args);
    }

    @Override
    protected NodeTypePattern struct(Object[] array) {
        return new NodeTypePattern(array);
    }

    public Type nodeType() {
        return (Type) get(0);
    }

    public Integer leftPrecedence() {
        Integer precedence = (Integer) get(1);
        return precedence != null ? precedence : Integer.MAX_VALUE;
    }

    public Integer innerPrecedence() {
        Integer precedence = (Integer) get(1);
        return precedence != null ? precedence : Integer.MIN_VALUE;
    }

    @Override
    public Patterns patterns(Patterns nextPatterns, NodeTypePattern left) {
        Integer leftPrecedence = left != null && left != this ? left.leftPrecedence() : null;
        Integer innerPrecedence = left == null || left != this ? innerPrecedence() : null;
        return new Patterns(nodeType(), nextPatterns, leftPrecedence, innerPrecedence);
    }

    @Override
    public List<Type> args() {
        return List.of(nodeType());
    }

    @Override
    public String toString() {
        Integer precedence = (Integer) get(1);
        return "n(" + nodeType() + (precedence != null ? precedence : "") + ")";
    }

    @Override
    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        int i = p[0];
        if (i < precedence.size() - 1) {
            p[0]++;
        }
        return set(1, precedence.get(i));
    }

}
