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
        if (java.lang.Boolean.getBoolean("VERBOSE_TESTS")) {
            System.out.println();
            System.out.printf("%s %-99s%s%n", Colors.code(42), msg + ":", Colors.code(0));
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
                if (root.type().equals(Type.RESULT)) {
                    System.out.println(root.toString(1));
                }
            }
        }
    }

    public static void printNode(String msg, List<Node> nodes) {
        if (java.lang.Boolean.getBoolean("VERBOSE_TESTS")) {
            System.out.println();
            System.out.printf("%s %-99s%s%n", Colors.code(42), msg + ":", Colors.code(0));
            for (Node node : nodes) {
                System.out.printf("    %s%-96s%s%n", Colors.code(46), node.toString(), Colors.code(0));
                printTokens("    :::tokens::", node.tokens());
            }
        }
    }

    public static class Colors {
        private static final String COLOR      = "\u001B[";
        private static final String RESET      = COLOR + "0m";
        private static final String BLACK_TEXT = COLOR + "30m";
        private static final String WHITE_TEXT = COLOR + "37m";

        public static String code(int code) {
            if (code == 0) {
                return RESET;
            }
            String textCode = "";
            if ((40 <= code && code <= 47) || (100 <= code && code <= 107)) {
                // bg color
                textCode = code == 40 || code == 100 ? WHITE_TEXT : BLACK_TEXT;
            }
            return COLOR + code + "m" + textCode;
        }

        private static String colorCodeTestPattern(int code, String s) {
            return code(code) + String.format(" %3d %s", code, s) + RESET;
        }

        public static void main(String[] args) {
            System.out.println("VT100 Color Test Pattern:");

            // Standard colors (30-37 foreground, 40-47 background)
            System.out.print("    Standard colors:\n        ");
            for (int i = 30; i < 38; i++) {
                System.out.print(colorCodeTestPattern(i, ""));
            }
            System.out.println();
            System.out.print("    Background:\n        ");
            for (int i = 40; i < 48; i++) {
                System.out.print(colorCodeTestPattern(i, ""));
            }
            System.out.println();
            // Bright colors (90-97 foreground, 100-107 background)
            System.out.print("    Bright colors:\n        ");
            for (int i = 90; i < 98; i++) {
                System.out.print(colorCodeTestPattern(i, ""));
            }
            System.out.println();
            System.out.print("    Bright background:\n        ");
            for (int i = 100; i < 108; i++) {
                System.out.print(colorCodeTestPattern(i, ""));
            }

            // Text styles
            System.out.println("\n\nText styles:");
            System.out.println(colorCodeTestPattern(1, "Bold"));
            System.out.println(colorCodeTestPattern(3, "Italic"));
            System.out.println(colorCodeTestPattern(4, "Underline"));
            System.out.println(colorCodeTestPattern(7, "Inverse"));
            System.out.println(colorCodeTestPattern(9, "Strikethrough"));
        }

    }

}
