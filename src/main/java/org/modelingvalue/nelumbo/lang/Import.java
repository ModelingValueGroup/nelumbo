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

import java.io.Serial;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.AstElement;
import org.modelingvalue.nelumbo.ConstructionReason;
import org.modelingvalue.nelumbo.KnowledgeBase;
import org.modelingvalue.nelumbo.NelumboConstructor;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.collections.NList;
import org.modelingvalue.nelumbo.syntax.ParseContext;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Token;

public final class Import extends Node {
    @Serial
    private static final long serialVersionUID = 4184295220819695199L;

    @NelumboConstructor
    public Import(Functor functor, List<AstElement> elements, Object... args) {
        super(functor, elements, args);
    }

    @Override
    public Node init(KnowledgeBase knowledgeBase, ParseContext ctx, ConstructionReason reason) throws ParseException {
        if (reason == ConstructionReason.parsing) {
            List<AstElement> elements = astElements();
            Functor functor = functor();
            NList roots = new NList(elements.sublist(0, 1), Type.ROOT);
            StringBuilder sb = new StringBuilder();
            List<AstElement> el = List.of();
            for (int i = 1; i <= elements.size(); i++) {
                Token t = i < elements.size() ? (Token) elements.get(i) : null;
                if (t == null || t.text().equals(",")) {
                    Import ip = new Import(functor, el, sb.toString());
                    roots = new NList(List.of(), roots, ip);
                    if (t != null) {
                        roots = roots.setAstElements(roots.astElements().add(t));
                    }
                    el = List.of();
                    sb = new StringBuilder();
                    knowledgeBase.doImport(ip.name(), ip);
                } else {
                    sb.append(t.text());
                    el = el.add(t);
                }
            }
            return roots;
        }
        return this;
    }

    private Import(Object[] array, List<AstElement> elements, Import declaration) {
        super(array, elements, declaration);
    }

    @Override
    protected Import struct(Object[] array, List<AstElement> elements, Node declaration) {
        return new Import(array, elements, (Import) declaration);
    }

    @Override
    public Import set(int i, Object... a) {
        return (Import) super.set(i, a);
    }

    public String name() {
        return (String) get(0);
    }

}
