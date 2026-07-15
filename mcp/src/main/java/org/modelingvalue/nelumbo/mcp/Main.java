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

package org.modelingvalue.nelumbo.mcp;

import java.awt.GraphicsEnvironment;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.swing.JOptionPane;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * MCP stdio server for authoring Nelumbo decision models. One session per process;
 * register with any MCP client as: java -jar nelumbo-mcp-server.jar [--eval-deadline-ms N]
 */
public final class Main {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String PRIMER = """
            Nelumbo (.nl) syntax essentials:
              type declarations:   Person :: Object
              pattern definitions: Integer ::= fib(<Integer>)
              instances:           Person ::= Alice, Bob
              variables:           Integer n, f
              rules:               fib(n)=f <=> f=n if n>=0 & n<=1, f=fib(n-1)+fib(n-2) if n>1
              fact types:          FactType ::= pc(<Person>,<Person>)
              facts:               fact pc(Alice, Bob)
              queries + expected:  fib(5)=f ? [(f=5)][..]
              logic:               & (and), | (or), E[x](..) exists, A[x](..) forall
              imports:             import nelumbo.integers (also: strings, collections, rationals, datetime, logic)
            Workflow: start from new_model, look up syntax with search_docs / get_example,
            and iterate with eval_nl until ok=true with all expectations matched.
            """;

    private Main() {
    }

    public static void main(String[] args) {
        long deadlineMs = 10_000;
        for (int i = 0; i < args.length - 1; i++) {
            if ("--eval-deadline-ms".equals(args[i])) {
                deadlineMs = Long.parseLong(args[i + 1]);
            }
        }
        NelumboTools tools = new NelumboTools(deadlineMs);
        // Peek the first byte before wiring the transport: an MCP client always speaks first, so EOF
        // before any byte means the jar was double-clicked (stdin is /dev/null) - explain instead of
        // exiting silently.
        PushbackInputStream in = new PushbackInputStream(System.in);
        int first;
        try {
            first = in.read();
            if (first < 0) {
                explainUsageWhenDoubleClicked();
                return;
            }
            in.unread(first);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        StdioServerTransportProvider transport = new StdioServerTransportProvider(McpJsonDefaults.getMapper(), in, System.out);
        // From here on the real stdout belongs to JSON-RPC: reroute everything else
        // (nelumbo TRACE output, stray prints) to stderr.
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.err), true));
        McpServer.sync(transport)
                .serverInfo("nelumbo", "0.1.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(evalNlTool(tools), searchDocsTool(tools), getExampleTool(tools), newModelTool(tools))
                .build();
        // The stdio transport owns non-daemon threads; the JVM stays alive until stdin closes.
    }

    private static void explainUsageWhenDoubleClicked() {
        String jar = ownJarName();
        String message = "This is a Model Context Protocol (MCP) server: it is not meant to be started directly,\n"
                + "but to be registered with an MCP client, which starts it itself. For example:\n\n"
                + "    claude mcp add nelumbo -- java -jar " + jar;
        System.err.println(message);
        if (System.console() == null && !GraphicsEnvironment.isHeadless()) {
            org.modelingvalue.nelumbo.tools.AppIcon.install(null);
            JOptionPane.showMessageDialog(null, message, "Nelumbo MCP Server", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static String ownJarName() {
        try {
            return Path.of(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (Exception e) {
            return "nelumbo-mcp-server-<version>.jar";
        }
    }

    private static McpServerFeatures.SyncToolSpecification evalNlTool(NelumboTools tools) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "content", Map.of("type", "string", "description", "the complete .nl source to parse and evaluate"),
                        "name", Map.of("type", "string", "description", "optional display name, e.g. model.nl")),
                "required", List.of("content"));
        return tool(Tool.builder("eval_nl", schema)
                        .description("Parse and evaluate a self-contained Nelumbo (.nl) decision model. Returns ok, "
                                + "diagnostics (line/col/message/sourceLine/caret, plus hint and docRef for known "
                                + "traps) and per-query results incl. whether embedded expected results matched.\n\n" + PRIMER)
                        .build(),
                args -> tools.evalNl((String) args.get("content"), (String) args.get("name")));
    }

    private static McpServerFeatures.SyncToolSpecification searchDocsTool(NelumboTools tools) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("query", Map.of("type", "string", "description", "keywords to search for")),
                "required", List.of("query"));
        return tool(Tool.builder("search_docs", schema)
                        .description("Keyword search over the bundled Nelumbo language documentation; returns the "
                                + "best-matching sections. Use before guessing syntax.")
                        .build(),
                args -> tools.searchDocs((String) args.get("query")));
    }

    private static McpServerFeatures.SyncToolSpecification getExampleTool(NelumboTools tools) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("name", Map.of("type", "string", "description", "example name; omit to list all")));
        return tool(Tool.builder("get_example", schema)
                        .description("Without a name: list the bundled working .nl examples. With a name: return the "
                                + "full source. Working examples are the best syntax reference.")
                        .build(),
                args -> tools.getExample((String) args.get("name")));
    }

    private static McpServerFeatures.SyncToolSpecification newModelTool(NelumboTools tools) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of("title", Map.of("type", "string", "description", "title of the decision model")));
        return tool(Tool.builder("new_model", schema)
                        .description("Return a commented, self-verifying skeleton .nl decision model to edit, instead "
                                + "of starting from a blank page.")
                        .build(),
                args -> tools.newModel((String) args.get("title")));
    }

    private static McpServerFeatures.SyncToolSpecification tool(Tool tool, Function<Map<String, Object>, Map<String, Object>> handler) {
        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    try {
                        return CallToolResult.builder()
                                .addTextContent(JSON.writeValueAsString(handler.apply(request.arguments())))
                                .build();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return CallToolResult.builder().addTextContent(e.toString()).isError(true).build();
                    }
                })
                .build();
    }
}
