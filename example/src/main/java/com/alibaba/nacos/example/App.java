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

package com.alibaba.nacos.example;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;

/**
 * Hello world.
 *
 * @author xxc
 */
public class App {
    public static void main(String[] args) throws NacosException, InterruptedException {
        Properties properties = new Properties();
        // nacos服务地址
        properties.setProperty("serverAddr", "127.0.0.1:8848");
        // 命名空间
//        properties.setProperty("namespace", "quickStart");

        NamingService naming = NamingFactory.createNamingService(properties);
        // 注册实例
        naming.registerInstance("nacos.test.3", "11.11.11.11", 8888, "TEST1");
        naming.registerInstance("nacos.test.3", "2.2.2.2", 9999, "DEFAULT");
        // 获取所有的实例
        System.out.println(naming.getAllInstances("nacos.test.3"));
        TimeUnit.SECONDS.sleep(1000);
    }
}
