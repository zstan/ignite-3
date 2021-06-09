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

package org.apache.ignite.internal.schema;

import java.util.Arrays;
import java.util.UUID;
import org.apache.ignite.internal.schema.row.Row;
import org.apache.ignite.internal.schema.row.RowAssembler;
import org.junit.jupiter.api.Test;

import static org.apache.ignite.internal.schema.NativeTypes.BYTE;
import static org.apache.ignite.internal.schema.NativeTypes.BYTES;
import static org.apache.ignite.internal.schema.NativeTypes.INTEGER;
import static org.apache.ignite.internal.schema.NativeTypes.SHORT;
import static org.apache.ignite.internal.schema.NativeTypes.STRING;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Validate row layout for different schema configurations.
 */
public class RowAssemblerAdvancedSchemaTest {
    /** Table ID test value. */
    public final UUID tableId = UUID.randomUUID();

    /**
     * Validate row layout for schema of fix-len nullable key and fix-len nullable value.
     */
    @Test
    public void fixedNullableColumns() {
        SchemaDescriptor schema = new SchemaDescriptor(tableId, 42,
            new Column[] {
                new Column("keyCol2", BYTE, true),
                new Column("keyCol1", SHORT, true),
                new Column("keyCol3", INTEGER, true)
            },
            new Column[] {
                new Column("valCol2", BYTE, true),
                new Column("valCol1", INTEGER, true),
                new Column("valCol3", SHORT, true)
            });

        // Last col null
        assertRowBytesEquals(
            new byte[] {42, 0, 0, -103, -11, 43, 0, 0, 8, 0, 0, 0, 4, 11, 22, 0, 8, 0, 0, 0, 4, -44, -66, -1},
            new RowAssembler(schema, 128, 0, 128, 0)
                .appendByte((byte)11)
                .appendShort((short)22)
                .appendNull()
                .appendByte((byte)-44)
                .appendShort((short)-66)
                .appendNull()
                .build());

        // First col null.
        assertRowBytesEquals(
            new byte[] {42, 0, 0, -103, -53, 2, 0, 0, 11, 0, 0, 0, 1, 22, 0, 33, 0, 0, 0, 11, 0, 0, 0, 1, -55, -1, -66, -1, -1, -1},
            new RowAssembler(schema, 128, 0, 128, 0)
                .appendNull()
                .appendShort((byte)22)
                .appendInt(33)
                .appendNull()
                .appendShort((short)-55)
                .appendInt(-66)
                .build());

        // Middle col null.
        assertRowBytesEquals(new byte[] {42, 0, 0, -103, 108, 41, 0, 0, 10, 0, 0, 0, 2, 11, 33, 0, 0, 0, 10, 0, 0, 0, 2, -44, -66, -1, -1, -1},
            new RowAssembler(schema, 128, 0, 128, 0)
                .appendByte((byte)11)
                .appendNull()
                .appendInt(33)
                .appendByte((byte)-44)
                .appendNull()
                .appendInt(-66)
                .build());

        // Null both.
        assertRowBytesEquals(new byte[] {42, 0, 0, -103, 0, 0, 0, 0, 5, 0, 0, 0, 7, 5, 0, 0, 0, 7},
            new RowAssembler(schema, 128, 0, 128, 0)
                .appendNull()
                .appendNull()
                .appendNull()
                .appendNull()
                .appendNull()
                .appendNull()
                .build());

        // No value.
        assertRowBytesEquals(new byte[] {42, 0, 1, 9, 22, 44, 0, 0, 12, 0, 0, 0, 0, 11, 22, 0, 33, 0, 0, 0},
            new RowAssembler(schema, 128, 0, 128, 0)
                .appendByte((byte)11)
                .appendShort((short)22)
                .appendInt(33)
                .build());
    }
    /**
     * Validate row layout for schema of fix-len nullable key and fix-len nullable value.
     */
    @Test
    public void varlenNullableColumns2() {
        SchemaDescriptor schema = new SchemaDescriptor(tableId, 42,
            new Column[] {
                new Column("keyCol2", BYTES, true),
                new Column("keyCol3", STRING, true)
            },
            new Column[] {
                new Column("valCol2", BYTES, true),
                new Column("valCol3", STRING, true)
            });

        final byte[] data = new RowAssembler(schema, 128, 1, 128, 1)
            .appendNull()
            .appendString("A我愛我愛我愛我愛我愛")
            .appendNull()
            .appendString("A我愛我愛我愛我愛我愛")
            .build();

        final Row row = new Row(schema, new ByteBufferRow(data));

        assertNull(row.bytesValue(0));
//        assertArrayEquals(new byte[] {33, -77}, row.bytesValue(0));
        assertEquals("A我愛我愛我愛我愛我愛", row.stringValue(1));
//        assertArrayEquals(new byte[] {33, -77}, row.bytesValue(2));
        assertEquals("A我愛我愛我愛我愛我愛", row.stringValue(3));
    }
    /**
     * Validate row layout for schema of fix-len nullable key and fix-len nullable value.
     */
    @Test
    public void varlenNullableColumns() {
        SchemaDescriptor schema = new SchemaDescriptor(tableId, 42,
            new Column[] {
                new Column("keyCol1", STRING, true),
                new Column("keyCol2", BYTES, true),
                new Column("keyCol3", STRING, true)
            },
            new Column[] {
                new Column("valCol1", STRING, true),
                new Column("valCol2", BYTES, true),
                new Column("valCol3", STRING, true)
            });

        // Last col null
        assertRowBytesEquals(
            new byte[] {
                42, 0, 0, 17, -12, -36, 109, -81,
                15, 0, 0, 0, 4, 1, 3, 33, -77, 120, 97, 115, 99, 105, 105,
                19, 0, 0, 0, 4, 1, 2, 33, -77, -26, -120, -111, -26, -124, -101, 74, 97, 118, 97},
            new RowAssembler(schema, 128, 2, 128, 2)
                .appendBytes(new byte[] {33, -77, 120})
                .appendString("ascii")
                .appendNull()
                .appendBytes(new byte[] {33, -77})
                .appendString("我愛Java")
                .appendNull()
                .build());

        // Fist col null.
        assertRowBytesEquals(
            new byte[] {
                42, 0, 0, 17, 59, -11, 127, 92,
                22, 0, 0, 0, 1, 1, 5, 97, 115, 99, 105, 105, -26, -120, -111, -26, -124, -101, 74, 97, 118, 97,
                22, 0, 0, 0, 1, 1, 10, -26, -120, -111, -26, -124, -101, 74, 97, 118, 97, 97, 115, 99, 105, 105},
            new RowAssembler(schema, 128, 2, 128, 2)
                .appendNull()
                .appendString("ascii")
                .appendString("我愛Java")
                .appendNull()
                .appendString("我愛Java")
                .appendString("ascii")
                .build());

        // Middle col null.
        assertRowBytesEquals(
            new byte[] {
                42, 0, 0, 17, -111, 92, -37, -77,
                20, 0, 0, 0, 2, 1, 3, 33, -77, 120, -26, -120, -111, -26, -124, -101, 74, 97, 118, 97,
                14, 0, 0, 0, 2, 1, 2, 33, -77, 97, 115, 99, 105, 105},
            new RowAssembler(schema, 128, 2, 128, 2)
                .appendBytes(new byte[] {33, -77, 120})
                .appendNull()
                .appendString("我愛Java")
                .appendBytes(new byte[] {33, -77})
                .appendNull()
                .appendString("ascii")
                .build());

        // Null both.
        assertRowBytesEquals(
            new byte[] {42, 0, 0, -103, 0, 0, 0, 0, 5, 0, 0, 0, 7, 5, 0, 0, 0, 7},
            new RowAssembler(schema, 128, 0, 128, 0)
                .appendNull()
                .appendNull()
                .appendNull()
                .appendNull()
                .appendNull()
                .appendNull()
                .build());

        // No value.
        assertRowBytesEquals(
            new byte[] {
                42, 0, 1, 1, -118, -28, 81, 103,
                26, 0, 0, 0, 0, 2, 3, 13, 33, -77, 120, -26, -120, -111, -26, -124, -101, 74, 97, 118, 97, 97, 115, 99, 105, 105},
            new RowAssembler(schema, 128, 3, 128, 0)
                .appendBytes(new byte[] {33, -77, 120})
                .appendString("我愛Java")
                .appendString("ascii")
                .build());
    }

    /**
     * Validate row layout for schema of fix-len nullable key and fix-len nullable value.
     */
    @Test
    public void mixedTypes() {
        SchemaDescriptor schema = new SchemaDescriptor(tableId, 42,
            new Column[] {
                new Column("keyCol1", BYTE, true),
                new Column("keyCol1", SHORT, true),
                new Column("keyCol2", BYTES, true),
                new Column("keyCol3", STRING, true)
            },
            new Column[] {
                new Column("valCol1", BYTE, true),
                new Column("valCol1", SHORT, true),
                new Column("valCol2", BYTES, true),
                new Column("valCol3", STRING, true)
            });

        // Check null/non-null all fixlen/varlen.
        assertRowBytesEquals(
            new byte[] {
                42, 0, 0, 25, -85, 82, 5, 0,
                8, 0, 0, 0, 12, 11, 22, 0,
                14, 0, 0, 0, 3, 1, 2, 77, -88, 97, 115, 99, 105, 105},
            new RowAssembler(schema, 128, 0, 128, 2)
                .appendByte((byte)11)
                .appendShort((short)22)
                .appendNull()
                .appendNull()
                .appendNull()
                .appendNull()
                .appendBytes(new byte[] {77, -88})
                .appendString("ascii")
                .build());

        // Check null/non-null fixlen.
        assertRowBytesEquals(
            new byte[] {
                42, 0, 0, 17, -18, -9, 119, -80,
                21, 0, 0, 0, 1, 1, 4, 22, 0, 33, -44, -26, -120, -111, -26, -124, -101, 74, 97, 118, 97,
                15, 0, 0, 0, 2, 1, 3, 55, 77, -88, 97, 115, 99, 105, 105},
            new RowAssembler(schema, 128, 2, 128, 2)
                .appendNull()
                .appendShort((short)22)
                .appendBytes(new byte[] {33, -44})
                .appendString("我愛Java")
                .appendByte((byte)55)
                .appendNull()
                .appendBytes(new byte[] {77, -88})
                .appendString("ascii")
                .build());

        // Check null/non-null varlen.
        assertRowBytesEquals(
            new byte[] {
                42, 0, 0, -120, 23, 13, 124, -80,
                18, 0, 0, 0, 4, 11, 22, 0, -26, -120, -111, -26, -124, -101, 74, 97, 118, 97,
                10, 0, 0, 0, 8, 55, 22, 0, 77, -88},
            new RowAssembler(schema, 1, 1)
                .appendByte((byte)11)
                .appendShort((short)22)
                .appendNull()
                .appendString("我愛Java")
                .appendByte((byte)55)
                .appendShort((short)22)
                .appendBytes(new byte[] {77, -88})
                .appendNull()
                .build());

        // Check null/non-null mixed.
        assertRowBytesEquals(
            new byte[] {
                42, 0, 0, -120, -127, -70, 123, -80,
                16, 0, 0, 0, 6, 11, -26, -120, -111, -26, -124, -101, 74, 97, 118, 97,
                9, 0, 0, 0, 9, 22, 0, 77, -88},
            new RowAssembler(schema, 1, 1)
                .appendByte((byte)11)
                .appendNull()
                .appendNull()
                .appendString("我愛Java")
                .appendNull()
                .appendShort((short)22)
                .appendBytes(new byte[] {77, -88})
                .appendNull()
                .build());

        // Check all null/non-null.
        assertRowBytesEquals(
            new byte[] {
                42, 0, 0, -111, 3, -8, 124, -80,
                22, 0, 0, 0, 0, 1, 5, 11, 22, 0, 33, -44, -26, -120, -111, -26, -124, -101, 74, 97, 118, 97,
                5, 0, 0, 0, 15},
            new RowAssembler(schema, 128, 2, 128, 0)
                .appendByte((byte)11)
                .appendShort((short)22)
                .appendBytes(new byte[] {33, -44})
                .appendString("我愛Java")
                .appendNull()
                .appendNull()
                .appendNull()
                .appendNull()
                .build());
    }

    /**
     * @param expected Expected row bytes.
     * @param actual Actual row bytes.
     */
    private void assertRowBytesEquals(byte[] expected, byte[] actual) {
        assertArrayEquals(expected, actual, Arrays.toString(actual));
    }
}
