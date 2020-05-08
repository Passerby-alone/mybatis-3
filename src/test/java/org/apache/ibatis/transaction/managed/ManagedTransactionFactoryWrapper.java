package org.apache.ibatis.transaction.managed;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * @author jinguo_peng
 * @description TODO
 * @date 2020/5/7 下午2:44
 */
public class ManagedTransactionFactoryWrapper extends ManagedTransactionFactory {

  private boolean closeConnection = true;

  @Override
  public Transaction newTransaction(Connection conn) {
    // 创建容器事务
    return new ManagedTransactionWrapper(conn, closeConnection);
  }

  @Override
  public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
    // Silently ignores autocommit and isolation level, as managed transactions are entirely
    // controlled by an external manager.  It's silently ignored so that
    // code remains portable between managed and unmanaged configurations.
    return new ManagedTransaction(ds, level, closeConnection);
  }
}
