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
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;
import org.modelingvalue.nelumbo.syntax.Patterns;
import org.modelingvalue.nelumbo.syntax.TokenType;

public abstract class Pattern extends Node {
    @Serial
    private static final long serialVersionUID = -1788203180486332564L;

    public static AlternationPattern a(Pattern... options) {
        return a(List.of(), options);
    }

    public static NodeTypePattern n(Type nodeType, Integer precedence) {
        return n(List.of(), nodeType, precedence);
    }

    public static OptionalPattern o(Pattern optional) {
        return o(List.of(), optional);
    }

    public static RepetitionPattern r(Pattern repeated) {
        return r(List.of(), repeated);
    }

    public static SequencePattern s(Pattern... elements) {
        return s(List.of(), elements);
    }

    public static TokenTextPattern t(String tokenText) {
        return t(List.of(), tokenText);
    }

    public static TokenTypePattern t(TokenType tokenType) {
        return t(List.of(), tokenType);
    }

    public static AlternationPattern a(List<AstElement> ast, Pattern... options) {
        return new AlternationPattern(Type.PATTERN, ast, List.of(options));
    }

    public static NodeTypePattern n(List<AstElement> ast, Type nodeType, Integer precedence) {
        return new NodeTypePattern(Type.PATTERN, ast, nodeType, precedence);
    }

    public static OptionalPattern o(List<AstElement> ast, Pattern optional) {
        return new OptionalPattern(Type.PATTERN, ast, optional);
    }

    public static RepetitionPattern r(List<AstElement> ast, Pattern repeated) {
        return new RepetitionPattern(Type.PATTERN, ast, repeated);
    }

    public static SequencePattern s(List<AstElement> ast, Pattern... elements) {
        return new SequencePattern(Type.PATTERN, ast, List.of(elements));
    }

    public static TokenTextPattern t(List<AstElement> ast, String tokenText) {
        return new TokenTextPattern(Type.PATTERN, ast, tokenText);
    }

    public static TokenTypePattern t(List<AstElement> ast, TokenType tokenType) {
        return new TokenTypePattern(Type.PATTERN, ast, tokenType);
    }

    protected Pattern(Type type, List<AstElement> ast, Object... args) {
        super(type, ast, args);
    }

    protected Pattern(Object[] args) {
        super(args);
    }

    @Override
    protected abstract Pattern struct(Object[] array);

    public abstract Patterns patterns(Patterns nextPatterns, NodeTypePattern left);

    public String name() {
        return "";
    }

    public List<Type> args() {
        return List.of();
    }

    public Pattern setPresedence(List<Integer> precedence, int[] p) {
        return this;
    }

    @Override
    public Pattern set(int i, Object... a) {
        return (Pattern) super.set(i, a);
    }

}
