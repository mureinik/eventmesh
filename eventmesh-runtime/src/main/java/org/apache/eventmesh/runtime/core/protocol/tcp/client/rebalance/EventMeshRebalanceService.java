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

package org.apache.eventmesh.runtime.core.protocol.tcp.client.rebalance;

import org.apache.eventmesh.common.EventMeshThreadFactory;
import org.apache.eventmesh.common.ThreadPoolFactory;
import org.apache.eventmesh.runtime.boot.EventMeshTCPServer;
import org.apache.eventmesh.runtime.util.EventMeshUtil;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventMeshRebalanceService {

    private final EventMeshTCPServer eventMeshTCPServer;

    private final Integer rebalanceIntervalMills;

    private final EventMeshRebalanceStrategy rebalanceStrategy;

    private ScheduledExecutorService serviceRebalanceScheduler;

    public EventMeshRebalanceService(EventMeshTCPServer eventMeshTCPServer, EventMeshRebalanceStrategy rebalanceStrategy) {
        this.eventMeshTCPServer = eventMeshTCPServer;
        this.rebalanceStrategy = rebalanceStrategy;
        this.rebalanceIntervalMills = eventMeshTCPServer.getEventMeshTCPConfiguration().getEventMeshTcpRebalanceIntervalInMills();
    }

    public void init() {
        this.serviceRebalanceScheduler = ThreadPoolFactory.createScheduledExecutor(5, new EventMeshThreadFactory("proxy-rebalance-sch", true));
        log.info("rebalance service inited ......");
    }

    public void start() throws Exception {
        rebalanceStrategy.doRebalance();
        serviceRebalanceScheduler.scheduleAtFixedRate(() -> {
            try {
                rebalanceStrategy.doRebalance();
            } catch (Exception ex) {
                log.error("RebalanceByService failed", ex);
            }
        }, rebalanceIntervalMills, rebalanceIntervalMills, TimeUnit.MILLISECONDS);
        log.info("rebalance service started......");
    }

    public void shutdown() {
        this.serviceRebalanceScheduler.shutdown();
        log.info("rebalance service shutdown......");
    }

    public void printRebalanceThreadPoolState() {
        EventMeshUtil.printState((ThreadPoolExecutor) serviceRebalanceScheduler);
    }
}
