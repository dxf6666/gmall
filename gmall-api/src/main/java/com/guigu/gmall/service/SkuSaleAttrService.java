package com.guigu.gmall.service;

import com.guigu.gmall.bean.SkuInfo;
import com.guigu.gmall.bean.SpuSaleAttr;

import java.util.List;

public interface SkuSaleAttrService {
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String spuId, String skuId);

    List<SkuInfo> getSkuSaleAttrValueListBySpu(String spuId);
}
