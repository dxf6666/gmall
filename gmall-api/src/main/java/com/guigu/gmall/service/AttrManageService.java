package com.guigu.gmall.service;

import com.guigu.gmall.bean.*;

import java.util.List;

public interface AttrManageService {
    //获取一级分类
    List<BaseCatalog1> getCatalog1();
    //根据一级分类id,获取二级分类
    List<BaseCatalog2> getCatalog2(String catalog1Id);
    //根据二级分类id,获取三级分类
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    //根据三级分类id,获取属性列表
    List<BaseAttrInfo> getAttrListByCtg3(String catalog3Id);

    //保存属性及属性值
    String saveAttr(BaseAttrInfo baseAttrInfo);

    List<BaseAttrValue> selectAttrValueById(String id);

    void delAttrById(String id);
}
