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

package org.modelingvalue.nelumbo.patterns;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.TokenType;

public abstract class AbstractPattern extends Node {
    @Serial
    private static final long serialVersionUID = -1788203180486332564L;

    public static AlternationPattern a(AbstractPattern... options) {
        return new AlternationPattern(Type.PATTERN, List.of(), List.of(options));
    }

    public static NodeTypePattern n(Type nodeType) {
        return new NodeTypePattern(Type.PATTERN, List.of(), nodeType);
    }

    public static OptionalPattern o(AbstractPattern optional) {
        return new OptionalPattern(Type.PATTERN, List.of(), optional);
    }

    public static RepetitionPattern r(AbstractPattern repeated) {
        return new RepetitionPattern(Type.PATTERN, List.of(), repeated);
    }

    public static SequencePattern s(AbstractPattern... elements) {
        return new SequencePattern(Type.PATTERN, List.of(), List.of(elements));
    }

    public static TokenTextPattern t(String tokenText) {
        return new TokenTextPattern(Type.PATTERN, List.of(), tokenText);
    }

    public static TokenTypePattern t(TokenType tokenType) {
        return new TokenTypePattern(Type.PATTERN, List.of(), tokenType);
    }

    protected AbstractPattern(Type type, List<AstElement> elements, Object... args) {
        super(type, elements, args);
    }

    protected AbstractPattern(Object[] args) {
        super(args);
    }

    @Override
    protected abstract AbstractPattern struct(Object[] array);

    public abstract void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException;

    public boolean peekIs(Parser parser) {
        return false;
    }

    public Patterns patterns(Patterns patterns, int precedence) {
        return patterns;
    }

    public abstract boolean isFixed();

}
