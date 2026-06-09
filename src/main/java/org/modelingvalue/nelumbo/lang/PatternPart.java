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

package org.modelingvalue.nelumbo.lang;

import static org.modelingvalue.nelumbo.ConstructionReason.transforming;
import static org.modelingvalue.nelumbo.patterns.Pattern.t;

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.patterns.Pattern;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;

public final class PatternPart extends Node {
    @Serial
    private static final long serialVersionUID = 7203022330680664829L;

    @NelumboConstructor
    public PatternPart(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing) {
            if (length() == 0) {
                PatternPart patternPart = (PatternPart) functor().astElements().get(0);
                return patternPart.setAstElements(astElements());
            }
            String name = name();
            List<AstElement> pttrn = getVal(1);
            Pattern pattern = Pattern.pattern(pttrn);
            Node self = setArgs(name, pattern);
            Functor functor = Functor.of(List.of(self), t(name), Type.PATTERN_PART, Type.NAMESPACE, PatternPart.class,
                    null);
            functor.init(knowledgeBase, ctx, transforming);
            return self;
        }
        return this;
    }

    public String name() {
        return (String) get(0);
    }

    public Pattern pattern() {
        return (Pattern) get(1);
    }

}
