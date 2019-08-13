/**
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
package io.chubao.joyqueue.service.impl;

import io.chubao.joyqueue.model.PageResult;
import io.chubao.joyqueue.model.QPageQuery;
import io.chubao.joyqueue.model.domain.Namespace;
import io.chubao.joyqueue.model.query.QNamespace;
import io.chubao.joyqueue.service.NamespaceService;
import io.chubao.joyqueue.nsr.NameSpaceServerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 命名空间服务实现
 * Created by chenyanying3 on 2018-11-18.
 */
@Service("namespaceService")
public class NamespaceServiceImpl implements NamespaceService {
    private final Logger logger = LoggerFactory.getLogger(NamespaceServiceImpl.class);

    @Autowired
    private NameSpaceServerService nameSpaceServerService;


    @Override
    public Namespace findByCode(String code) {
        return nameSpaceServerService.findByCode(code);
    }

    @Override
    public Namespace findById(String s) throws Exception {
        return nameSpaceServerService.findById(s);
    }

    @Override
    public PageResult<Namespace> findByQuery(QPageQuery<QNamespace> query) {
        try {
            return nameSpaceServerService.findByQuery(query);
        } catch (Exception e) {
            logger.error("findByQuery exception",e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int add(Namespace model) {
        try {
            return nameSpaceServerService.add(model);
        } catch (Exception e) {
            throw new RuntimeException("add",e);
        }
    }

    @Override
    public int delete(Namespace model) {
        try {
            return nameSpaceServerService.delete(model);
        } catch (Exception e) {
            throw new RuntimeException("delete", e);
        }
    }

    @Override
    public int update(Namespace model) {
        try {
            return nameSpaceServerService.update(model);
        } catch (Exception e) {
            throw new RuntimeException("update",e);
        }
    }

    @Override
    public List<Namespace> findByQuery(QNamespace query) throws Exception {
        return nameSpaceServerService.findByQuery(query);
    }

}