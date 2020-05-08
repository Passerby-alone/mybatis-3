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
package org.apache.ibatis.transaction.managed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.sql.Connection;
import java.util.Properties;

import org.apache.ibatis.BaseDataTest;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.TransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagedTransactionFactoryTest extends BaseDataTest {

  @Mock
  private Connection conn;

  @Test
  void shouldEnsureThatCallsToManagedTransactionAPIDoNotForwardToManagedConnections() throws Exception {

    UnpooledDataSource dataSource = new UnpooledDataSource();
    dataSource.setUrl("jdbc:mysql://localhost:3306/yiyong-zhanggui?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&useSSL=false&allowMultiQueries=true&rewriteBatchedStatements=true&serverTimezone=GMT%2B8");
    dataSource.setDriver("com.mysql.cj.jdbc.Driver");
    Properties properties = new Properties();
    properties.put("user", "root");
    properties.put("password", "888888");

    Connection connection = dataSource.doGetConnection(properties);

    TransactionFactory tf = new ManagedTransactionFactoryWrapper();
    tf.setProperties(new Properties());
    Transaction tx = tf.newTransaction(connection);
    tx.commit();
    tx.rollback();
    tx.close();
  }

  @Test
  void shouldEnsureThatCallsToManagedTransactionAPIDoNotForwardToManagedConnectionsAndDoesNotCloseConnection()
      throws Exception {
    TransactionFactory tf = new ManagedTransactionFactory();
    Properties props = new Properties();
    props.setProperty("closeConnection", "false");
    tf.setProperties(props);
    Transaction tx = tf.newTransaction(conn);
    assertEquals(conn, tx.getConnection());
    tx.commit();
    tx.rollback();
    tx.close();
    verifyNoMoreInteractions(conn);
  }

}
