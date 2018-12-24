package com.guigu.gmall.gware.mapper;

import com.guigu.gmall.gware.bean.WareInfo;
import tk.mybatis.mapper.common.Mapper;
import java.util.List;

public interface WareInfoMapper extends Mapper<WareInfo> {
    public List<WareInfo> selectWareInfoBySkuid(String skuid);
}
