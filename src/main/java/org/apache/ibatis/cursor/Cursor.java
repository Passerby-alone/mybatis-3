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
package org.apache.ibatis.cursor;

import java.io.Closeable;

/**
 * 游标接口
 */
public interface Cursor<T> extends Closeable, Iterable<T> {

  /**
   * 是否处于打开状态
   */
  boolean isOpen();

  /**
   * 是否全部消费
   */
  boolean isConsumed();

  /**
   * 获得当前索引
   */
  int getCurrentIndex();
}
