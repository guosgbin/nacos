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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Unified thread pool creation factory, and actively create thread pool resources by ThreadPoolManager for unified life
 * cycle management {@link ExecutorFactory.Managed}.
 *
 * <p>Unified thread pool creation factory without life cycle management {@link ExecutorFactory}.
 *
 * <p>two check style ignore will be removed after issue#2856 finished.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
@SuppressWarnings({"PMD.ThreadPoolCreationRule", "checkstyle:overloadmethodsdeclarationorder",
        "checkstyle:missingjavadocmethod"})
public final class ExecutorFactory {

    /**
     * 创建只有一个线程的线程池
     * 使用默认的线程工厂
     */
    public static ExecutorService newSingleExecutorService() {
        return Executors.newFixedThreadPool(1);
    }

    /**
     * 创建只有一个线程的线程池
     * 使用指定的线程工厂
     */
    public static ExecutorService newSingleExecutorService(final ThreadFactory threadFactory) {
        return Executors.newFixedThreadPool(1, threadFactory);
    }

    /**
     * 创建指定个数的线程的线程池
     * 使用默认的线程工厂
     */
    public static ExecutorService newFixedExecutorService(final int nThreads) {
        return Executors.newFixedThreadPool(nThreads);
    }

    /**
     * 创建指定个数的线程的线程池
     * 使用指定的线程工厂
     */
    public static ExecutorService newFixedExecutorService(final int nThreads, final ThreadFactory threadFactory) {
        return Executors.newFixedThreadPool(nThreads, threadFactory);
    }

    /**
     * 创建只有单个线程的 周期性执行任务的 的线程池
     * 使用指定的线程工厂
     */
    public static ScheduledExecutorService newSingleScheduledExecutorService(final ThreadFactory threadFactory) {
        return Executors.newScheduledThreadPool(1, threadFactory);
    }

    /**
     * 创建指定个数的线程的 周期性执行任务的 线程池
     * 使用指定的线程工厂
     */
    public static ScheduledExecutorService newScheduledExecutorService(final int nThreads,
            final ThreadFactory threadFactory) {
        return Executors.newScheduledThreadPool(nThreads, threadFactory);
    }

    /**
     * 创建自定义线程池
     *
     * @param coreThreads 核心线程数
     * @param maxThreads 最大线程数
     * @param keepAliveTimeMs 空闲线程超时时间
     * @param threadFactory 指定线程工厂
     * @return
     */
    public static ThreadPoolExecutor newCustomerThreadExecutor(final int coreThreads, final int maxThreads,
            final long keepAliveTimeMs, final ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(coreThreads, maxThreads, keepAliveTimeMs, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), threadFactory);
    }

    public static final class Managed {

        /** 默认的命名空间 */
        private static final String DEFAULT_NAMESPACE = "nacos";

        /** 线程池生命周期管理，资源管理的对象 */
        private static final ThreadPoolManager THREAD_POOL_MANAGER = ThreadPoolManager.getInstance();

        /**
         * Create a new single executor service with default thread factory and register to manager.
         *
         * 创建一个拥有单个线程的线程池，并将其注册到 线程资源管理对象
         * 默认的命名空间 nacos     JDK默认线程工厂
         *
         * @param group group name
         * @return new single executor service
         */
        public static ExecutorService newSingleExecutorService(final String group) {
            ExecutorService executorService = Executors.newFixedThreadPool(1);
            THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executorService);
            return executorService;
        }

        /**
         * Create a new single executor service with input thread factory and register to manager.
         *
         * 创建一个拥有单个线程的线程池，并将其注册到 线程资源管理对象
         * 默认的命名空间 nacos     指定线程工厂
         *
         * @param group         group name
         * @param threadFactory thread factory
         * @return new single executor service
         */
        public static ExecutorService newSingleExecutorService(final String group, final ThreadFactory threadFactory) {
            ExecutorService executorService = Executors.newFixedThreadPool(1, threadFactory);
            THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executorService);
            return executorService;
        }

        /**
         * Create a new fixed executor service with default thread factory and register to manager.
         *
         * 创建一个拥有指定线程数的线程池，并将其注册到 线程资源管理对象
         * 默认的命名空间 nacos     JDK默认的线程工厂
         *
         *
         * @param group    group name
         * @param nThreads thread number
         * @return new fixed executor service
         */
        public static ExecutorService newFixedExecutorService(final String group, final int nThreads) {
            ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
            THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executorService);
            return executorService;
        }

        /**
         * Create a new fixed executor service with input thread factory and register to manager.
         *
         * 创建一个拥有指定线程数的线程池，并将其注册到 线程资源管理对象
         * 默认的命名空间 nacos    指定线程工厂
         *
         * @param group         group name
         * @param nThreads      thread number
         * @param threadFactory thread factory
         * @return new fixed executor service
         */
        public static ExecutorService newFixedExecutorService(final String group, final int nThreads,
                final ThreadFactory threadFactory) {
            ExecutorService executorService = Executors.newFixedThreadPool(nThreads, threadFactory);
            THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executorService);
            return executorService;
        }

        /**
         * Create a new single scheduled executor service with input thread factory and register to manager.
         *
         * 创建一个拥有一个线程数的 周期性 线程池，并将其注册到 线程资源管理对象
         * 默认的命名空间 nacos    指定线程工厂
         *
         *
         * @param group         group name
         * @param threadFactory thread factory
         * @return new single scheduled executor service
         */
        public static ScheduledExecutorService newSingleScheduledExecutorService(final String group,
                final ThreadFactory threadFactory) {
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, threadFactory);
            THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executorService);
            return executorService;
        }

        /**
         * Create a new scheduled executor service with input thread factory and register to manager.
         *
         * 创建一个拥有指定线程数的 周期性 线程池，并将其注册到 线程资源管理对象
         * 默认的命名空间 nacos    指定线程工厂
         *
         * @param group         group name
         * @param nThreads      thread number
         * @param threadFactory thread factory
         * @return new scheduled executor service
         */
        public static ScheduledExecutorService newScheduledExecutorService(final String group, final int nThreads,
                final ThreadFactory threadFactory) {
            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(nThreads, threadFactory);
            THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executorService);
            return executorService;
        }

        /**
         * Create a new custom executor service and register to manager.
         *
         * 创建一个自定义的线程工厂
         *
         * @param group           group name 组名
         * @param coreThreads     core thread number 核心线程数
         * @param maxThreads      max thread number 最大线程数
         * @param keepAliveTimeMs keep alive time milliseconds 线程超时时间
         * @param threadFactory   thread facotry 线程工厂
         * @return new custom executor service
         */
        public static ThreadPoolExecutor newCustomerThreadExecutor(final String group, final int coreThreads,
                final int maxThreads, final long keepAliveTimeMs, final ThreadFactory threadFactory) {
            ThreadPoolExecutor executor = new ThreadPoolExecutor(coreThreads, maxThreads, keepAliveTimeMs,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
            THREAD_POOL_MANAGER.register(DEFAULT_NAMESPACE, group, executor);
            return executor;
        }
    }
}
