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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.modelingvalue.nelumbo.mcp.ExampleCatalog.Example;

public class ExampleCatalogTest {

    @Test
    public void listContainsKnownExamplesWithDescriptions() {
        List<Example> all = ExampleCatalog.list();
        assertTrue(all.stream().anyMatch(e -> e.name().equals("family")));
        assertTrue(all.stream().anyMatch(e -> e.name().equals("langOnly")));
        assertTrue(all.stream().allMatch(e -> e.description() != null && !e.description().isBlank()));
    }

    @Test
    public void familyDescriptionIsInformative() {
        List<Example> all  = ExampleCatalog.list();
        String        desc = all.stream()
                               .filter(e -> e.name().equals("family"))
                               .findFirst()
                               .map(Example::description)
                               .orElse("");
        assertTrue(desc.toLowerCase().contains("family") || desc.toLowerCase().contains("fact"),
                   "family description should mention 'family' or 'fact': " + desc);
    }

    @Test
    public void everyEntryResolvesOnTheClasspath() {
        for (Example e : ExampleCatalog.list()) {
            assertNotNull(ExampleCatalog.content(e.name()), e.name());
        }
    }

    @Test
    public void contentOfFamily() {
        String content = ExampleCatalog.content("family");
        assertNotNull(content);
        assertTrue(content.contains("pc(Hendrik, Juliana)"));
    }

    @Test
    public void unknownNameGivesNull() {
        assertNull(ExampleCatalog.content("no-such-example"));
    }
}
