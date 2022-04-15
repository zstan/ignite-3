/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// XMLDoc check fails on older SDKs: https://github.com/dotnet/roslyn/issues/44571.
#pragma warning disable CS1572
#pragma warning disable CS1573
namespace Apache.Ignite.Internal
{
    using System;
    using Ignite.Network;

    /// <summary>
    /// Socket connection context.
    /// </summary>
    /// <param name="Version">Protocol version.</param>
    /// <param name="IdleTimeout">Server idle timeout.</param>
    /// <param name="ClusterNode">Cluster node.</param>
    internal record ConnectionContext(ClientProtocolVersion Version, TimeSpan IdleTimeout, IClusterNode ClusterNode);
}