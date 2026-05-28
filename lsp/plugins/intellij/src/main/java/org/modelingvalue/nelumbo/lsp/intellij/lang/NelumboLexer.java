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

package org.modelingvalue.nelumbo.lsp.intellij.lang;

import com.intellij.lexer.LexerBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.modelingvalue.nelumbo.syntax.Token;
import org.modelingvalue.nelumbo.syntax.Tokenizer;
import org.modelingvalue.nelumbo.syntax.Tokenizer.TokenizerResult;

/**
 * IntelliJ {@link com.intellij.lexer.Lexer} that delegates to the core nelumbo
 * {@link Tokenizer} so PSI tokens match LSP-side tokenization exactly (multi-char
 * operators, comments, decimals etc. each become a single PSI leaf).
 * <p>
 * The tokenizer is run eagerly on {@link #start} over the requested buffer slice; subsequent
 * {@link #advance} calls walk the pre-tokenized list.
 */
public class NelumboLexer extends LexerBase {
    private CharSequence  buffer;
    private int           startOffset;
    private int           bufferEnd;
    private Token[]       tokens = new Token[0];
    private int           index;

    @Override
    public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        this.buffer      = buffer;
        this.startOffset = startOffset;
        this.bufferEnd   = endOffset;
        this.index       = 0;
        this.tokens      = tokenize(buffer, startOffset, endOffset);
    }

    private static Token[] tokenize(CharSequence buffer, int startOffset, int endOffset) {
        String input = buffer.subSequence(startOffset, endOffset).toString();
        try {
            TokenizerResult result = new Tokenizer(input, "<intellij>").tokenize();
            return result.listAll().filter(t -> !t.text().isEmpty()).toArray(Token[]::new);
        } catch (RuntimeException e) {
            return new Token[0];
        }
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public IElementType getTokenType() {
        if (index < tokens.length) {
            return NelumboTokenType.forNelumbo(tokens[index].type());
        }
        // Cover any trailing range the tokenizer didn't produce a token for (e.g. after an
        // ERROR) so IntelliJ's lexer contract holds: every offset must be reachable.
        if (currentOffset() < bufferEnd) {
            return com.intellij.psi.TokenType.BAD_CHARACTER;
        }
        return null;
    }

    @Override
    public int getTokenStart() {
        return currentOffset();
    }

    @Override
    public int getTokenEnd() {
        if (index < tokens.length) {
            return startOffset + tokens[index].indexEnd();
        }
        return bufferEnd;
    }

    @Override
    public void advance() {
        if (index < tokens.length) {
            index++;
        } else {
            // Consume any trailing fallback range in one step.
            index = tokens.length + 1;
        }
    }

    @Override
    public @NotNull CharSequence getBufferSequence() {
        return buffer;
    }

    @Override
    public int getBufferEnd() {
        return bufferEnd;
    }

    private int currentOffset() {
        if (index < tokens.length) {
            return startOffset + tokens[index].index();
        }
        if (tokens.length == 0) {
            return startOffset;
        }
        return startOffset + tokens[tokens.length - 1].indexEnd();
    }
}
