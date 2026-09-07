//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//  (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                         ~
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

package org.modelingvalue.collections.util;

// OVERLAY of immutable-collections ContextThread (from ../immutable-collections, API-compatible with 6.0.2)
// for the TeaVM (browser) build - deltas: no ForkJoin worker-thread hierarchy and no createPool(); the class
// is never instantiated, so context storage is the upstream non-worker-thread fallback (a plain ThreadLocal)
// made the only path. POOL_SIZE / getCurrentNr() keep their upstream non-worker-thread semantics because
// Concurrent, HashCollectionImpl and friends reference them.

import org.modelingvalue.collections.Collection;

@SuppressWarnings("unused")
public final class ContextThread extends Thread {
    public static final int POOL_SIZE = Integer.getInteger("POOL_SIZE", Collection.PARALLELISM * 2 + 2);

    private final static ThreadLocal<Object[]> CONTEXT = new ThreadLocal<>();

    public static Object[] getContext() {
        return CONTEXT.get();
    }

    public static Object[] setIncrement(Object[] context) {
        return setContext(context, +1);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Object[] setDecrement(Object[] context) {
        return setContext(context, -1);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static Object[] setContext(Object[] context) {
        return setContext(context, 0);
    }

    private static Object[] setContext(Object[] context, int delta) {
        Object[] pre = CONTEXT.get();
        CONTEXT.set(context);
        return pre;
    }

    public static int getCurrentNr() {
        return -1;
    }

    public static boolean isCurrentAContextThread() {
        return false;
    }

    private ContextThread() {
        // never instantiated in the browser build
    }
}
