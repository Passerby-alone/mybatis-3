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
package org.apache.ibatis.scripting;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;

/**
 * 语言驱动接口
 * */
public interface LanguageDriver {

  /**
   * 创建ParameterHandler对象
   */
  ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

  /**
   * 创建SqlSource 对象 解析 Mapper XML配置的 <select /> <insert />标签
   */
  SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);

  /**
   * 从注解中解析出来SqlSource对象 即@Select @Update标签等
   */
  SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);

}
