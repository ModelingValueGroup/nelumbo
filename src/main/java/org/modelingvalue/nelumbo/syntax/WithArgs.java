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

package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.struct.impl.StructImpl;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public abstract class WithArgs extends StructImpl {
    private static final long serialVersionUID = -3715793473520345160L;

    public static WithArgs of(ThrowingTriFunction<Token[], List<Node>, Token[], Node> constructor, Type... args) {
        return new WithArgs(args) {
            private static final long serialVersionUID = 4861999019838252134L;

            @Override
            public Node construct(Token[] token, List<Node> args, Token[] end) throws ParseException {
                return constructor.apply(token, args, end);
            }
        };
    }

    public static WithArgs of(ThrowingQuadFunction<Node, Token[], List<Node>, Token[], Node> constructor, Type... args) {
        return new WithArgs(args) {
            private static final long serialVersionUID = -3034299516226643893L;

            @Override
            public Node construct(Node left, Token[] token, List<Node> args, Token[] end) throws ParseException {
                return constructor.apply(left, token, args, end);
            }
        };
    }

    private WithArgs(Type[] args) {
        super(List.of(args));
    }

    @SuppressWarnings("unchecked")
    public List<Type> args() {
        return (List<Type>) get(0);
    }

    public Node construct(Token[] tokens, List<Node> args, Token[] end) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public Node construct(Node left, Token[] tokens, List<Node> args, Token[] end) throws ParseException {
        throw new UnsupportedOperationException();
    }

    public boolean isAssignableFrom(WithArgs sub) {
        return isAssignableFrom(sub.args());
    }

    public boolean isAssignableFrom(List<Type> args) {
        if (args.size() != args().size()) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            if (!args().get(i).isAssignableFrom(args.get(i))) {
                return false;
            }
        }
        return true;
    }

}
