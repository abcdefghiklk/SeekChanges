/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hdfs.protocol;

import java.io.IOException;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.fs.BatchedRemoteIterator;

/**
 * CachePoolIterator is a remote iterator that iterates cache pools.
 * It supports retrying in case of namenode failover.
 */
@InterfaceAudience.Private
@InterfaceStability.Evolving
public class CachePoolIterator
    extends BatchedRemoteIterator<String, CachePoolEntry> {

  private final ClientProtocol namenode;

  public CachePoolIterator(ClientProtocol namenode) {
    super("");
    this.namenode = namenode;
  }

  @Override
  public BatchedEntries<CachePoolEntry> makeRequest(String prevKey)
      throws IOException {
    return namenode.listCachePools(prevKey);
  }

  @Override
  public String elementToPrevKey(CachePoolEntry entry) {
    return entry.getInfo().getPoolName();
  }
}
