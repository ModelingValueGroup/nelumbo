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

/**
 * Abstraction for execution context that handles threading differently
 * depending on the environment (JVM with threads vs browser/TeaVM without threads)
 */
public interface ExecutionContext {

    /**
     * Execute a runnable with a given KnowledgeBase context and return the updated KnowledgeBase
     *
     * @param runnable The code to execute
     * @param knowledgeBase The initial KnowledgeBase state
     * @return The updated KnowledgeBase after execution
     */
    KnowledgeBase invoke(Runnable runnable, KnowledgeBase knowledgeBase);

    /**
     * Execute a task asynchronously (or synchronously in environments without threading)
     *
     * @param task The task to execute
     */
    void executeAsync(Runnable task);

    /**
     * Get the platform-specific implementation
     * This method is called once during initialization to select the right implementation
     *
     * @return The appropriate ExecutionContext for the current platform
     */
    static ExecutionContext getInstance() {
        // Try to detect if we're running in TeaVM/browser environment
        // TeaVM doesn't support ForkJoinPool, so we can use that as a detection mechanism
        try {
            Class.forName("java.util.concurrent.ForkJoinPool");
            // We're in a JVM environment, use threaded implementation
            return new JvmExecutionContext();
        } catch (ClassNotFoundException e) {
            // We're in TeaVM/browser environment, use synchronous implementation
            return new BrowserExecutionContext();
        }
    }
}
