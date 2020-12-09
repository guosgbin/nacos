/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.nacos.common.executor;

import com.alibaba.nacos.common.utils.StringUtils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Name thread factory.
 *
 * 实现自己的ThreadFactory，因为JDK默认的线程工厂的线程名称太统一了，
 * 使用自己传入的名字可以很好的查看线程是干什么的
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public class NameThreadFactory implements ThreadFactory {

    /**
     * 线程名称自增
     */
    private final AtomicInteger id = new AtomicInteger(0);

    /**
     * 线程名称前缀，真正的线程名称加上原子自增的变量的值
     */
    private String name;

    public NameThreadFactory(String name) {
        // 给线程名字前缀加个点
        if (!name.endsWith(StringUtils.DOT)) {
            name += StringUtils.DOT;
        }
        this.name = name;
    }

    /**
     * 创建一个新的线程
     * @param r
     * @return
     */
    @Override
    public Thread newThread(Runnable r) {
        // 自定义线程名称
        String threadName = name + id.getAndDecrement();
        Thread thread = new Thread(r, threadName);
        thread.setDaemon(true);
        return thread;
    }
}
