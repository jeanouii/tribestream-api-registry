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
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.observer.Observes;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
public class HazelcastBrokerToEventBridge implements MessageListener<ClusterEventWrapper> {

    private static final Logger LOGGER = Logger.getLogger(HazelcastBrokerToEventBridge.class.getName());
    public static final String CLUSTER_NAME_KEY = "tribe.cluster.group.name";

    private final String clusterGroupeName;
    private final ITopic<ClusterEventWrapper> topic;

    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private String listenerId = null;
    private String instanceId = null;

    public HazelcastBrokerToEventBridge() {

        // for the moment, let's make this a property we read at the beginning and for the life of the JVM
        clusterGroupeName = System.getProperty(CLUSTER_NAME_KEY, "tribe");

        if (clusterGroupeName == null || clusterGroupeName.trim().isEmpty()) {
            LOGGER.log(Level.FINEST, "Cluster group name is null or empty. Set properly the system property {0}.", CLUSTER_NAME_KEY);
        }

        // todo add some configuration capabilities?
        HazelcastInstance hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(new Config(clusterGroupeName));

        topic = hazelcastInstance.getReliableTopic("tribe-event-broker");

        instanceId = hazelcastInstance.getCluster().getLocalMember().getUuid();
        LOGGER.log(Level.INFO, "Hazelcast successfully initialized. Current instance is {0}", instanceId);

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

    @PostConstruct
    public void startListening() {

        // could be done right in the constructor too, just a bit weird to do it because the application
        // isn't fully deployed so not really able to do anything with the messages
        listenerId = topic.addMessageListener(this);
    }

    @PreDestroy
    public void stopListening() {
        if (listenerId != null) {
            topic.removeMessageListener(listenerId);
        }
    }

    @Override
    public void onMessage(final Message<ClusterEventWrapper> message) {

        // check if message comes from this instance
        final String uuid = message.getPublishingMember().getUuid();
        if (uuid.equals(instanceId)) {
            LOGGER.log(Level.FINEST, "Ignoring brodcasted event because it comes from this instance {0}.", instanceId);
            return;
        }

        // issue here is that this event still has the annotation and therefor the event will be brodcasted
        // so we have to dynamically remove the annotation from it
        final Object event = message.getMessageObject().getEvent();
        SystemInstance.get().fireEvent(event);
    }
}
