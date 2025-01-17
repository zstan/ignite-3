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

package org.apache.ignite.internal.table.distributed;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.ignite.configuration.schemas.table.TableChange;
import org.apache.ignite.configuration.schemas.table.TableView;
import org.apache.ignite.configuration.schemas.table.TablesConfiguration;
import org.apache.ignite.configuration.NamedListView;
import org.apache.ignite.internal.affinity.AffinityManager;
import org.apache.ignite.internal.affinity.event.AffinityEvent;
import org.apache.ignite.internal.affinity.event.AffinityEventParameters;
import org.apache.ignite.internal.configuration.ConfigurationManager;
import org.apache.ignite.internal.configuration.util.ConfigurationUtil;
import org.apache.ignite.internal.manager.EventListener;
import org.apache.ignite.internal.manager.ListenerRemovedException;
import org.apache.ignite.internal.manager.Producer;
import org.apache.ignite.internal.metastorage.MetaStorageManager;
import org.apache.ignite.internal.metastorage.client.Conditions;
import org.apache.ignite.internal.metastorage.client.Entry;
import org.apache.ignite.internal.metastorage.client.Operations;
import org.apache.ignite.internal.raft.Loza;
import org.apache.ignite.internal.schema.SchemaManager;
import org.apache.ignite.internal.schema.SchemaRegistry;
import org.apache.ignite.internal.schema.event.SchemaEvent;
import org.apache.ignite.internal.schema.event.SchemaEventParameters;
import org.apache.ignite.internal.table.TableImpl;
import org.apache.ignite.internal.table.distributed.raft.PartitionListener;
import org.apache.ignite.internal.table.distributed.storage.InternalTableImpl;
import org.apache.ignite.internal.table.event.TableEvent;
import org.apache.ignite.internal.table.event.TableEventParameters;
import org.apache.ignite.internal.util.Cursor;
import org.apache.ignite.internal.vault.VaultManager;
import org.apache.ignite.lang.ByteArray;
import org.apache.ignite.lang.IgniteBiTuple;
import org.apache.ignite.lang.IgniteLogger;
import org.apache.ignite.network.ClusterNode;
import org.apache.ignite.raft.client.service.RaftGroupService;
import org.apache.ignite.table.Table;
import org.apache.ignite.table.manager.IgniteTables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Table manager.
 */
public class TableManager extends Producer<TableEvent, TableEventParameters> implements IgniteTables {
    /** The logger. */
    private static final IgniteLogger LOG = IgniteLogger.forClass(TableManager.class);

    /** Internal prefix for the metasorage. */
    private static final String INTERNAL_PREFIX = "internal.tables.";

    /** Public prefix for metastorage. */
    private static final String PUBLIC_PREFIX = "dst-cfg.table.tables.";

    /** Meta storage service. */
    private final MetaStorageManager metaStorageMgr;

    /** Configuration manager. */
    private final ConfigurationManager configurationMgr;

    /** Raft manmager. */
    private final Loza raftMgr;

    /** Schema manager. */
    private final SchemaManager schemaMgr;

    /** Affinity manager. */
    private final AffinityManager affMgr;

    /** Tables. */
    private final Map<String, TableImpl> tables = new ConcurrentHashMap<>();

    /**
     * Creates a new table manager.
     *
     * @param configurationMgr Configuration manager.
     * @param metaStorageMgr Meta storage manager.
     * @param schemaMgr Schema manager.
     * @param affMgr Affinity manager.
     * @param raftMgr Raft manager.
     * @param vaultManager Vault manager.
     */
    public TableManager(
        ConfigurationManager configurationMgr,
        MetaStorageManager metaStorageMgr,
        SchemaManager schemaMgr,
        AffinityManager affMgr,
        Loza raftMgr,
        VaultManager vaultManager
    ) {
        this.configurationMgr = configurationMgr;
        this.metaStorageMgr = metaStorageMgr;
        this.affMgr = affMgr;
        this.raftMgr = raftMgr;
        this.schemaMgr = schemaMgr;

        listenForTableChange();
    }

    /**
     * Creates local structures for a table.
     *
     * @param name Table name.
     * @param tblId Table id.
     * @param assignment Affinity assignment.
     * @param schemaReg Schema registry for the table.
     */
    private void createTableLocally(
        String name,
        UUID tblId,
        List<List<ClusterNode>> assignment,
        SchemaRegistry schemaReg
    ) {
        int partitions = assignment.size();

        HashMap<Integer, RaftGroupService> partitionMap = new HashMap<>(partitions);

        for (int p = 0; p < partitions; p++) {
            partitionMap.put(p, raftMgr.startRaftGroup(
                raftGroupName(tblId, p),
                assignment.get(p),
                new PartitionListener()
            ));
        }

        InternalTableImpl internalTable = new InternalTableImpl(name, tblId, partitionMap, partitions);

        var table = new TableImpl(internalTable, schemaReg);

        tables.put(name, table);

        onEvent(TableEvent.CREATE, new TableEventParameters(table), null);
    }

    /**
     * Drops local structures for a table.
     *
     * @param name Table name.
     * @param tblId Table id.
     * @param assignment Affinity assignment.
     */
    private void dropTableLocally(String name, UUID tblId, List<List<ClusterNode>> assignment) {
        int partitions = assignment.size();

        for (int p = 0; p < partitions; p++)
            raftMgr.stopRaftGroup(raftGroupName(tblId, p), assignment.get(p));

        TableImpl table = tables.get(name);

        assert table != null : "There is no table with the name specified [name=" + name + ']';

        onEvent(TableEvent.DROP, new TableEventParameters(table), null);
    }

    /**
     * Compounds a RAFT group unique name.
     *
     * @param tableId Table identifier.
     * @param partition Number of table partitions.
     * @return A RAFT group name.
     */
    @NotNull private String raftGroupName(UUID tableId, int partition) {
        return tableId + "_part_" + partition;
    }

    /**
     * Listens on a drop or create table.
     */
    private void listenForTableChange() {
        //TODO: IGNITE-14652 Change a metastorage update in listeners to multi-invoke
        configurationMgr.configurationRegistry().getConfiguration(TablesConfiguration.KEY).tables().listen(ctx -> {
            Set<String> tablesToStart = (ctx.newValue() == null || ctx.newValue().namedListKeys() == null) ?
                Collections.emptySet() :
                ctx.newValue().namedListKeys().stream().filter(t -> !ctx.oldValue().namedListKeys().contains(t)).collect(Collectors.toSet());

            Set<String> tablesToStop = (ctx.oldValue() == null || ctx.oldValue().namedListKeys() == null) ?
                Collections.emptySet() :
                ctx.oldValue().namedListKeys().stream().filter(t -> !ctx.newValue().namedListKeys().contains(t)).collect(Collectors.toSet());

            List<CompletableFuture<Boolean>> futs = new ArrayList<>();

            final Set<String> schemaChanged =
                (ctx.oldValue() != null && ctx.oldValue().namedListKeys() != null && ctx.newValue() != null && ctx.newValue().namedListKeys() != null) ?
                    ctx.oldValue().namedListKeys().stream()
                        .filter(tblName -> ctx.newValue().namedListKeys().contains(tblName)) // Filter changed tables.
                        .filter(tblName -> {
                            final TableView newTbl = ctx.newValue().get(tblName);
                            final TableView oldTbl = ctx.oldValue().get(tblName);

                            assert newTbl.columns().namedListKeys() != null && oldTbl.columns().namedListKeys() != null;

                            return newTbl.columns().namedListKeys().stream().anyMatch(c -> !oldTbl.columns().namedListKeys().contains(c)) ||
                                oldTbl.columns().namedListKeys().stream().anyMatch(c -> !newTbl.columns().namedListKeys().contains(c));
                        }).collect(Collectors.toSet()) :
                    Collections.emptySet();

            if (!tablesToStart.isEmpty())
                futs.addAll(startTables(tablesToStart, ctx.storageRevision(), ctx.newValue()));

            if (!schemaChanged.isEmpty())
                futs.addAll(changeSchema(schemaChanged));

            if (!tablesToStop.isEmpty())
                futs.addAll(stopTables(tablesToStop));

            return CompletableFuture.allOf(futs.toArray(CompletableFuture[]::new));
        });
    }

    /**
     * Start tables routine.
     *
     * @param tbls Tables to start.
     * @param rev Metastore revision.
     * @param cfgs Table configurations.
     * @return Table creation futures.
     */
    private List<CompletableFuture<Boolean>> startTables(Set<String> tbls, long rev, NamedListView<TableView> cfgs) {
        boolean hasMetastorageLocally = metaStorageMgr.hasMetastorageLocally(configurationMgr);

        List<CompletableFuture<Boolean>> futs = new ArrayList<>();

        for (String tblName : tbls) {
            TableView tableView = cfgs.get(tblName);

            UUID tblId = new UUID(rev, 0L);

            if (hasMetastorageLocally) {
                var key = new ByteArray(INTERNAL_PREFIX + tblId);
                futs.add(metaStorageMgr.invoke(
                    Conditions.notExists(key),
                    Operations.put(key, tableView.name().getBytes(StandardCharsets.UTF_8)),
                    Operations.noop())
                    .thenCompose(res -> schemaMgr.initSchemaForTable(tblId, tableView.name()))
                    .thenCompose(res -> affMgr.calculateAssignments(tblId, tableView.name())));
            }

            final CompletableFuture<AffinityEventParameters> affinityReadyFut = new CompletableFuture<>();
            final CompletableFuture<SchemaEventParameters> schemaReadyFut = new CompletableFuture<>();

            CompletableFuture.allOf(affinityReadyFut, schemaReadyFut)
                .exceptionally(e -> {
                    LOG.error("Failed to create a new table [name=" + tblName + ", id=" + tblId + ']', e);

                    onEvent(TableEvent.CREATE, new TableEventParameters(tblId, tblName), e);

                    return null;
                })
                .thenRun(() -> createTableLocally(
                    tblName,
                    tblId,
                    affinityReadyFut.join().assignment(),
                    schemaReadyFut.join().schemaRegistry()
                ));

            affMgr.listen(AffinityEvent.CALCULATED, new EventListener<>() {
                @Override public boolean notify(@NotNull AffinityEventParameters parameters, @Nullable Throwable e) {
                    if (!tblId.equals(parameters.tableId()))
                        return false;

                    if (e == null)
                        affinityReadyFut.complete(parameters);
                    else
                        affinityReadyFut.completeExceptionally(e);

                    return true;
                }

                @Override public void remove(@NotNull Throwable e) {
                    affinityReadyFut.completeExceptionally(e);
                }
            });

            schemaMgr.listen(SchemaEvent.INITIALIZED, new EventListener<>() {
                @Override public boolean notify(@NotNull SchemaEventParameters parameters, @Nullable Throwable e) {
                    if (!tblId.equals(parameters.tableId()) && parameters.schemaRegistry().lastSchemaVersion() >= 1)
                        return false;

                    if (e == null)
                        schemaReadyFut.complete(parameters);
                    else
                        schemaReadyFut.completeExceptionally(e);

                    return true;
                }

                @Override public void remove(@NotNull Throwable e) {
                    schemaReadyFut.completeExceptionally(e);
                }
            });
        }

        return futs;
    }

    /**
     * Drop tables routine.
     *
     * @param tbls Tables to drop.
     * @return Table drop futures.
     */
    private List<CompletableFuture<Boolean>> stopTables(Set<String> tbls) {
        boolean hasMetastorageLocally = metaStorageMgr.hasMetastorageLocally(configurationMgr);

        List<CompletableFuture<Boolean>> futs = new ArrayList<>();

        for (String tblName : tbls) {
            TableImpl t = tables.get(tblName);

            UUID tblId = t.tableId();

            if (hasMetastorageLocally) {
                var key = new ByteArray(INTERNAL_PREFIX + tblId);

                futs.add(affMgr.removeAssignment(tblId)
                    .thenCompose(res -> schemaMgr.unregisterSchemas(tblId))
                    .thenCompose(res ->
                        metaStorageMgr.invoke(Conditions.exists(key),
                            Operations.remove(key),
                            Operations.noop())));
            }

            affMgr.listen(AffinityEvent.REMOVED, new EventListener<>() {
                @Override public boolean notify(@NotNull AffinityEventParameters parameters, @Nullable Throwable e) {
                    if (!tblId.equals(parameters.tableId()))
                        return false;

                    if (e == null)
                        dropTableLocally(tblName, tblId, parameters.assignment());
                    else
                        onEvent(TableEvent.DROP, new TableEventParameters(tblId, tblName), e);

                    return true;
                }

                @Override public void remove(@NotNull Throwable e) {
                    onEvent(TableEvent.DROP, new TableEventParameters(tblId, tblName), e);
                }
            });
        }

        return futs;
    }

    /**
     * Start tables routine.
     *
     * @param tbls Tables to start.
     * @return Table creation futures.
     */
    private List<CompletableFuture<Boolean>> changeSchema(Set<String> tbls) {
        boolean hasMetastorageLocally = metaStorageMgr.hasMetastorageLocally(configurationMgr);

        List<CompletableFuture<Boolean>> futs = new ArrayList<>();

        for (String tblName : tbls) {
            TableImpl tbl = tables.get(tblName);

            UUID tblId = tbl.tableId();

            final int ver = tbl.schemaView().lastSchemaVersion() + 1;

            if (hasMetastorageLocally)
                futs.add(schemaMgr.updateSchemaForTable(tblId, tblName));

            final CompletableFuture<SchemaEventParameters> schemaReadyFut = new CompletableFuture<>();

            CompletableFuture.allOf(schemaReadyFut)
                .exceptionally(e -> {
                    LOG.error("Failed to upgrade schema for a table [name=" + tblName + ", id=" + tblId + ']', e);

                    onEvent(TableEvent.ALTER, new TableEventParameters(tblId, tblName), e);

                    return null;
                })
                .thenRun(() ->
                    onEvent(TableEvent.ALTER, new TableEventParameters(tblId, tblName), null)
                );

            schemaMgr.listen(SchemaEvent.CHANGED, new EventListener<>() {
                @Override public boolean notify(@NotNull SchemaEventParameters parameters, @Nullable Throwable e) {
                    if (!tblId.equals(parameters.tableId()) && parameters.schemaRegistry().lastSchemaVersion() < ver)
                        return false;

                    if (e == null)
                        schemaReadyFut.complete(parameters);
                    else
                        schemaReadyFut.completeExceptionally(e);

                    return true;
                }

                @Override public void remove(@NotNull Throwable e) {
                    schemaReadyFut.completeExceptionally(e);
                }
            });
        }

        return futs;
    }

    /** {@inheritDoc} */
    @Override public Table createTable(String name, Consumer<TableChange> tableInitChange) {
        CompletableFuture<Table> tblFut = new CompletableFuture<>();

        listen(TableEvent.CREATE, new EventListener<>() {
            @Override public boolean notify(@NotNull TableEventParameters parameters, @Nullable Throwable e) {
                String tableName = parameters.tableName();

                if (!name.equals(tableName))
                    return false;

                if (e == null)
                    tblFut.complete(parameters.table());
                else
                    tblFut.completeExceptionally(e);

                return true;
            }

            @Override public void remove(@NotNull Throwable e) {
                tblFut.completeExceptionally(e);
            }
        });

        try {
            configurationMgr.configurationRegistry()
                .getConfiguration(TablesConfiguration.KEY).tables().change(change ->
                change.create(name, tableInitChange)).get();
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Table wasn't created [name=" + name + ']', e);

            tblFut.completeExceptionally(e);
        }

        return tblFut.join();
    }

    /** {@inheritDoc} */
    @Override public void alterTable(String name, Consumer<TableChange> tableChange) {
        CompletableFuture<Void> tblFut = new CompletableFuture<>();

        listen(TableEvent.ALTER, new EventListener<>() {
            @Override public boolean notify(@NotNull TableEventParameters parameters, @Nullable Throwable e) {
                String tableName = parameters.tableName();

                if (!name.equals(tableName))
                    return false;

                if (e == null) {
                    tblFut.complete(null);
                }
                else
                    tblFut.completeExceptionally(e);

                return true;
            }

            @Override public void remove(@NotNull Throwable e) {
                tblFut.completeExceptionally(e);
            }
        });

        try {
            configurationMgr.configurationRegistry()
                .getConfiguration(TablesConfiguration.KEY).tables().change(change ->
                change.update(name, tableChange)).get();
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Table wasn't created [name=" + name + ']', e);

            tblFut.completeExceptionally(e);
        }

        tblFut.join();
    }

    /** {@inheritDoc} */
    @Override public void dropTable(String name) {
        CompletableFuture<Void> dropTblFut = new CompletableFuture<>();

        listen(TableEvent.DROP, new EventListener<>() {
            @Override public boolean notify(@NotNull TableEventParameters parameters, @Nullable Throwable e) {
                String tableName = parameters.tableName();

                if (!name.equals(tableName))
                    return false;

                if (e == null) {
                    Table droppedTable = tables.remove(tableName);

                    assert droppedTable != null;

                    dropTblFut.complete(null);
                }
                else
                    dropTblFut.completeExceptionally(e);

                return true;
            }

            @Override public void remove(@NotNull Throwable e) {
                dropTblFut.completeExceptionally(e);
            }
        });

        try {
            configurationMgr
                .configurationRegistry()
                .getConfiguration(TablesConfiguration.KEY)
                .tables()
                .change(change -> change.delete(name)).get();
        }
        catch (InterruptedException | ExecutionException e) {
            LOG.error("Table wasn't dropped [name=" + name + ']', e);

            dropTblFut.completeExceptionally(e);
        }

        dropTblFut.join();
    }

    /** {@inheritDoc} */
    @Override public List<Table> tables() {
        ArrayList<Table> tables = new ArrayList<>();

        for (String tblName : tableNamesConfigured()) {
            Table tbl = table(tblName, false);

            if (tbl != null)
                tables.add(tbl);
        }

        return tables;
    }

    /**
     * Collects a set of table names from the distributed configuration storage.
     *
     * @return A set of table names.
     */
    private HashSet<String> tableNamesConfigured() {
        IgniteBiTuple<ByteArray, ByteArray> range = toRange(new ByteArray(PUBLIC_PREFIX));

        HashSet<String> tableNames = new HashSet<>();

        try (Cursor<Entry> cursor = metaStorageMgr.range(range.get1(), range.get2())) {
            while (cursor.hasNext()) {
                Entry entry = cursor.next();

                String keyTail = entry.key().toString().substring(PUBLIC_PREFIX.length());

                int idx = -1;

                //noinspection StatementWithEmptyBody
                while ((idx = keyTail.indexOf('.', idx + 1)) > 0 && keyTail.charAt(idx - 1) == '\\')
                    ;

                String tablName = keyTail.substring(0, idx);

                tableNames.add(ConfigurationUtil.unescape(tablName));
            }
        }
        catch (Exception e) {
            LOG.error("Can't get table names.", e);
        }

        return tableNames;
    }

    /** {@inheritDoc} */
    @Override public Table table(String name) {
        return table(name, true);
    }

    /**
     * Gets a table if it exists or {@code null} if it was not created or was removed before.
     *
     * @param name Table name.
     * @param checkConfiguration True when the method checks a configuration before tries to get a table,
     * false otherwise.
     * @return A table or {@code null} if table does not exist.
     */
    private Table table(String name, boolean checkConfiguration) {
        if (checkConfiguration && !isTableConfigured(name))
            return null;

        Table tbl = tables.get(name);

        if (tbl != null)
            return tbl;

        CompletableFuture<Table> getTblFut = new CompletableFuture<>();

        EventListener<TableEventParameters> clo = new EventListener<>() {
            @Override public boolean notify(@NotNull TableEventParameters parameters, @Nullable Throwable e) {
                if (e instanceof ListenerRemovedException) {
                    getTblFut.completeExceptionally(e);

                    return true;
                }

                String tableName = parameters.tableName();

                if (!name.equals(tableName))
                    return false;

                if (e == null)
                    getTblFut.complete(parameters.table());
                else
                    getTblFut.completeExceptionally(e);

                return true;
            }

            @Override public void remove(@NotNull Throwable e) {
                getTblFut.completeExceptionally(e);
            }
        };

        listen(TableEvent.CREATE, clo);

        tbl = tables.get(name);

        if (tbl != null && getTblFut.complete(tbl) ||
            !isTableConfigured(name) && getTblFut.complete(null))
            removeListener(TableEvent.CREATE, clo);

        return getTblFut.join();
    }

    /**
     * Checks that the table is configured.
     *
     * @param name Table name.
     * @return True if table configured, false otherwise.
     */
    private boolean isTableConfigured(String name) {
        return metaStorageMgr.get(new ByteArray(PUBLIC_PREFIX + ConfigurationUtil.escape(name) + ".name")).join() != null;
    }

    /**
     * Transforms a prefix bytes to range.
     * This method should be replaced to direct call of range by prefix
     * in Meta storage manager when it will be implemented.
     * TODO: IGNITE-14799
     *
     * @param prefixKey Prefix bytes.
     * @return Tuple with left and right borders for range.
     */
    private IgniteBiTuple<ByteArray, ByteArray> toRange(ByteArray prefixKey) {
        var bytes = Arrays.copyOf(prefixKey.bytes(), prefixKey.bytes().length);

        if (bytes[bytes.length - 1] != Byte.MAX_VALUE)
            bytes[bytes.length - 1]++;
        else
            bytes = Arrays.copyOf(bytes, bytes.length + 1);

        return new IgniteBiTuple<>(prefixKey, new ByteArray(bytes));
    }
}
