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

package org.modelingvalue.nelumbo.test;

import static org.modelingvalue.nelumbo.U.BLACK_TEXT;
import static org.modelingvalue.nelumbo.U.COLOR_PRE;
import static org.modelingvalue.nelumbo.U.RESET;

import org.modelingvalue.nelumbo.U;

public class Colors {
    private static String colorCodeTestPattern(int code, String s) {
        return U.colorCode(code) + String.format(" %3d %s", code, s) + RESET;
    }

    private static String extended256ColorPattern(int colorCode, boolean isForeground) {
        String code     = isForeground ? "38;5;" + colorCode : "48;5;" + colorCode;
        String textCode = !isForeground && (colorCode < 8 || (16 <= colorCode && colorCode < 232)) ? BLACK_TEXT : "";
        return COLOR_PRE + code + "m" + textCode + String.format(" %3d ", colorCode) + RESET;
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

        // Extended 256-color palette
        System.out.println("\n\n256-Color Palette:");

        // System colors (0-15) - same as standard/bright colors
        System.out.print("    System colors (0-15):\n        ");
        for (int i = 0; i < 16; i++) {
            System.out.print(extended256ColorPattern(i, false));
            if (i == 7) {
                System.out.print("\n        ");
            }
        }
        System.out.println();

        // 216 colors (16-231) - 6x6x6 RGB cube
        System.out.println("    216 RGB colors (16-231):");
        for (int r = 0; r < 6; r++) {
            System.out.print("        ");
            for (int g = 0; g < 6; g++) {
                for (int b = 0; b < 6; b++) {
                    int colorCode = 16 + (r * 36) + (g * 6) + b;
                    System.out.print(extended256ColorPattern(colorCode, false));
                }
                System.out.print(" ");
            }
            System.out.println();
        }

        // Grayscale colors (232-255)
        System.out.print("    Grayscale colors (232-255):\n        ");
        for (int i = 232; i < 256; i++) {
            System.out.print(extended256ColorPattern(i, false));
            if (i == 243) {
                System.out.print("\n        ");
            }
        }
        System.out.println();
    }

}
