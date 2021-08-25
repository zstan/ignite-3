/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.schema.builder;

import static org.apache.ignite.internal.util.ArrayUtils.asSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import org.apache.ignite.schema.SchemaBuilders;
import org.apache.ignite.schema.definition.builder.HashIndexDefinitionBuilder;
import org.apache.ignite.schema.definition.index.HashIndexDefinition;
import org.junit.jupiter.api.Test;

/**
 * Tests for hash index builder.
 */
public class HashIndexDefinitionBuilderTest {
    /**
     * Build index and check its parameters.
     */
    @Test
    public void testBuild() {
        HashIndexDefinitionBuilder builder = SchemaBuilders.hashIndex("testHI")
                .withColumns(asSet("A", "B", "C"))
                .withHints(Collections.singletonMap("param", "value"));
        HashIndexDefinition idx = builder.build();

        assertEquals("testHI", idx.name());
        assertEquals(3, idx.columns().size());
    }

    /**
     * Try to create index without columns and check error.
     */
    @Test
    public void testBuildNoColumns() {
        HashIndexDefinitionBuilder builder = SchemaBuilders.hashIndex("testHI");

        assertThrows(AssertionError.class, builder::build);
    }
}
