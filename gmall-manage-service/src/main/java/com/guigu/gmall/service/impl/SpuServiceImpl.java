package com.guigu.gmall.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.guigu.gmall.bean.*;
import com.guigu.gmall.mapper.*;
import com.guigu.gmall.mapper.*;
import com.guigu.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service  //spring 的 @Service 自动注册到Spring容器，之后可以使用spring【本地注入】的@Autowired注解，将对象注入
//这里导入的是com.alibaba.dubbo的@service注解，之后可以使用dubbo【远程注入】的@Reference注解，将对象注入
public class SpuServiceImpl implements SpuService {
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuInfoMapper spuInfoMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    //查询 销售属性列表
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    //保存spu相关信息
    @Override
    public void saveSpu(SpuInfo spuInfo) {
        //保存到 spu_info表
        spuInfoMapper.insertSelective(spuInfo); //insertSelective()插入数据，空的数据不插入并且返回数据主键给spuInfo--只有bean对象设置了主键返回策略才可以
        String spuInfoId = spuInfo.getId();//插入后返回主键

        //保存销售属性信息
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
            spuSaleAttr.setSpuId(spuInfoId);
            spuSaleAttrMapper.insertSelective(spuSaleAttr);

            //保存销售属性值
            List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
            for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                spuSaleAttrValue.setSpuId(spuInfoId);
                spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);

            }
        }
        // 保存图片信息 spu_image
        List<SpuImage> spuImages = spuInfo.getSpuImageList();
        for (SpuImage spuImage : spuImages) {
            spuImage.setSpuId(spuInfoId);
            spuImageMapper.insert(spuImage);
        }

    }

    @Override
    public List<SpuInfo> getSpuList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<SpuSaleAttr> spuSaleAttrList(String spuId) {
        //------  获取销售属性和属性值列表    ---------

        //创建销售属性    -- 销售属性里包含销售属性值列表的字段
        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuId);
        //根据spu的id 查询销售属性
        List<SpuSaleAttr> select = spuSaleAttrMapper.select(spuSaleAttr);

        //循环销售属性列表，查询出每个销售属性所对应的销售属性值列表
        if(select != null && select.size()>0){
            for (SpuSaleAttr saleAttr : select) {
                //创建销售属性值
                SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();
                spuSaleAttrValue.setSpuId(spuId);
                spuSaleAttrValue.setSaleAttrId(saleAttr.getSaleAttrId());
                //根据spuid和saleAttrId组成复合主键，查询出spu的Xxx属性的属性值列表
                List<SpuSaleAttrValue> select1 = spuSaleAttrValueMapper.select(spuSaleAttrValue);

                saleAttr.setSpuSaleAttrValueList(select1);
            }
        }
        return select;
    }

    @Override
    public List<SpuImage> getSpuImgList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }
}
