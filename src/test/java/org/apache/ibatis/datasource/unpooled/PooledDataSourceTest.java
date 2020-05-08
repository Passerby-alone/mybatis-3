/**
 *    Copyright 2009-2019 the original author or authors.
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
package org.apache.ibatis.datasource.unpooled;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.base.Stopwatch;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.*;

class PooledDataSourceTest {

  @Test
  public void testPool() throws Exception {

    // 1000个线程5个连接池，执行一条sql语句，平均消耗时间：[11]毫秒
//    runnable(1000, 5);

    // 1000个线程100个连接池，执行一条sql语句，平均消耗时间：[18]毫秒
//    runnable(1000, 100);

    // 1000个线程50个连接池，执行一条sql语句，平均消耗时间：[10]毫秒
//    runnable(1000, 50);

    //1000个线程10个连接池，执行一条sql语句，平均消耗时间：[8]毫秒
//    runnable(1000, 10);
    // 1000个线程7个连接池，执行一条sql语句，平均消耗时间：[9]毫秒
//    runnable(1000, 7);

    // 1000个线程6个连接池，执行一条sql语句，平均消耗时间：[9]毫秒
    runnable(1000, 100);
  }

  private void runnable(int threadPoolCount, int connectionCount) throws Exception {

    CountDownLatch countDownLatch = new CountDownLatch(threadPoolCount);
    ExecutorService executors = Executors.newFixedThreadPool(threadPoolCount);

//    PooledDataSource dataSource = null;
//    dataSource = new PooledDataSource("com.mysql.cj.jdbc.Driver", "jdbc:mysql://localhost:3306/yiyong-zhanggui?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&useSSL=false&allowMultiQueries=true&rewriteBatchedStatements=true&serverTimezone=GMT%2B8", "root", "888888");
//    dataSource.setPoolMaximumActiveConnections(connectionCount);
//    dataSource.setPoolMaximumIdleConnections(connectionCount);

    Queue<Connection> queue = new ArrayBlockingQueue(connectionCount);
    for (int count = 0; count < connectionCount; count ++) {
      UnpooledDataSource dataSource = new UnpooledDataSource();
      dataSource.setUrl("jdbc:mysql://localhost:3306/yiyong-zhanggui?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&zeroDateTimeBehavior=convertToNull&useSSL=false&allowMultiQueries=true&rewriteBatchedStatements=true&serverTimezone=GMT%2B8");
      dataSource.setDriver("com.mysql.cj.jdbc.Driver");
      Properties properties = new Properties();
      properties.put("user", "root");
      properties.put("password", "888888");

      Connection connection = dataSource.doGetConnection(properties);
      queue.add(connection);
    }

    Stopwatch stopwatch = Stopwatch.createStarted();
    for (int count = 0; count < threadPoolCount; count ++) {

      while (queue.size() == 0) {
      }
      Connection connection = queue.poll();
      TaskRunnable runnable = new TaskRunnable(connection, countDownLatch, queue);
      executors.submit(runnable);
    }
    countDownLatch.await();
    System.out.println(threadPoolCount + "个线程" + connectionCount + "个连接池，执行一条sql语句，平均消耗时间：[" + stopwatch.elapsed(TimeUnit.MILLISECONDS) / threadPoolCount + "]毫秒");
  }

  class TaskRunnable implements Callable<Object> {

    private Connection connection;
    private CountDownLatch countDownLatch;
    private Queue queue;

    public TaskRunnable(Connection connection, CountDownLatch countDownLatch, Queue queue) {
      this.connection = connection;
      this.countDownLatch = countDownLatch;
      this.queue = queue;
    }

    @Override
    public Object call() throws Exception {

      Statement statement = connection.createStatement();
      statement.execute("select * from mall limit 1000");
      countDownLatch.countDown();
      queue.add(connection);
      return true;
    }
  }
}
