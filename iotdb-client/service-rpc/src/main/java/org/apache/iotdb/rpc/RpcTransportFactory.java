/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.rpc;

import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransportFactory;

@SuppressWarnings("java:S1135") // ignore todos
public class RpcTransportFactory extends TTransportFactory {

  // TODO: make it a config
  public static boolean USE_SNAPPY = false;
  public static RpcTransportFactory INSTANCE;

  private static int thriftDefaultBufferSize = RpcUtils.THRIFT_DEFAULT_BUF_CAPACITY;
  private static int thriftMaxFrameSize = RpcUtils.THRIFT_FRAME_MAX_SIZE;

  static {
    reInit();
  }

  private final TTransportFactory inner;

  private RpcTransportFactory(TTransportFactory inner) {
    this.inner = inner;
  }

  @Override
  public TTransport getTransport(TTransport trans) throws TTransportException {
    updateConfigurationForAsyncServerIfNecessary(trans);
    return inner.getTransport(trans);
  }

  /**
   * The Thrift AsyncServer uses TMemoryInputTransport (<a
   * href="https://github.com/apache/thrift/blob/master/lib/java/src/main/java/org/apache/thrift/server/AbstractNonblockingServer.java#L291">...</a>),
   * which employs the default configuration and performs size checks during the read and write
   * processes. This behavior is not in line with our expectations of manually adjusting certain
   * parameters. Therefore, special handling is required for it in this function.
   *
   * @param trans TTransport
   */
  private void updateConfigurationForAsyncServerIfNecessary(TTransport trans) {
    if (trans instanceof TMemoryInputTransport) {
      trans
          .getConfiguration()
          .setRecursionLimit(TConfigurationConst.defaultTConfiguration.getRecursionLimit());
      trans
          .getConfiguration()
          .setMaxMessageSize(TConfigurationConst.defaultTConfiguration.getMaxMessageSize());
      trans
          .getConfiguration()
          .setMaxFrameSize(TConfigurationConst.defaultTConfiguration.getMaxFrameSize());
    }
  }

  public TTransport getTransportWithNoTimeout(String ip, int port) throws TTransportException {
    return inner.getTransport(new TSocket(TConfigurationConst.defaultTConfiguration, ip, port));
  }

  public TTransport getTransport(String ip, int port, int timeout) throws TTransportException {
    return inner.getTransport(
        new TSocket(TConfigurationConst.defaultTConfiguration, ip, port, timeout));
  }

  public static boolean isUseSnappy() {
    return USE_SNAPPY;
  }

  public static void setUseSnappy(boolean useSnappy) {
    USE_SNAPPY = useSnappy;
  }

  public static void setDefaultBufferCapacity(int thriftDefaultBufferSize) {
    RpcTransportFactory.thriftDefaultBufferSize = thriftDefaultBufferSize;
  }

  public static void setThriftMaxFrameSize(int thriftMaxFrameSize) {
    RpcTransportFactory.thriftMaxFrameSize = thriftMaxFrameSize;
  }

  public static void reInit() {
    INSTANCE =
        USE_SNAPPY
            ? new RpcTransportFactory(
                new TimeoutChangeableTSnappyFramedTransport.Factory(
                    thriftDefaultBufferSize, thriftMaxFrameSize))
            : new RpcTransportFactory(
                new TimeoutChangeableTFastFramedTransport.Factory(
                    thriftDefaultBufferSize, thriftMaxFrameSize));
  }
}
