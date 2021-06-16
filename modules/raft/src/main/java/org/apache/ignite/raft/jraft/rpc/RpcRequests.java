/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: rpc.proto

package org.apache.ignite.raft.jraft.rpc;

import org.apache.ignite.raft.jraft.entity.RaftOutter;
import org.apache.ignite.raft.jraft.util.ByteString;

public final class RpcRequests {
    private RpcRequests() {
    }

    public interface PingRequest extends Message {
        /**
         * <code>required int64 send_timestamp = 1;</code>
         */
        long getSendTimestamp();

        interface Builder {
            Builder setSendTimestamp(long timestamp);

            PingRequest build();
        }

        public static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createPingRequest();
        }
    }

    public interface ErrorResponse extends Message {
        static Message getDefaultInstance() {
            return null;
        }

        /**
         * <code>required int32 errorCode = 1;</code>
         */
        int getErrorCode();

        /**
         * <code>optional string errorMsg = 2;</code>
         */
        String getErrorMsg();

        interface Builder {
            Builder setErrorCode(int code);

            Builder setErrorMsg(String msg);

            ErrorResponse build();
        }

        public static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createErrorResponse();
        }
    }

    public interface InstallSnapshotRequest extends Message {
        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createInstallSnapshotRequest();
        }

        String getGroupId();

        String getServerId();

        String getPeerId();

        long getTerm();

        RaftOutter.SnapshotMeta getMeta();

        String getUri();

        interface Builder {
            InstallSnapshotRequest build();

            Builder setTerm(long term);

            Builder setGroupId(String groupId);

            Builder setServerId(String serverId);

            Builder setPeerId(String peerId);

            Builder setMeta(RaftOutter.SnapshotMeta meta);

            Builder setUri(String uri);
        }
    }

    public interface InstallSnapshotResponse extends Message {
        static Message getDefaultInstance() { // TODO asch remove https://issues.apache.org/jira/browse/IGNITE-14838
            return null;
        }

        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createInstallSnapshotResponse();
        }

        long getTerm();

        boolean getSuccess();

        interface Builder {
            InstallSnapshotResponse build();

            Builder setTerm(long currTerm);

            Builder setSuccess(boolean success);
        }
    }

    public interface TimeoutNowRequest extends Message {
        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createTimeoutNowRequest();
        }

        String getGroupId();

        String getServerId();

        String getPeerId();

        long getTerm();

        interface Builder {
            TimeoutNowRequest build();

            Builder setTerm(long term);

            Builder setGroupId(String groupId);

            Builder setServerId(String serverId);

            Builder setPeerId(String peerId);
        }
    }

    public interface TimeoutNowResponse extends Message {
        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createTimeoutNowResponse();
        }

        static Message getDefaultInstance() {
            return null;
        }

        /**
         * <code>required int64 term = 1;</code>
         */
        long getTerm();

        /**
         * <code>required bool success = 2;</code>
         */
        boolean getSuccess();

        interface Builder {
            TimeoutNowResponse build();

            Builder setTerm(long currTerm);

            Builder setSuccess(boolean success);
        }
    }

    public interface RequestVoteRequest extends Message {
        String getGroupId();

        String getServerId();

        String getPeerId();

        long getTerm();

        long getLastLogTerm();

        long getLastLogIndex();

        boolean getPreVote();

        interface Builder {
            Builder setPreVote(boolean preVote);

            Builder setGroupId(String groupId);

            Builder setServerId(String serverId);

            Builder setPeerId(String peerId);

            Builder setTerm(long currTerm);

            Builder setLastLogIndex(long index);

            Builder setLastLogTerm(long term);

            RequestVoteRequest build();
        }

        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createVoteRequest();
        }
    }

    public interface RequestVoteResponse extends Message {
        static Message getDefaultInstance() {
            return null;
        }

        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createVoteResponse();
        }

        /**
         * <code>required int64 term = 1;</code>
         */
        long getTerm();

        /**
         * <code>required bool granted = 2;</code>
         */
        boolean getGranted();

        interface Builder {
            RequestVoteResponse build();

            Builder setTerm(long currTerm);

            Builder setGranted(boolean granted);
        }
    }

    // TODO asch not needed https://issues.apache.org/jira/browse/IGNITE-14838
    public interface AppendEntriesRequestHeader extends Message {
        /**
         * <code>required string group_id = 1;</code>
         */
        String getGroupId();

        /**
         * <code>required string server_id = 2;</code>
         */
        String getServerId();

        /**
         * <code>required string peer_id = 3;</code>
         */
        String getPeerId();

        interface Builder {
            AppendEntriesRequestHeader build();
        }
    }

    public interface AppendEntriesRequest extends Message {
        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createAppendEntriesRequest();
        }

        String getGroupId();

        String getServerId();

        String getPeerId();

        long getTerm();

        long getPrevLogTerm();

        long getPrevLogIndex();

        java.util.List<RaftOutter.EntryMeta> getEntriesList();

        RaftOutter.EntryMeta getEntries(int index);

        int getEntriesCount();

        long getCommittedIndex();

        ByteString getData();

        boolean hasData();

        byte[] toByteArray();

        interface Builder {
            AppendEntriesRequest build();

            Builder setData(ByteString data);

            Builder setTerm(long term);

            Builder setGroupId(String groupId);

            Builder setServerId(String serverId);

            Builder setPeerId(String peerId);

            Builder setPrevLogIndex(long prevLogIndex);

            Builder setPrevLogTerm(long prevLogTerm);

            Builder setCommittedIndex(long lastCommittedIndex);

            Builder addEntries(RaftOutter.EntryMeta entryMeta);

            int getEntriesCount();
        }
    }

    public interface AppendEntriesResponse extends Message {
        static Message getDefaultInstance() {
            return null;
        }

        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createAppendEntriesResponse();
        }

        long getTerm();

        boolean getSuccess();

        long getLastLogIndex();

        interface Builder {
            AppendEntriesResponse build();

            Builder setSuccess(boolean success);

            Builder setTerm(long currTerm);

            Builder setLastLogIndex(long lastLogIndex);
        }
    }

    public interface GetFileRequest extends Message {
        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createGetFileRequest();
        }

        long getReaderId();

        String getFilename();

        long getCount();

        long getOffset();

        boolean getReadPartly();

        interface Builder {
            GetFileRequest build();

            long getReaderId();

            String getFilename();

            long getOffset();

            Builder setCount(long cnt);

            long getCount();

            Builder setOffset(long offset);

            Builder setReadPartly(boolean readPartly);

            Builder setFilename(String fileName);

            Builder setReaderId(long readerId);
        }
    }

    public interface GetFileResponse extends Message {
        static Message getDefaultInstance() {
            return null;
        }

        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createGetFileResponse();
        }

        boolean getEof();

        long getReadSize();

        ByteString getData();

        interface Builder {
            GetFileResponse build();

            Builder setReadSize(int read);

            Builder setEof(boolean eof);

            Builder setData(ByteString data);
        }
    }

    public interface ReadIndexRequest extends Message {
        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createReadIndexRequest();
        }

        String getGroupId();

        String getServerId();

        java.util.List<ByteString> getEntriesList();

        int getEntriesCount();

        ByteString getEntries(int index);

        String getPeerId();

        interface Builder {
            ReadIndexRequest build();

            Builder mergeFrom(ReadIndexRequest request);

            Builder setPeerId(String peerId);

            Builder setGroupId(String groupId);

            Builder setServerId(String serverId);

            Builder addEntries(ByteString data);
        }
    }

    public interface ReadIndexResponse extends Message {
        static Builder newBuilder() {
            return MessageBuilderFactory.DEFAULT.createReadIndexResponse();
        }

        static Message getDefaultInstance() {
            return null;
        }

        long getIndex();

        boolean getSuccess();

        interface Builder {
            ReadIndexResponse build();

            Builder setSuccess(boolean success);

            Builder setIndex(long lastCommittedIndex);
        }
    }
}
