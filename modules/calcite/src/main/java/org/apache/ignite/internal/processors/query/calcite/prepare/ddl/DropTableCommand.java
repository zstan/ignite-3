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

package org.apache.ignite.internal.processors.query.calcite.prepare.ddl;

/**
 * DROP TABLE statement.
 */
public class DropTableCommand extends AbstractAlterTableCommand {
    /** Schema name. */
    private String schemaName;

    /** Table name. */
    private String tblName;

    /** Quietly ignore this command if table does not exist. */
    private boolean ifExists;

    /**
     * Get schema name.
     */
    public String schemaName() {
        return schemaName;
    }

    /**
     * Set schema name.
     */
    public void schemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Get table name.
     */
    public String tableName() {
        return tblName;
    }

    /**
     * Set table name.
     */
    public void tableName(String tblName) {
        this.tblName = tblName;
    }

    /**
     * Get quietly ignore this command if table does not exist.
     */
    public boolean ifExists() {
        return ifExists;
    }

    /**
     * Set quietly ignore this command if table does not exist.
     */
    public void ifExists(boolean ifExists) {
        this.ifExists = ifExists;
    }
}
