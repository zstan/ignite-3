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

package org.apache.ignite.internal.schema.definition.builder;

import static org.apache.ignite.internal.util.ArrayUtils.asSet;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.apache.ignite.internal.schema.definition.index.PrimaryKeyDefinitionImpl;
import org.apache.ignite.internal.tostring.IgniteToStringInclude;
import org.apache.ignite.internal.util.CollectionUtils;
import org.apache.ignite.schema.definition.PrimaryKeyDefinition;
import org.apache.ignite.schema.definition.builder.PrimaryKeyDefinitionBuilder;
import org.apache.ignite.schema.definition.builder.SchemaObjectBuilder;

/**
 * Primary key builder.
 */
public class PrimaryKeyDefinitionBuilderImpl implements SchemaObjectBuilder, PrimaryKeyDefinitionBuilder {
    /** Index columns. */
    @IgniteToStringInclude
    private Set<String> columns;

    /** Affinity columns. */
    @IgniteToStringInclude
    private Set<String> affinityColumns;

    /** Builder hints. */
    protected Map<String, String> hints;

    /** {@inheritDoc} */
    @Override
    public PrimaryKeyDefinitionBuilderImpl withColumns(String... columns) {
        this.columns = asSet(columns);

        return this;
    }
    
    /** {@inheritDoc} */
    @Override
    public PrimaryKeyDefinitionBuilderImpl withColumns(Set<String> columns) {
        this.columns = columns;
        
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PrimaryKeyDefinitionBuilderImpl withAffinityColumns(String... affinityColumns) {
        this.affinityColumns = asSet(affinityColumns);

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PrimaryKeyDefinitionBuilderImpl withAffinityColumns(Set<String> affinityColumns) {
        this.affinityColumns = affinityColumns;

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PrimaryKeyDefinitionBuilderImpl withHints(Map<String, String> hints) {
        this.hints = hints;

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public PrimaryKeyDefinition build() {
        if (columns == null) {
            throw new IllegalStateException("Primary key column(s) must be configured.");
        }

        Set<String> affCols;

        if (CollectionUtils.nullOrEmpty(affinityColumns)) {
            affCols = columns;
        } else {
            affCols = affinityColumns;

            if (!columns.containsAll(affinityColumns)) {
                throw new IllegalStateException("Schema definition error: All affinity columns must be part of key.");
            }
        }

        return new PrimaryKeyDefinitionImpl(Collections.unmodifiableSet(columns), Collections.unmodifiableSet(affCols));
    }
}
