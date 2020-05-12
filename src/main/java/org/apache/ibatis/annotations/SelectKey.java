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

import org.apache.ibatis.mapping.StatementType;

/**
 * The annotation that specify an SQL for retrieving a key value.
 *
 * <p>
 * <b>How to use:</b>
 *
 * <pre>
 * public interface UserMapper {
 *   &#064;SelectKey(statement = "SELECT identity('users')", keyProperty = "id", before = true, resultType = int.class)
 *   &#064;Insert("INSERT INTO users (id, name) VALUES(#{id}, #{name})")
 *   boolean insert(User user);
 * }
 * </pre>
 *
 * @author Clinton Begin
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
/**
 * 通过sql语句获得主键的注解
 * */
public @interface SelectKey {
  /**
   * 返回的sql语句
   */
  String[] statement();

  /**
   * 主键 java对象的属性
   */
  String keyProperty();

  /**
   * 主键对应的列
   */
  String keyColumn() default "";

  /**
   * 在sql执行前，还是在sql执行后
   */
  boolean before();

  /**
   * 返回类型
   */
  Class<?> resultType();

  /**
   * 语句类型
   */
  StatementType statementType() default StatementType.PREPARED;
}
