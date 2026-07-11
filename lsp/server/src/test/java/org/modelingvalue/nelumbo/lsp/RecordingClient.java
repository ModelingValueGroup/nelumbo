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

package org.modelingvalue.nelumbo.lsp;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

/** Test double: records diagnostics published by the server under test. */
public class RecordingClient implements LanguageClient {
    public final List<PublishDiagnosticsParams> diagnostics        = new CopyOnWriteArrayList<>();
    private final CountDownLatch                firstDiagnostics   = new CountDownLatch(1);
    private final CountDownLatch                inlayHintsRefreshed = new CountDownLatch(1);

    @Override
    public void telemetryEvent(Object object) {
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams params) {
        diagnostics.add(params);
        firstDiagnostics.countDown();
    }

    @Override
    public void showMessage(MessageParams params) {
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams params) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void logMessage(MessageParams params) {
    }

    @Override
    public CompletableFuture<Void> refreshInlayHints() {
        inlayHintsRefreshed.countDown();
        return CompletableFuture.completedFuture(null);
    }

    public boolean awaitDiagnostics(long seconds) throws InterruptedException {
        return firstDiagnostics.await(seconds, TimeUnit.SECONDS);
    }

    public boolean awaitInlayHintRefresh(long seconds) throws InterruptedException {
        return inlayHintsRefreshed.await(seconds, TimeUnit.SECONDS);
    }
}
