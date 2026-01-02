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

import java.util.Scanner;

import org.modelingvalue.nelumbo.integers.Integer;
import org.modelingvalue.nelumbo.syntax.ParseException;
import org.modelingvalue.nelumbo.syntax.Parser;

public final class Main {

    private final static String READ  = "nelumbo: ";
    private final static String WRITE = " result: ";
    private final static String ERROR = "  error: ";

    private Main() {
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        scanner.useDelimiter("\r\n|[\n\r\u2028\u2029\u0085]");
        KnowledgeBase.BASE.run(() -> {
            try {
                Parser.parse(Integer.class, "integers.nl");
            } catch (ParseException e) {
                System.err.println(ERROR + e.getMessage());
            }
            System.out.print(READ);
            String line = scanner.next();
            while (line != null) {
                try {
                    for (Node root : Parser.parse(line)) {
                        if (root instanceof Query query) {
                            System.out.println(WRITE + query.inferResult());
                        }
                    }
                } catch (ParseException e) {
                    System.err.println(ERROR + e.getShortMessage());
                }
                System.out.print(READ);
                line = scanner.next();
            }
        });
    }

}
