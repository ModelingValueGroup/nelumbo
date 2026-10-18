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

package org.modelingvalue.nelumbo.collections;

import java.io.Serial;

import org.modelingvalue.collections.Collection;
import org.modelingvalue.nelumbo.Node;
import org.modelingvalue.nelumbo.NodeInfo;
import org.modelingvalue.nelumbo.lang.Type;

public abstract class NCollection extends Node {
    @Serial
    private static final long serialVersionUID = -4533295959981910935L;

    protected NCollection(NodeInfo nodeInfo, Object... args) {
        super(nodeInfo, args);
    }

    @SuppressWarnings("unchecked")
    public <T> Collection<T> collection() {
        return (Collection<T>) get(0);
    }

    public boolean complete() {
        return (Boolean) get(1);
    }

    public int size() {
        return collection().size();
    }

    public Type elementType() {
        return type().arguments().first();
    }

    protected static final int open(Object[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("..".equals(args[i])) {
                return i;
            }
        }
        return -1;
    }

    protected static final Object[] remove(Object[] args, int i) {
        if (i >= 0) {
            Object[] result = new Object[args.length - 1];
            System.arraycopy(args, 0, result, 0, i);
            System.arraycopy(args, i + 1, result, i, result.length - i);
            return result;
        }
        return args;
    }

}
