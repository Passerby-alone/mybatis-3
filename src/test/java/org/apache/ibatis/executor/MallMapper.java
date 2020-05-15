package org.apache.ibatis.executor;

import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.cache.decorators.LruCache;

import java.util.List;

/**
 * @author jinguo_peng
 * @description TODO
 * @date 2020/5/14 下午3:38
 */
@CacheNamespace
public interface MallMapper {

    @Select({"select * from mall where is_binding = 1 limit 100"})
    List<Object> selectMall();

    @Update({"update mall set mall_id = 1 where mall_id = 5618"})
    int update();
}
