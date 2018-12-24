package com.guigu.gmall.cart.mapper;

import com.guigu.gmall.bean.CartInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

public interface CartInfoMapper extends Mapper<CartInfo> {
    void deleteCartsById(@Param("join") String join);
}
