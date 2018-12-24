package com.guigu.gmall.service;

import com.guigu.gmall.bean.BaseAttrInfo;
import com.guigu.gmall.bean.SkuLsInfo;
import com.guigu.gmall.bean.SkuLsParam;

import java.util.List;

public interface ListService {
    List<SkuLsInfo> search(SkuLsParam skuLsParam);

    List<BaseAttrInfo> getAttrListByValueId(String join);
}
