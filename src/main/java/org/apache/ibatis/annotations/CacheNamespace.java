/**
 *    Copyright 2009-2020 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;

/**
 * The annotation that specify to use cache on namespace(e.g. mapper interface).
 *
 * <p>
 * <b>How to use:</b>
 *
 * <pre>
 * &#064;acheNamespace(implementation = CustomCache.class, properties = {
 *   &#064;Property(name = "host", value = "${mybatis.cache.host}"),
 *   &#064;Property(name = "port", value = "${mybatis.cache.port}"),
 *   &#064;Property(name = "name", value = "usersCache")
 * })
 * public interface UserMapper {
 *   // ...
 * }
 * </pre>
 *
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE) // 放在mapper类上
public @interface CacheNamespace {

  /**
   * 负责存储缓存的cache实现类 先入先出缓存 最少使用缓存 最近未使用缓存
   */
  Class<? extends Cache> implementation() default PerpetualCache.class;

  /**
   * 负责存储过期的cache实现类
   */
  Class<? extends Cache> eviction() default LruCache.class;

  /**
   * 清空的时间间隔 0:表示不会清空
   */
  long flushInterval() default 0;

  /**
   * 缓存大小
   */
  int size() default 1024;

  /**
   * 是否序列化
   */
  boolean readWrite() default true;

  /**
   * 是否阻塞
   */
  boolean blocking() default false;

  /**
   * Returns property values for a implementation object.
   *
   * @return property values
   * @since 3.4.2
   */
  Property[] properties() default {};

}
