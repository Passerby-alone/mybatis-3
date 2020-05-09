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
package org.apache.ibatis.type;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation that specify jdbc types to map {@link TypeHandler}.
 *
 * <p>
 * <b>How to use:</b>
 * <pre>
 * &#064;MappedJdbcTypes({JdbcType.CHAR, JdbcType.VARCHAR})
 * public class StringTrimmingTypeHandler implements TypeHandler&lt;String&gt; {
 *   // ...
 * }
 * </pre>
 * @author Eduardo Macarron
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// 放到类上
@Target(ElementType.TYPE)
public @interface MappedJdbcTypes {
  /**
   * 匹配的JDBC Type类型的注解
   */
  JdbcType[] value();

  /**
   * 包含空的JDBC Type类型，没有匹配到JDBC Type类型
   */
  boolean includeNullJdbcType() default false;
}
