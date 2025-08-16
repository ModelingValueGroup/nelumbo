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

package org.modelingvalue.nelumbo;

import java.lang.Boolean;
import java.util.Arrays;
import java.util.Collection;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.syntax.Token;

public class U {
    public static String traceable(String s) {
        return s//
                .replaceAll(" ", "\\\\.")//
                .replaceAll("\n", "\\\\n")//
                .replaceAll("\r", "\\\\r");

    }

    public static void printKnowledgeBase(String msg, boolean withTokens) {
        if (Boolean.getBoolean("VERBOSE_TESTS")) {
            System.out.println(msg + ":");
            KnowledgeBase.CURRENT.get().print(System.out, withTokens);
        }
    }

    public static void printTokens(String msg, Collection<Token> tokens) {
        if (Boolean.getBoolean("VERBOSE_TESTS")) {
            System.out.println(msg + ":");
            for (Token token : tokens) {
                System.out.println("    Token: " + token);
            }
        }
    }

    public static void printResults(List<Node> roots) {
        if (Boolean.getBoolean("VERBOSE_TESTS")) {
            for (Node root : roots) {
                if (root.type().equals(Type.RESULT)) {
                    System.out.println(root.toString(1));
                }
            }
        }
    }

    public static void printNode(String msg, List<Node> roots) {
        if (Boolean.getBoolean("VERBOSE_TESTS")) {
            System.out.println(msg + ":");
            System.err.println("\u001B[42m" + " ".repeat(80) + "\u001B[0m");
            for (Node root : roots) {
                System.out.println("  - " + root.toString());
                printTokens("    :::tokens::", Arrays.asList(root.tokens()));
                System.err.println("\u001B[42m" + " ".repeat(80) + "\u001B[0m");
            }
        }
    }
}
