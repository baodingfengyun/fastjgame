/*
 *  Copyright 2019 wjybxx
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to iBn writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.wjybxx.fastjgame.reflect;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类型参数匹配器
 *
 * @author wjybxx
 * @version 1.0
 * date - 2019/9/26
 * github - https://github.com/hl845740757
 */
public abstract class TypeParameterMatcher {

    /**
     * 它会导致一定程度的内存泄漏，不过应该不多
     */
    private static final ThreadLocal<FindCache> LOCAL_FIND_CACHE = ThreadLocal.withInitial(FindCache::new);

    /**
     * 查询是否与泛型参数匹配
     *
     * @param object 待检测对象
     * @return true/false
     */
    public abstract boolean match(@Nonnull Object object);


    /**
     * 查找父类/父接口定义的且被子类声明为具体类型的泛型参数的具体类型。
     * 查找结果不会加入缓存，适合用在起服/初始化阶段。
     *
     * @param instance              superClazzOrInterface的子类实例
     * @param superClazzOrInterface 泛型参数typeParamName存在的类,class或interface
     * @param typeParamName         泛型参数名字
     * @param <T>                   约束必须有继承关系或实现关系
     * @return 如果定义的泛型存在，则返回对应的泛型clazz
     */
    public static <T> Class<?> findTypeParameterNoCache(@Nonnull T instance,
                                                        @Nonnull Class<? super T> superClazzOrInterface,
                                                        @Nonnull String typeParamName) throws Exception {
        return NettyTypeParameterFinderAdapter.DEFAULT_INSTANCE.findTypeParameter(instance, superClazzOrInterface, typeParamName);
    }

    /**
     * 查找父类/父接口定义的且被子类声明为具体类型的泛型参数的具体类型
     *
     * @param instance              superClazzOrInterface的子类实例
     * @param superClazzOrInterface 泛型参数typeParamName存在的类,class或interface
     * @param typeParamName         泛型参数名字
     * @param <T>                   约束必须有继承关系或实现关系
     * @return 如果定义的泛型存在，则返回对应的泛型clazz
     */
    public static <T> Class<?> findTypeParameter(@Nonnull T instance,
                                                 @Nonnull Class<? super T> superClazzOrInterface,
                                                 @Nonnull String typeParamName) throws Exception {
        final FindCache findCache = LOCAL_FIND_CACHE.get();
        List<FindResult> findResultList = findCache.instanceFindCache.computeIfAbsent(instance.getClass(), k -> new ArrayList<>());
        for (FindResult findResult : findResultList) {
            if (findResult.superClazzOrInterface == superClazzOrInterface && findResult.typeParamName.equals(typeParamName)) {
                return findResult.realType;
            }
        }
        final Class<?> realType = NettyTypeParameterFinderAdapter.DEFAULT_INSTANCE.findTypeParameter(instance, superClazzOrInterface, typeParamName);
        findResultList.add(new FindResult(superClazzOrInterface, typeParamName, realType));
        return realType;
    }

    public static <T> TypeParameterMatcher findTypeMatcher(@Nonnull T instance, Class<? super T> superClazzOrInterface, String typeParamName) throws Exception {
        final Class<?> type = findTypeParameter(instance, superClazzOrInterface, typeParamName);
        return new ReflectiveTypeMatcher(type);
    }

    private static class ReflectiveTypeMatcher extends TypeParameterMatcher {

        private final Class<?> type;

        private ReflectiveTypeMatcher(Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean match(@Nonnull Object object) {
            return type.isInstance(object);
        }
    }

    private static class FindCache {
        /**
         * 每一个实例类开始的查找结果
         */
        private final Map<Class<?>, List<FindResult>> instanceFindCache = new HashMap<>();

    }

    private static class FindResult {

        private final Class<?> superClazzOrInterface;
        private final String typeParamName;
        private final Class<?> realType;

        private FindResult(Class<?> superClazzOrInterface, String typeParamName, Class<?> realType) {
            this.superClazzOrInterface = superClazzOrInterface;
            this.typeParamName = typeParamName;
            this.realType = realType;
        }
    }
}
