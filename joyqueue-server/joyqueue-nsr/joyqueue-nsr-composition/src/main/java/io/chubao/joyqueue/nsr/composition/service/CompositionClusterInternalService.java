/**
 * Copyright 2019 The JoyQueue Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.chubao.joyqueue.nsr.composition.service;

import io.chubao.joyqueue.nsr.composition.config.CompositionConfig;
import io.chubao.joyqueue.nsr.service.internal.ClusterInternalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

/**
 * CompositionClusterInternalService
 * author: gaohaoxiang
 * date: 2019/10/31
 */
public class CompositionClusterInternalService implements ClusterInternalService {

    protected static final Logger logger = LoggerFactory.getLogger(CompositionClusterInternalService.class);

    private CompositionConfig config;
    private ClusterInternalService igniteClusterInternalService;
    private ClusterInternalService journalkeeperClusterInternalService;

    public CompositionClusterInternalService(CompositionConfig config, ClusterInternalService igniteClusterInternalService, ClusterInternalService journalkeeperClusterInternalService) {
        this.config = config;
        this.igniteClusterInternalService = igniteClusterInternalService;
        this.journalkeeperClusterInternalService = journalkeeperClusterInternalService;
    }

    @Override
    public String getCluster() {
        return journalkeeperClusterInternalService.getCluster();
    }

    @Override
    public String addNode(URI uri) {
        return journalkeeperClusterInternalService.addNode(uri);
    }

    @Override
    public String removeNode(URI uri) {
        return journalkeeperClusterInternalService.removeNode(uri);
    }

    @Override
    public String updateNodes(List<URI> uris) {
        return journalkeeperClusterInternalService.updateNodes(uris);
    }
}