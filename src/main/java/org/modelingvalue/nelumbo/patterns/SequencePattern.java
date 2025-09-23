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
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Token;

public class SequencePattern extends Pattern {
    @Serial
    private static final long serialVersionUID = 1477171023667359130L;

    public SequencePattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected SequencePattern(Object[] args) {
        super(args);
    }

    @Override
    protected SequencePattern struct(Object[] array) {
        return new SequencePattern(array);
    }

    @SuppressWarnings("unchecked")
    public List<Pattern> elements() {
        return (List<Pattern>) get(0);
    }

    private Pattern next(List<Pattern> elements, int i, Pattern next) {
        if (i + 1 < elements.size()) {
            Pattern element = elements.get(i + 1);
            if (element instanceof OptionalPattern op) {
                return a(op.optional(), next(elements, i + 1, next));
            }
            if (element instanceof RepetitionPattern rp) {
                return a(rp.repeated(), next(elements, i + 1, next));
            }
            return element;
        }
        return next;
    }

    @Override
    public Token parse(Token token, String group, Parser parser, Pattern next, ParseResult result) throws ParseException {
        List<Pattern> elements = elements();
        for (int i = 0; i < elements.size(); i++) {
            Pattern element = elements.get(i);
            token = element.parse(token, group, parser, next(elements, i, next), result);
        }
        return token;
    }

    @Override
    public boolean peekIs(Token token, Parser parser) throws ParseException {
        List<Pattern> elements = elements();
        return !elements.isEmpty() && elements.first().peekIs(token, parser);
    }

    @Override
    public boolean isFixed(boolean first) {
        return true;
    }

    @Override
    public List<Pattern> fixed(List<Pattern> list, boolean[] stop) {
        for (Pattern element : elements()) {
            if (!element.isFixed(list.isEmpty())) {
                stop[0] = true;
                break;
            }
            list = element.fixed(list, stop);
            if (stop[0]) {
                break;
            }
        }
        return list;
    }

    @Override
    public String name() {
        String name = "";
        for (Pattern element : elements()) {
            name += element.name();
        }
        return name;
    }

    @Override
    public List<Type> args() {
        List<Type> args = List.of();
        for (Pattern element : elements()) {
            Type last = args.last();
            List<Type> l = element.args();
            Type first = l.first();
            if (first != null && last != null && first.isList() && last.equals(first.element())) {
                args = args.removeLast();
            }
            args = args.addAll(l);
        }
        return args;
    }

    @Override
    public String toString() {
        String string = elements().toString();
        return "s(" + string.substring(5, string.length() - 1) + ")";
    }

    @Override
    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        List<Pattern> elements = elements();
        for (int i = 0; i < elements.size(); i++) {
            Pattern pa = elements.get(i);
            Pattern pb = pa.setPresedence(precedence, p);
            if (!pb.equals(pa)) {
                elements = elements.replace(i, pb);
            }
        }
        return set(0, elements);
    }

}
