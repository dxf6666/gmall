package com.guigu.gmall.item.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.guigu.gmall.bean.SkuInfo;
import com.guigu.gmall.bean.SpuSaleAttr;
import com.guigu.gmall.item.mapper.SkuSaleAttrValueMapper;
import com.guigu.gmall.service.SkuSaleAttrService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class SkuSaleAttrServiceImpl implements SkuSaleAttrService {
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String spuId, String skuId) {
        List<SpuSaleAttr> spuSaleAttrs = skuSaleAttrValueMapper.selectSpuSaleAttrListCheckBySku(Integer.parseInt(spuId),Integer.parseInt(skuId));
        return  spuSaleAttrs;
    }

    @Override
    public List<SkuInfo> getSkuSaleAttrValueListBySpu(String spuId) {

        List<SkuInfo> skuInfos = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(Integer.parseInt(spuId));

        return skuInfos;
    }
}
