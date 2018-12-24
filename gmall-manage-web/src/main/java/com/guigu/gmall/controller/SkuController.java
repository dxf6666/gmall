package com.guigu.gmall.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.guigu.gmall.bean.SkuInfo;
import com.guigu.gmall.service.SkuService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Controller
public class SkuController {
    @Reference
    private SkuService skuService;

    @RequestMapping("skuInfoListBySpu")
    public List<SkuInfo> skuInfoList(String spuId){
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(spuId);
        List<SkuInfo> skuInfoList =  skuService.getSkuInfoList(skuInfo);
        return skuInfoList;
    }

    @RequestMapping("saveSku")
    public String saveSku(SkuInfo skuInfo){
        skuService.saveSku(skuInfo);
        return "success !";
    }
}
