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

package org.modelingvalue.nelumbo;

import org.modelingvalue.collections.List;
import org.modelingvalue.nelumbo.syntax.Token;

@SuppressWarnings("unused")
public class U {
    public static final String COLOR_PRE  = "\u001B[";
    public static final String WHITE_TEXT = COLOR_PRE + "37m";
    public static final String BLACK_TEXT = COLOR_PRE + "30m";
    public static final String RESET      = COLOR_PRE + "0m";

    public static int numLines(String s) {
        int length = 0;
        if (s != null && !s.isEmpty()) {
            length++;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '\n') {
                    length++;
                }
            }
        }
        return length;
    }

    public static String traceable(String s) {
        return s//
                .replaceAll(" ", "\\\\.")//
                .replaceAll("\n", "\\\\n")//
                .replaceAll("\r", "\\\\r");

    }

    public static void printKnowledgeBase(String msg, boolean withTokens) {
        if (java.lang.Boolean.getBoolean("VERBOSE_TESTS")) {
            System.out.println();
            System.out.printf("%s %-99s%s%n", colorCode(42), msg + ":", colorCode(0));
            KnowledgeBase.CURRENT.get().print(System.out, withTokens);
        }
    }

    public static void printTokens(String msg, List<Token> tokens) {
        if (java.lang.Boolean.getBoolean("VERBOSE_TESTS")) {
            System.out.println(msg + ":");
            for (Token token : tokens) {
                System.out.println("    Token: " + token);
            }
        }
    }

    public static void printResults(List<Node> roots) {
        if (java.lang.Boolean.getBoolean("VERBOSE_TESTS")) {
            for (Node root : roots) {
                if (root instanceof Query query) {
                    System.out.println(query.inferResult());
                }
            }
        }
    }

    public static void printNode(String msg, List<Node> nodes) {
        if (java.lang.Boolean.getBoolean("VERBOSE_TESTS")) {
            System.out.println();
            System.out.printf("%s %-99s%s%n", colorCode(42), msg + ":", colorCode(0));
            for (Node node : nodes) {
                System.out.printf("    %s%-96s%s%n", colorCode(46), node.toString(), colorCode(0));
                printTokens("    :::tokens::", node.tokens());
            }
        }
    }

    public static String colorCode(int code) {
        if (code == 0) {
            return RESET;
        }
        String textCode = "";
        if ((40 <= code && code <= 47) || (100 <= code && code <= 107)) {
            // bg color
            textCode = code == 40 || code == 100 ? WHITE_TEXT : BLACK_TEXT;
        }
        return COLOR_PRE + code + "m" + textCode;
    }
}
