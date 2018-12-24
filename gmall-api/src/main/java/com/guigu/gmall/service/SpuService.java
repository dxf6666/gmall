package com.guigu.gmall.service;

import com.guigu.gmall.bean.BaseSaleAttr;
import com.guigu.gmall.bean.SpuImage;
import com.guigu.gmall.bean.SpuInfo;
import com.guigu.gmall.bean.SpuSaleAttr;

import java.util.List;

public interface SpuService {
    List<BaseSaleAttr> getBaseSaleAttrList();


    void saveSpu(SpuInfo spuInfo);

    List<SpuInfo> getSpuList(String catalog3Id);

    List<SpuSaleAttr> spuSaleAttrList(String spuId);

    List<SpuImage> getSpuImgList(String spuId);
}
