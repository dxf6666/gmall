package com.guigu.gmall.service;

import com.guigu.gmall.bean.SkuInfo;

import java.util.List;

public interface SkuService {
    List<SkuInfo> getSkuInfoList(SkuInfo skuInfo);

    void saveSku(SkuInfo skuInfo);

    SkuInfo getSkuById(String skuId);

    List<SkuInfo> getSkuByCatalog3Id(int catalog3Id);

    List<SkuInfo> getMySkuInfo(String catalog3Id);
}
