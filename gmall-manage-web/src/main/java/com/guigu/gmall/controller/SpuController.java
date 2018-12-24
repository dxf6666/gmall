package com.guigu.gmall.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.guigu.gmall.bean.BaseSaleAttr;
import com.guigu.gmall.bean.SpuImage;
import com.guigu.gmall.bean.SpuInfo;
import com.guigu.gmall.bean.SpuSaleAttr;
import com.guigu.gmall.utils.FileUploadUtil;
import com.guigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller  // @RequestBody + @ResponseBody(将方法的返回值转成json对象响应给浏览器 )
public class SpuController {
    // 查询销售属性列表
    @Reference  //远程注入，使用的是dubbo分布式架构技术，所以远程注入调用的是com.alibaba.dubbo，表示使用dubbo远程注入
    private SpuService spuService;

    @RequestMapping("baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList(){
        return spuService.getBaseSaleAttrList();
    }


    @RequestMapping("saveSpu")
    public String saveSpu(SpuInfo spuInfo){
        spuService.saveSpu(spuInfo);
        return "success";
    }

    @RequestMapping("fileUpload")
    public String fileUpload(@RequestParam("file") MultipartFile file){
        // 调用fdfs的上传工具
        String imgUrl = FileUploadUtil.uploadImage(file);
        return imgUrl;
    }

    @RequestMapping("spuList")
    public List<SpuInfo> spuList(String catalog3Id){
        List<SpuInfo> SpuInfoList = spuService.getSpuList(catalog3Id);
        return  SpuInfoList;
    }

    @RequestMapping("spuSaleAttrList")
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){
        List<SpuSaleAttr> spuSaleAttrs =  spuService.spuSaleAttrList(spuId);
        return spuSaleAttrs;
    }

    @RequestMapping("spuImageList")
    public List<SpuImage> spuImageList(String spuId){
        return spuService.getSpuImgList(spuId);
    }
}
