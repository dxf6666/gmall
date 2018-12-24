package com.guigu.gmall.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.guigu.gmall.bean.*;
import com.guigu.gmall.mapper.*;
import com.guigu.gmall.mapper.*;
import com.guigu.gmall.service.AttrManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class AttrManageServiceImpl implements AttrManageService {

    @Autowired  //本地注入
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired  //本地注入
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired  //本地注入
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired  //本地注入
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired  //本地注入
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private SaveAttrMapper saveAttrMapper;
    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        //创建 BaseCatalog2 bean对象
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        // 在二级表中有个一级表字段id引用
        baseCatalog2.setCatalog1Id(catalog1Id);
        // 将要查询的对象出入，它会根据对象的属性去查询具有相同属性的值
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrListByCtg3(String catalog3Id) {
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        List<BaseAttrInfo> attrList = baseAttrInfoMapper.select(baseAttrInfo);

        //根据平台属性id查询平台属性值
        if (attrList !=null && attrList.size()>0){
            for (BaseAttrInfo attrInfo : attrList) {
                //获取平台属性id
                String id = attrInfo.getId();
                //平台属性值attr_id 引用 平台属性id,使用 通用mybatis 查询是存入对象，而不是字段条件
                BaseAttrValue arrtValue = new BaseAttrValue();
                arrtValue.setAttrId(id);
                List<BaseAttrValue> attrValueList = baseAttrValueMapper.select(arrtValue);

                attrInfo.setAttrValueList(attrValueList);
            }
        }
        return attrList;
    }

    @Override
    public String saveAttr(BaseAttrInfo baseAttrInfo) {
        String id = baseAttrInfo.getId();

        if(StringUtils.isBlank(id)){//id为空时是保存
            // 保存属性信息
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
            String attrId = baseAttrInfo.getId(); //返回属性表的编号，给属性值表的attr_id引用  --主键返回策略
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(attrId);
                baseAttrValueMapper.insert(baseAttrValue);
            }
        }else{ //id不为空时是更新
            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);

            // 删除属性值
            BaseAttrValue baseAttrValue = new BaseAttrValue();
            baseAttrValue.setAttrId(id);
            baseAttrValueMapper.delete(baseAttrValue);

            // 添加新的属性值
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

            for (BaseAttrValue attrValue : attrValueList) {
                attrValue.setAttrId(id);
                baseAttrValueMapper.insert(attrValue);
            }
        }
        return "success";
    }
   /*通过属性id查询属性值列表*/
    @Override
    public List<BaseAttrValue> selectAttrValueById(String id) {
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(id);
        return baseAttrValueMapper.select(baseAttrValue);
    }

    //根据属性id删除属性和属性列表
    @Override
    public void delAttrById(String id) {
        baseAttrInfoMapper.deleteByPrimaryKey(id);
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(id);
        baseAttrValueMapper.delete(baseAttrValue);
    }
}
