package com.guigu.gmall.list.mapper;

import com.guigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AttrMapper {
    List<BaseAttrInfo> selectAttrListByValueId(@Param("join") String join);
}
