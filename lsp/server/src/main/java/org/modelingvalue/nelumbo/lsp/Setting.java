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

import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public record Setting(Formatting formatting,
                      Classpath classpath,
                      boolean debugging) {
    private static final ObjectMapper jacksonObjectMapper = new ObjectMapper()//
                                                                              .setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE)//
                                                                              .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public Setting() {
        this(new Formatting(), new Classpath(), false);
    }

    @Override
    public String toString() {
        return "Setting{formatting=" + formatting + ", classpath=" + classpath + ", debugging=" + debugging + "}";
    }

    public record Formatting(PropsSpaceLine propsSpaceLine) {
        public Formatting() {
            this(PropsSpaceLine.HAS_ANNOTATION);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Formatting that = (Formatting) o;
            return propsSpaceLine == that.propsSpaceLine;
        }

        @Override
        public String toString() {
            return "Formatting{propsSpaceLine=" + propsSpaceLine + "}";
        }

        public enum PropsSpaceLine {
            ALWAYS, NEVER, HAS_ANNOTATION
        }
    }

    public record Classpath(boolean findConfiguration,
                            boolean findOtherProject) {
        public Classpath() {
            this(true, true);
        }

        @Override
        public String toString() {
            return "Classpath{findConfiguration=" + findConfiguration + ", findOtherProject=" + findOtherProject + "}";
        }
    }

    public static Setting read(Path path) {
        try {
            return jacksonObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE).readValue(path.toFile(), Setting.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void save(Path path) {
        try {
            jacksonObjectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE).writeValue(path.toFile(), this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
