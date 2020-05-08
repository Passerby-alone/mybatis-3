package org.apache.ibatis.transaction.managed;

import org.apache.ibatis.session.TransactionIsolationLevel;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author jinguo_peng
 * @description TODO
 * @date 2020/5/7 下午2:40
 */
public class ManagedTransactionWrapper extends ManagedTransaction {

  public ManagedTransactionWrapper(Connection connection, boolean closeConnection) {
    super(connection, closeConnection);
  }

  public ManagedTransactionWrapper(DataSource ds, TransactionIsolationLevel level, boolean closeConnection) {
    super(ds, level, closeConnection);
  }

  @Override
  public void commit() throws SQLException {
    System.out.println("自定义实现managedTransaction的commit方法");
  }

  @Override
  public void rollback() throws SQLException {
    System.out.println("自定义实现managedTransaction的rollback方法");
  }
}
