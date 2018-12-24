package com.guigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.guigu.gmall.bean.SkuSaleAttrValue;
import com.guigu.gmall.bean.SpuSaleAttr;
import com.guigu.gmall.bean.SkuInfo;
import com.guigu.gmall.service.SkuSaleAttrService;
import com.guigu.gmall.service.SkuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {
    @Reference
    private SkuSaleAttrService skuSaleAttrService;
    @Reference
    private SkuService skuService;


    @RequestMapping("{skuId}.html")
    public String index(@PathVariable String skuId, ModelMap map){  //@PathVariable 从路径上获取参数
       // 1、查出该商品的spu的所有销售属性和属性值
       // 2、标识出本商品对应的销售属性
       // 3、点击其他销售属性值的组合，跳转到另外的sku页面



        // 查询出sku信息  -- 具体商品
        SkuInfo skuInfo = skuService.getSkuById(skuId);
        if(skuInfo != null){
            map.put("skuInfo",skuInfo); //根据skuId查询对象，保存到request域中，转发给item.html

            String spuId = skuInfo.getSpuId();
            //查询销售属性列表
            List<SpuSaleAttr> spuSaleAttrs =  skuSaleAttrService.getSpuSaleAttrListCheckBySku(spuId,skuInfo.getId());
            map.put("spuSaleAttrListCheckBySku",spuSaleAttrs);

            //查询sku兄弟姐妹的hash表，效果：点击颜色、内存等销售属性，有就切换商品  【难点：对不同销售属性进行切换】

            //通过spuID查询出该spu下的所有sku详细信息
            List<SkuInfo> skuInfos = skuSaleAttrService.getSkuSaleAttrValueListBySpu(spuId);

            // {"176|180":"95","176|180":"96",...}
            // key:销售属性值id   value：sku_id  通过sku销售属性值找到具体的sku
            Map<String,String> skuMap = new HashMap<>();//作用：
            for (SkuInfo info : skuInfos) {
                String v = info.getId(); //获取sku的id
                //获取sku销售属性值
                List<SkuSaleAttrValue> skuSaleAttrValueList = info.getSkuSaleAttrValueList();
                String k = "";
                for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                    // 获取销售属性值id
                    String saleAttrValueId = skuSaleAttrValue.getSaleAttrValueId();
                    k = k +"|"+saleAttrValueId;
                }
                skuMap.put(k,v);
            }
            // 用阿里的json工具将hashmap转化成json字符串
            String skuMapJson = JSON.toJSONString(skuMap);
            map.put("skuMapJson",skuMapJson);
        }
        return "item";
    }
}