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

import com.alibaba.nacos.common.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * // TODO Access Metric.
 *
 * <p>For unified management of thread pool resources, the consumer can simply call the register method to {@link
 * ThreadPoolManager#register(String, String, ExecutorService)} the thread pool that needs to be included in the life
 * cycle management of the resource
 *
 * 管理线程池的资源
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
public final class ThreadPoolManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolManager.class);

    /**
     * 第一个key String 是namespace
     * 第二个key String 是group
     */
    private Map<String, Map<String, Set<ExecutorService>>> resourcesManager;

    private Map<String, Object> lockers = new ConcurrentHashMap<String, Object>(8);

    private static final ThreadPoolManager INSTANCE = new ThreadPoolManager();

    /** 线程池管理对象是否关闭  标志 */
    private static final AtomicBoolean CLOSED = new AtomicBoolean(false);

    static {
        // 初始化resourcesManager
        INSTANCE.init();
        // 添加一个钩子函数，在虚拟机退出的时候打印日志
        ThreadUtils.addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                LOGGER.warn("[ThreadPoolManager] Start destroying ThreadPool");
                shutdown(); // 关闭此对象
                LOGGER.warn("[ThreadPoolManager] Destruction of the end");
            }
        }));
    }

    public static ThreadPoolManager getInstance() {
        return INSTANCE;
    }

    private ThreadPoolManager() {
    }

    /**
     * 初始化resourcesManager
     */
    private void init() {
        resourcesManager = new ConcurrentHashMap<String, Map<String, Set<ExecutorService>>>(8);
    }

    /**
     * Register the thread pool resources with the resource manager.
     *
     * 向ThreadPoolManager管理对象 注册 线程池资源
     *
     * @param namespace namespace name 命名空间
     * @param group     group name 组名
     * @param executor  {@link ExecutorService} 线程池
     */
    public void register(String namespace, String group, ExecutorService executor) {
        // 假如resourcesManager中不包含此namespace，则参加一个对象作为锁对象
        if (!resourcesManager.containsKey(namespace)) {
            synchronized (this) {
                lockers.put(namespace, new Object());
            }
        }
        // 获取锁对象，对此对象加锁
        final Object monitor = lockers.get(namespace);
        synchronized (monitor) {
            Map<String, Set<ExecutorService>> map = resourcesManager.get(namespace);
            if (map == null) {
                // 初始化map
                map = new HashMap<String, Set<ExecutorService>>(8);
                map.put(group, new HashSet<ExecutorService>());
                map.get(group).add(executor);
                resourcesManager.put(namespace, map);
                return;
            }
            if (!map.containsKey(group)) {
                // map中没有此group，则插入数据
                map.put(group, new HashSet<ExecutorService>());
            }
            // 添加线程池对象到resourcesManager
            map.get(group).add(executor);
        }
    }

    /**
     * Cancel the uniform lifecycle management for all threads under this resource.
     *
     * resourcesManager 剔除 组里面所有线程池的线程
     *
     * @param namespace namespace name 命名空间
     * @param group     group name 组名
     */
    public void deregister(String namespace, String group) {
        // 获取锁对象，加锁，剔除 线程池
        if (resourcesManager.containsKey(namespace)) {
            final Object monitor = lockers.get(namespace);
            synchronized (monitor) {
                resourcesManager.get(namespace).remove(group);
            }
        }
    }

    /**
     * Undoing the uniform lifecycle management of {@link ExecutorService} under this resource.
     *
     * resourcesManager 剔除 组里面指定线程池的线程
     *
     * @param namespace namespace name
     * @param group     group name
     * @param executor  {@link ExecutorService}
     */
    public void deregister(String namespace, String group, ExecutorService executor) {
        // 获取锁对象，加锁，剔除 线程池
        if (resourcesManager.containsKey(namespace)) {
            final Object monitor = lockers.get(namespace);
            synchronized (monitor) {
                final Map<String, Set<ExecutorService>> subResourceMap = resourcesManager.get(namespace);
                if (subResourceMap.containsKey(group)) {
                    subResourceMap.get(group).remove(executor);
                }
            }
        }
    }

    /**
     * Destroys all thread pool resources under this namespace.
     *
     * resourcesManager 剔除 命名空间 里面所有 线程池的线程
     *
     * @param namespace namespace
     */
    public void destroy(final String namespace) {
        // 获取锁对象
        final Object monitor = lockers.get(namespace);
        if (monitor == null) {
            return;
        }
        synchronized (monitor) {
            // 加锁
            Map<String, Set<ExecutorService>> subResource = resourcesManager.get(namespace);
            if (subResource == null) {
                return;
            }
            for (Map.Entry<String, Set<ExecutorService>> entry : subResource.entrySet()) {
                for (ExecutorService executor : entry.getValue()) {
                    // 关闭线程池
                    ThreadUtils.shutdownThreadPool(executor);
                }
            }
            resourcesManager.get(namespace).clear();
            resourcesManager.remove(namespace);
        }
    }

    /**
     * This namespace destroys all thread pool resources under the grouping.
     *
     * 剔除 resourcesManager 组里面指定线程池的线程
     *
     * @param namespace namespace
     * @param group     group
     */
    public void destroy(final String namespace, final String group) {
        final Object monitor = lockers.get(namespace);
        if (monitor == null) {
            return;
        }
        // 加锁
        synchronized (monitor) {
            Map<String, Set<ExecutorService>> subResource = resourcesManager.get(namespace);
            if (subResource == null) {
                return;
            }
            Set<ExecutorService> waitDestroy = subResource.get(group);
            for (ExecutorService executor : waitDestroy) {
                ThreadUtils.shutdownThreadPool(executor);
            }
            resourcesManager.get(namespace).remove(group);
        }
    }

    /**
     * Shutdown thread pool manager.
     * 关闭 线程池管理 对象
     */
    public static void shutdown() {
        // CAS尝试更新CLOSE标志值，假如更新失败，则说明已经被其他线程关闭了
        if (!CLOSED.compareAndSet(false, true)) {
            return;
        }
        // 获取所有的命名空间，剔除每个命名空间下的线程池
        Set<String> namespaces = INSTANCE.resourcesManager.keySet();
        for (String namespace : namespaces) {
            INSTANCE.destroy(namespace);
        }
    }

}
