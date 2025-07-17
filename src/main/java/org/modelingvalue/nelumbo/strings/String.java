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

package org.modelingvalue.nelumbo.strings;

import org.modelingvalue.nelumbo.Functor;
import org.modelingvalue.nelumbo.Terminal;

import java.io.Serial;

public final class String extends Terminal {

    @Serial
    private static final long serialVersionUID = 8360866611309554234L;

    private static final java.lang.String DELIM = "\"";

    private static Functor FUNCTOR;

    public String(Functor functor, Object[] args) {
        super(functor, parse((java.lang.String) args[0]));
        if (FUNCTOR == null) {
            FUNCTOR = functor;
        }
    }

    private String(Functor functor, java.lang.String val) {
        super(functor, val);
    }

    public static String of(java.lang.String val) {
        return new String(FUNCTOR, val);
    }

    public static java.lang.String strip(java.lang.String val) {
        return val != null && val.startsWith(DELIM) ? val.substring(1, val.length() - 1) : null;
    }

    private static java.lang.String parse(java.lang.String string) {
        return strip(string);
    }

    private String(Object[] array) {
        super(array);
    }

    @Override
    protected String struct(Object[] array) {
        return new String(array);
    }

    @Override
    public String set(int i, Object... a) {
        return (String) super.set(i, a);
    }

    public java.lang.String value() {
        return (java.lang.String) get(1);
    }

    @Override
    public java.lang.String toString() {
        return DELIM + value() + DELIM;
    }

}
