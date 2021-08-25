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
 * ALTER TABLE ... ADD/DROP COLUMN statement.
 */
public abstract class AbstractAlterTableCommand implements DdlCommand {
    /** Quietly ignore this command if table is not exists. */
    protected boolean ifTableExists;

    /** Table name. */
    private String tblName;

    /**
     * Schema name upon which this statement has been issued - <b>not</b> the name of the schema where this new table
     * will be created.
     */
    private String schemaName;

    /**
     * @return Table name.
     */
    public String tableName() {
        return tblName;
    }

    /**
     * @param tblName Table name.
     */
    public void tableName(String tblName) {
        this.tblName = tblName;
    }

    /**
     * @return Schema name upon which this statement has been issued.
     */
    public String schemaName() {
        return schemaName;
    }

    /**
     * @param schemaName Schema name upon which this statement has been issued.
     */
    public void schemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * @return Quietly ignore this command if table is not exists.
     */
    public boolean ifTableExists() {
        return ifTableExists;
    }

    /**
     * @param ifTableExists Quietly ignore this command if table is not exists.
     */
    public void ifTableExists(boolean ifTableExists) {
        this.ifTableExists = ifTableExists;
    }
}
