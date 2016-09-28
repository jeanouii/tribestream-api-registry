/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.tomitribe.tribestream.registryng.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import org.apache.openejb.observer.Observes;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HazelcastEventToBrokerBridge {

    private static final Logger LOGGER = Logger.getLogger(HazelcastEventToBrokerBridge.class.getName());
    public static final String CLUSTER_NAME_KEY = "tribe.cluster.group.name";

    private final String clusterGroupeName;
    private final ITopic<ClusterEventWrapper> topic;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);

    public HazelcastEventToBrokerBridge() {

        // for the moment, let's make this a property we read at the beginning and for the life of the JVM
        clusterGroupeName = System.getProperty(CLUSTER_NAME_KEY, "tribe");

        if (clusterGroupeName == null || clusterGroupeName.trim().isEmpty()) {
            LOGGER.log(Level.FINEST, "Cluster group name is null or empty. Set properly the system property {0}.", CLUSTER_NAME_KEY);
        }

        // todo add some configuration capabilities?
        HazelcastInstance hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(new Config(clusterGroupeName));

        topic = hazelcastInstance.getReliableTopic("tribe-event-broker");

        final String uuid = hazelcastInstance.getCluster().getLocalMember().getUuid();
        LOGGER.log(Level.INFO, "Hazelcast successfully initialized. Current instance is {0}", uuid);

        // todo add an observer for shutdown. Using shutdown hook for now
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (isRunning.compareAndSet(true, false)) {
                    LOGGER.log(Level.INFO, "Shutting down Hazelcast. No more @Cluster events are going to be broadcasted.");
                    // hazelcastInstance.shutdown();
                }
            }
        });
    }

    public void send(@Observes final Object eventObject) {

        final Cluster cluster = eventObject.getClass().getAnnotation(Cluster.class);
        if (cluster == null) {
            LOGGER.log(Level.FINEST, "Object {0} is not annotated with @Cluster. Ignoring.", eventObject.getClass().getName());
            return;
        }

        LOGGER.log(Level.FINEST, "Object {0} is will be broadcasted to cluster with group name {1}.",
                new Object[] { eventObject.getClass().getName(), clusterGroupeName} );

        if (isRunning.get()) {

            // todo deal with headers - should at least put current node identifier to avoid loops on the receiving side
            topic.publish(new ClusterEventWrapper(eventObject));

        } else {
            LOGGER.log(Level.INFO, "System shutting down. Ignoring event type {0} for group name {1}.",
                    new Object[] { eventObject.getClass().getName(), clusterGroupeName} );
        }
    }

}
