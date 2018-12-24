package com.guigu.gmall.item.mapper;


import com.guigu.gmall.bean.SkuSaleAttrValue;
import com.guigu.gmall.bean.SkuInfo;
import com.guigu.gmall.bean.SpuSaleAttr;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;
public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(@Param("spuId") Integer spuId, @Param("skuId") Integer skuId);

    List<SkuInfo> selectSkuSaleAttrValueListBySpu(@Param("spuId") int spuId);
}
