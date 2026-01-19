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

package org.modelingvalue.nelumbo.syntax;

import org.modelingvalue.collections.List;
import org.modelingvalue.collections.mutable.MutableList;
import org.modelingvalue.nelumbo.Evaluatable;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.U;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

public class ParserResult implements ParseExceptionHandler {

    private final TokenizerResult             tokenizerResult;
    private final boolean                     throwing;
    private final MutableList<ParseException> exceptions;

    private Node                              root;

    public ParserResult(TokenizerResult tokenizerResult, boolean throwing) {
        this.tokenizerResult = tokenizerResult;
        this.throwing = throwing;
        this.exceptions = MutableList.of(List.of());
    }

    public List<Node> roots() {
        return root instanceof NList listRoot ? listRoot.elementsFlattened() : root != null ? List.of(root) : List.of();
    }

    public Node root() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }

    @Override
    public void addException(ParseException exception) throws ParseException {
        if (throwing) {
            throw exception;
        }
        this.exceptions.add(exception);
    }

    @Override
    public List<ParseException> exceptions() {
        return exceptions.toImmutable();
    }

    public void throwException() throws ParseException {
        if (!exceptions.isEmpty()) {
            throw exceptions().first();
        }
    }

    public void evaluate() throws ParseException {
        if (exceptions.isEmpty()) {
            KnowledgeBase knowledgeBase = KnowledgeBase.CURRENT.get();
            for (Node root : roots()) {
                if (root instanceof Evaluatable eval) {
                    eval.evaluate(knowledgeBase, this);
                }
            }
        }
    }

    public void print() {
        for (ParseException exc : exceptions()) {
            System.out.println(exc);
        }
        for (Node root : roots()) {
            System.out.println(root);
        }
    }

    public void checkAssertions() {
        if (false && tokenizerResult != null && root != null && U.areAssertsEnabled()) {
            StringBuffer sb = new StringBuffer();
            root.deparse(sb);
            String result = sb.toString();
            String input = tokenizerResult.input();
            assert input.equals(result);
        }
    }

}
