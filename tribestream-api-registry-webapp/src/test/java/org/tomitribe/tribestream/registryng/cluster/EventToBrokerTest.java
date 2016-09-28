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
import org.apache.ziplock.maven.Mvn;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.tribestream.registryng.resources.Registry;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@RunWith(Arquillian.class)
public class EventToBrokerTest {

    @Deployment(testable = false)
    public static WebArchive webArchive() {
        return new Mvn.Builder()
                .name("registry.war")
                .build(WebArchive.class)
                .addPackages(true, "org.tomitribe");
    }

    @ArquillianResource
    private URL base;

    private static HazelcastInstance hazelcastInstance;
    private static ITopic<ClusterEventWrapper> topic;

    @BeforeClass
    public static void startupHazelcast() {
        hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(new Config("tribe"));

        Assert.assertNotNull(hazelcastInstance);

        topic = hazelcastInstance.getReliableTopic("tribe-event-broker");
        Assert.assertNotNull(topic);
    }

    @AfterClass
    public static void shutdownHazelcast() {
        if (hazelcastInstance != null && hazelcastInstance.getLifecycleService().isRunning()) {
            hazelcastInstance.getLifecycleService().shutdown();
        }
    }

    @Test
    public void send() throws InterruptedException {

        final List<Message<ClusterEventWrapper>> events = new ArrayList<>();
        topic.addMessageListener(message -> {
                    System.out.println("Received >>>>> " + message.getPublishingMember().getUuid() + " --- " + message.getMessageObject());
                    events.add(message);
                }
        );


        final String result = new Registry()
                .client()
                .target(base.toExternalForm())
                .path("/api/foo/bar")
                .request()
                .get(String.class);

        // make sure the event can be received
        for (int i = 0 ; i < 5 ; i++) {
            if (events.size() == 1) break;
            Thread.sleep(1000);
        }

        final Message<ClusterEventWrapper> clusterEventWrapperMessage = events.get(0);
        final FakeEventSender.MySecretEvent event = (FakeEventSender.MySecretEvent) clusterEventWrapperMessage.getMessageObject().event;
        final String uuid = hazelcastInstance.getCluster().getLocalMember().getUuid();

        // make sure the member isn't the same otherwise, we aren't testing anything
        Assert.assertTrue(!clusterEventWrapperMessage.getPublishingMember().getUuid().equals(uuid));
        Assert.assertEquals("tintin", event.getName());

    }

}
