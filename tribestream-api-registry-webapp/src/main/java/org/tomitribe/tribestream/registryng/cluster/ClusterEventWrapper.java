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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClusterEventWrapper implements Serializable {

    // todo work on serialisation
    final transient Map<String, String> headers = new HashMap<>();

    final Object event;

    public ClusterEventWrapper(final Object event) {
        this(new HashMap<>(), event);
    }

    public ClusterEventWrapper(final Map<String, String> headers, final Object event) {
        this.event = event;
        this.headers.putAll(headers);
    }

    public Object getEvent() {
        return event;
    }

    public String getHeader(final String name) {
        return headers.get(name);
    }

    public Set<String> headersName() {
        return headers.keySet();
    }
}
