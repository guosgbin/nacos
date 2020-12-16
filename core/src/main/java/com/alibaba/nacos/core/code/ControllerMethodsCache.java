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

package com.alibaba.nacos.core.code;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.core.auth.RequestMappingInfo;
import com.alibaba.nacos.core.auth.RequestMappingInfo.RequestMappingInfoComparator;
import com.alibaba.nacos.core.auth.condition.ParamRequestCondition;
import com.alibaba.nacos.core.auth.condition.PathRequestCondition;
import org.apache.commons.lang3.ArrayUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.alibaba.nacos.sys.env.Constants.REQUEST_PATH_SEPARATOR;


/**
 * Method cache.
 *
 * @author nkorange
 * @since 1.2.0
 */
@Component
public class ControllerMethodsCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerMethodsCache.class);

    @Value("${server.servlet.contextPath:/nacos}")
    private String contextPath;

    /**
     * 请求映射和方法的 map
     */
    private ConcurrentMap<RequestMappingInfo, Method> methods = new ConcurrentHashMap<>();

    /**
     * key urlkey eg.  POST--> PUT-->
     * value
     */
    private final ConcurrentMap<String, List<RequestMappingInfo>> urlLookup = new ConcurrentHashMap<>();

    /**
     * 获取匹配列表
     *
     * @param request
     * @return
     */
    public Method getMethod(HttpServletRequest request) {
        // 获取请求路径
        String path = getPath(request);
        if (path == null) {
            return null;
        }
        // 获取请求方式
        String httpMethod = request.getMethod();
        // 组装 eg.  POST-->
        String urlKey = httpMethod + REQUEST_PATH_SEPARATOR + path.replace(contextPath, "");
        // 从urlLookup获取映射列表
        List<RequestMappingInfo> requestMappingInfos = urlLookup.get(urlKey);
        if (CollectionUtils.isEmpty(requestMappingInfos)) {
            return null;
        }
        // 找到匹配的映射列表
        List<RequestMappingInfo> matchedInfo = findMatchedInfo(requestMappingInfos, request);
        if (CollectionUtils.isEmpty(matchedInfo)) {
            return null;
        }
        RequestMappingInfo bestMatch = matchedInfo.get(0);
        if (matchedInfo.size() > 1) {
            // 假如匹配到了多个映射
            RequestMappingInfoComparator comparator = new RequestMappingInfoComparator();
            matchedInfo.sort(comparator);
            // 根据映射注解参数的多少 来排序 拿到第一个映射      RequestMapping#params()
            bestMatch = matchedInfo.get(0);
            RequestMappingInfo secondBestMatch = matchedInfo.get(1);
            if (comparator.compare(bestMatch, secondBestMatch) == 0) {
                // 如果两个请求路径注解的参数个数的相等 则抛异常  RequestMapping#params()
                throw new IllegalStateException(
                        "Ambiguous methods mapped for '" + request.getRequestURI() + "': {" + bestMatch + ", "
                                + secondBestMatch + "}");
            }
        }
        return methods.get(bestMatch);
    }

    /**
     * 获取请求路径
     *
     * @param request
     * @return
     */
    private String getPath(HttpServletRequest request) {
        String path = null;
        try {
            path = new URI(request.getRequestURI()).getPath();
        } catch (URISyntaxException e) {
            LOGGER.error("parse request to path error", e);
        }
        return path;
    }

    /**
     * 找出匹配的请求
     *
     * @param requestMappingInfos
     * @param request
     * @return
     */
    private List<RequestMappingInfo> findMatchedInfo(List<RequestMappingInfo> requestMappingInfos,
            HttpServletRequest request) {
        List<RequestMappingInfo> matchedInfo = new ArrayList<>();
        for (RequestMappingInfo requestMappingInfo : requestMappingInfos) {
            // 遍历所有映射 判断路径是否匹配
            ParamRequestCondition matchingCondition = requestMappingInfo.getParamRequestCondition().getMatchingCondition(request);
            if (matchingCondition != null) {
                matchedInfo.add(requestMappingInfo);
            }
        }
        return matchedInfo;
    }

    /**
     * find target method from this package.
     *
     * @param packageName package name
     */
    public void initClassMethod(String packageName) {
        Reflections reflections = new Reflections(packageName);
        // 找出指定包下有RequestMapping注解的类
        Set<Class<?>> classesList = reflections.getTypesAnnotatedWith(RequestMapping.class);

        for (Class clazz : classesList) {
            initClassMethod(clazz);
        }
    }

    /**
     * find target method from class list.
     *
     * @param classesList class list
     */
    public void initClassMethod(Set<Class<?>> classesList) {
        for (Class clazz : classesList) {
            initClassMethod(clazz);
        }
    }

    /**
     * find target method from target class.
     * 找到从类中找到指定的方法
     *
     * @param clazz {@link Class}
     */
    private void initClassMethod(Class<?> clazz) {
        // 获取方法上的注解
        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
        // 遍历注解的calue属性
        for (String classPath : requestMapping.value()) {
            // 遍历类中的方法
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    // 假如当前方法上面不是 RequestMapping注解，尝试去查找是否是 GetMapping PostMapping这些注解
                    // 组装这些方法到 urlLookup和methods的map
                    parseSubAnnotations(method, classPath);
                    continue;
                }
                requestMapping = method.getAnnotation(RequestMapping.class);
                // 获取方法上的所有的请求方式
                RequestMethod[] requestMethods = requestMapping.method();
                if (requestMethods.length == 0) {
                    requestMethods = new RequestMethod[1];
                    requestMethods[0] = RequestMethod.GET; // 默认get
                }
                for (String methodPath : requestMapping.value()) {
                    String urlKey = requestMethods[0].name() + REQUEST_PATH_SEPARATOR + classPath + methodPath;
                    // 组装这些方法到 urlLookup和methods的map
                    addUrlAndMethodRelation(urlKey, requestMapping.params(), method);
                }
            }
        }
    }

    private void parseSubAnnotations(Method method, String classPath) {

        final GetMapping getMapping = method.getAnnotation(GetMapping.class);
        final PostMapping postMapping = method.getAnnotation(PostMapping.class);
        final PutMapping putMapping = method.getAnnotation(PutMapping.class);
        final DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        final PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);

        if (getMapping != null) {
            put(RequestMethod.GET, classPath, getMapping.value(), getMapping.params(), method);
        }

        if (postMapping != null) {
            put(RequestMethod.POST, classPath, postMapping.value(), postMapping.params(), method);
        }

        if (putMapping != null) {
            put(RequestMethod.PUT, classPath, putMapping.value(), putMapping.params(), method);
        }

        if (deleteMapping != null) {
            put(RequestMethod.DELETE, classPath, deleteMapping.value(), deleteMapping.params(), method);
        }

        if (patchMapping != null) {
            put(RequestMethod.PATCH, classPath, patchMapping.value(), patchMapping.params(), method);
        }

    }

    private void put(RequestMethod requestMethod, String classPath, String[] requestPaths, String[] requestParams,
            Method method) {
        if (ArrayUtils.isEmpty(requestPaths)) {
            // urlKey eg. Post-->/get/something
            String urlKey = requestMethod.name() + REQUEST_PATH_SEPARATOR + classPath;
            addUrlAndMethodRelation(urlKey, requestParams, method);
            return;
        }
        // eg "GET-->/v1/auth/search"
        for (String requestPath : requestPaths) {
            String urlKey = requestMethod.name() + REQUEST_PATH_SEPARATOR + classPath + requestPath;
            addUrlAndMethodRelation(urlKey, requestParams, method);
        }
    }

    /**
     * 添加url和方法的关系
     *
     * @param urlKey
     * @param requestParam
     * @param method
     */
    private void addUrlAndMethodRelation(String urlKey, String[] requestParam, Method method) {
        RequestMappingInfo requestMappingInfo = new RequestMappingInfo();
        requestMappingInfo.setPathRequestCondition(new PathRequestCondition(urlKey));
        requestMappingInfo.setParamRequestCondition(new ParamRequestCondition(requestParam));
        List<RequestMappingInfo> requestMappingInfos = urlLookup.get(urlKey);
        if (requestMappingInfos == null) {
            // 添加此url到map
            urlLookup.putIfAbsent(urlKey, new ArrayList<>());
            requestMappingInfos = urlLookup.get(urlKey);
        }
        requestMappingInfos.add(requestMappingInfo);
        methods.put(requestMappingInfo, method);
    }

    public String getContextPath() {
        return contextPath;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
