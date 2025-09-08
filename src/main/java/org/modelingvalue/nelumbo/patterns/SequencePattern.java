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
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.ParseResult;
import org.modelingvalue.nelumbo.syntax.Parser;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.TokenType;

public class SequencePattern extends AbstractPattern {
    @Serial
    private static final long                   serialVersionUID    = 1477171023667359130L;

    private static final List<TokenTypePattern> TOKEN_TYPE_PATTERNS = List.of(TokenType.values()).exclude(TokenType::skip).map(TokenTypePattern::of).asList();

    private static final List<AbstractPattern>  ALL_PATTERNS        = List.<AbstractPattern> of(RepetitionPattern.PATTERN).addAll(TOKEN_TYPE_PATTERNS);

    public static final AbstractPattern         PATTERN             = RepetitionPattern.of(AlternationPattern.of(ALL_PATTERNS.toArray(i -> new AbstractPattern[i])));

    public static SequencePattern of(AbstractPattern... elements) {
        return new SequencePattern(Type.PATTERN, Token.EMPTY, List.of(elements));
    }

    public SequencePattern(Type type, Token[] tokens, Object... args) {
        super(type, tokens, args);
    }

    protected SequencePattern(Object[] args, int start) {
        super(args, start);
    }

    @Override
    protected SequencePattern struct(Object[] array, int start) {
        return new SequencePattern(array, start);
    }

    @SuppressWarnings("unchecked")
    public List<AbstractPattern> elements() {
        return (List<AbstractPattern>) get(0);
    }

    @Override
    public void parse(Type expected, int precedence, Parser parser, AbstractPattern next, ParseResult result) throws ParseException {
        List<AbstractPattern> elements = elements();
        for (int i = 0; i < elements.size(); i++) {
            AbstractPattern pattern = elements.get(i);
            pattern.parse(expected, precedence, parser, i + 1 < elements.size() ? elements.get(i + 1) : next, result);
        }
    }

    @Override
    public boolean peekIs(Parser parser) {
        List<AbstractPattern> elements = elements();
        return !elements.isEmpty() && elements.first().peekIs(parser);
    }

    @Override
    public Patterns patterns(Patterns patterns, int precedence) {
        List<AbstractPattern> elements = elements();
        int max = elements.size();
        for (int i = 0; i < max; i++) {
            if (!elements.get(i).isFixed()) {
                max = i;
                break;
            }
        }
        for (int i = max; i > 0; i--) {
            patterns = elements.get(i).patterns(patterns, precedence);
        }
        return patterns;
    }

    @Override
    public boolean isFixed() {
        return true;
    }

}
