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

package org.modelingvalue.nelumbo;

import java.io.Serial;

public class InconsistencyException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -30585101694801066L;

    private final Rule        rule;
    private final InferResult ruleResult;
    private final InferResult existingResult;

    public InconsistencyException(Rule rule, InferResult ruleResult, InferResult existingResult) {
        super("Rule " + rule + " causes inconsistent result " + ruleResult + " that does not biimplicate " + existingResult + ".");
        this.rule = rule;
        this.ruleResult = ruleResult;
        this.existingResult = existingResult;
    }

    public Rule rule() {
        return rule;
    }

    public InferResult ruleResult() {
        return ruleResult;
    }

    public InferResult existingResult() {
        return existingResult;
    }

}
