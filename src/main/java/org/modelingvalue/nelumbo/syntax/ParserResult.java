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

package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.ListNode;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public class ParserResult {

    private final boolean                     throwing;
    private final MutableList<ParseException> exceptions;

    private Node                              root;

    public ParserResult(boolean throwing) {
        this.throwing = throwing;
        this.exceptions = throwing ? null : MutableList.of(List.of());
    }

    public List<Node> roots() {
        return root instanceof ListNode ? ((ListNode) root).elements() : root == null ? List.of() : List.of(root);
    }

    public Node root() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }

    public void addException(ParseException exception) throws ParseException {
        if (throwing) {
            throw exception;
        }
        this.exceptions.add(exception);
    }

    public List<ParseException> exceptions() {
        return exceptions.toImmutable();
    }

    public void throwException() throws ParseException {
        if (!exceptions.isEmpty()) {
            throw exceptions.getFirst();
        }
    }

    public void evaluate() throws ParseException {
        throwException();
        for (Node root : roots()) {
            if (root.type() == Type.QUERY) {
                // TODO
            }
        }
    }

}
