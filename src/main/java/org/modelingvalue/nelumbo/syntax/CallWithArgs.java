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

import java.text.ParseException;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.Type;

public abstract class CallWithArgs {

    private final String     name;
    private final List<Type> args;

    public CallWithArgs(String name, Type... args) {
        this.name = name;
        this.args = List.of(args);
    }

    public String name() {
        return name;
    }

    public List<Type> args() {
        return args;
    }

    public abstract Node construct(Token token, List<Node> args) throws ParseException;

    public static CallWithArgs of(String text, ThrowingBiFunction<Token, List<Node>, Node> constructor, Type... args) {
        return new CallWithArgs(text, args) {
            @Override
            public Node construct(Token token, List<Node> args) throws ParseException {
                return constructor.apply(token, args);
            }
        };
    }

    public boolean isAssignableFrom(CallWithArgs sub) {
        if (!sub.name().equals(name()) || sub.args().size() != args().size()) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            if (!args().get(i).isAssignableFrom(sub.args().get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean isAssignableFrom(List<Node> args) {
        if (args.size() != args().size()) {
            return false;
        }
        for (int i = 0; i < args.size(); i++) {
            if (!args().get(i).isAssignableFrom(args.get(i).type())) {
                return false;
            }
        }
        return true;
    }

}
