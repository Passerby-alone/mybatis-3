/**
 *    Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;

/**
 * 跟数据库相关的sql语句操作，都会使用这个对象
 */
public interface StatementHandler {

  /**
   * 准备操作，创建 Statement对象
   * */
  Statement prepare(Connection connection, Integer transactionTimeout)
      throws SQLException;

  /**
   * 设置 statement 对象参数
   * */
  void parameterize(Statement statement)
      throws SQLException;

  /**
   * 添加 Statement 对象的批量操作
   * */
  void batch(Statement statement)
      throws SQLException;

  /**
   * 执行更新语句
   * */
  int update(Statement statement)
      throws SQLException;

  /**
   * 执行读操作
   * */
  <E> List<E> query(Statement statement, ResultHandler resultHandler)
      throws SQLException;

  /**
   * 返回游标
   * */
  <E> Cursor<E> queryCursor(Statement statement)
      throws SQLException;

  BoundSql getBoundSql();

  /**
   * ParameterHandler 参数处理接口
   * */
  ParameterHandler getParameterHandler();

}
