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
package org.apache.ibatis.cache;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.fastjson.JSON;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.junit.jupiter.api.Test;

class FifoCacheTest {

  @Test
  void shouldRemoveFirstItemInBeyondFiveEntries() {
    // 基于先入先出的缓存
    FifoCache cache = new FifoCache(new PerpetualCache("default"));
    cache.setSize(5);
    for (int i = 0; i < 5; i++) {
      cache.putObject(i, i);
    }
    System.out.println(cache.getObject(0));
    System.out.println(JSON.toJSONString(cache));
    // 会抛弃最先进入队列的那个数据
    cache.putObject(5, 5);
    System.out.println(JSON.toJSONString(cache));
  }

  @Test
  void shouldRemoveItemOnDemand() {
    FifoCache cache = new FifoCache(new PerpetualCache("default"));
    cache.putObject(0, 0);
    assertNotNull(cache.getObject(0));
    cache.removeObject(0);
    assertNull(cache.getObject(0));
  }

  @Test
  void shouldFlushAllItemsOnDemand() {
    FifoCache cache = new FifoCache(new PerpetualCache("default"));
    for (int i = 0; i < 5; i++) {
      cache.putObject(i, i);
    }
    assertNotNull(cache.getObject(0));
    assertNotNull(cache.getObject(4));
    cache.clear();
    assertNull(cache.getObject(0));
    assertNull(cache.getObject(4));
  }

}
