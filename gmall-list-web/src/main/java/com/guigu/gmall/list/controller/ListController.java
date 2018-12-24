package com.guigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.guigu.gmall.bean.*;
import com.guigu.gmall.service.ListService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {
    @Reference
    private ListService listService;

    @RequestMapping("list.html")
    public String search(SkuLsParam skuLsParam, ModelMap model){   //SkuLsParam存储各种搜索条件字段
        List<SkuLsInfo> skuLsInfoList = listService.search(skuLsParam); //SkuLsInfo封住了平台属性值
        model.put("skuLsInfoList",skuLsInfoList);
        model.put("keyword", skuLsParam.getKeyword()); //==
        //去重复取出所有平台属性值
        HashSet<String> strings = new HashSet<>();//set唯一不可重复
        for (SkuLsInfo skuLsInfo : skuLsInfoList) {
            List<SkuLsAttrValue> skuAttrValueList = skuLsInfo.getSkuAttrValueList();
            for (SkuLsAttrValue skuLsAttrValue : skuAttrValueList) {
                String valueId = skuLsAttrValue.getValueId(); //平台属性值id
                strings.add(valueId);
            }
        }
        // 通过平台属性值获取平台属性
        String join =  StringUtils.join(strings, ",");  //  1,2,3,4,5,6.......
        List<BaseAttrInfo> baseAttrInfos = listService.getAttrListByValueId(join);

        // 获取页面上选中的平台属性    -- 在所有的属性值中，用户点击一个，那么就让它在该集合中删掉
        String[] valueId = skuLsParam.getValueId();
        // 声明面包屑
        List<Crumb> crumbs = new ArrayList<>();
        String valueName = null;  // 选中的属性名
        if (valueId != null && valueId.length>0) {
            for (String selectedAttrValueId : valueId) {
                // 循环获取所有的属性值
                Iterator<BaseAttrInfo> iterator = baseAttrInfos.iterator();
                while (iterator.hasNext()) {
                    BaseAttrInfo baseAttrInfo = iterator.next();
                    List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                    // 循环属性值集合
                    for (BaseAttrValue baseAttrValue : attrValueList) {
                        String id = baseAttrValue.getId();// 获取属性值id
                        // 如果选中的属性值id 和  所有属性值id 一样，那么就删除
                        if (selectedAttrValueId.equals(id)) {
                            // 获取属性值 == 面包屑名称
                            valueName = baseAttrValue.getValueName();
                            iterator.remove();// 删除属性和属性值
                        }
                    }
                }
                // 面包屑的作用： 当用户点击完属性值，就将该属性值删除，并把属性值存放在面包屑中，到时候删除面包屑，那么该属性就恢复显示   -- 选中几个属性值，就有几个面包屑
                // 当前请求 -->点击-->平台属性 = (添加)面包屑
                // (删除)面包屑 = 当前请求 + 平台属性
                // 制作面包屑   -- 选中了多少个属性值，就有多少个面包屑
                Crumb crumb = new Crumb();
                //面包屑地址
                String crumbUrlParam = getUrlParam(skuLsParam,selectedAttrValueId);
                crumb.setUrlParam(crumbUrlParam);

                crumb.setValueName(valueName); //属性名
                crumbs.add(crumb);
            }
        }
        model.put("attrList",baseAttrInfos);

        String urlParam = getUrlParam(skuLsParam,null);
        model.put("urlParam",urlParam);
        // 面包屑
        model.put("attrValueSelectedList",crumbs);
        return "list";
    }

    /*将搜索条件进行拼接     http://localhost:8080/list.html?  [urlParam]   */
    public String getUrlParam(SkuLsParam skuLsParam, String selectedAttrValueId) {
        String catalog3Id = skuLsParam.getCatalog3Id();
        String keyword = skuLsParam.getKeyword();
        String[] valueId = skuLsParam.getValueId();

        String urlParam = null;
        //http:localhost:8080/index? K=v & K=v & K=v ...   如果urlParam为null，就不需要加 &
        if (StringUtils.isNotBlank(catalog3Id)){
            if (skuLsParam!=null){  // 不为null
                urlParam = urlParam + "&";
            }
            urlParam = "catalog3Id="+catalog3Id;
        }
        if (StringUtils.isNotBlank(keyword)){
            if (skuLsParam!=null){  // 不为null
                urlParam = urlParam + "&";
            }
            urlParam = "keyword="+keyword;
        }

        if ( valueId != null && valueId.length > 0){
            for (String id : valueId) {
                if(selectedAttrValueId == null){
                    urlParam = "valueId="+id;   // 属性值地址
                } else {   // 面包屑地址
                    if(!id.equals(selectedAttrValueId)){
                        urlParam = urlParam +"&" + "valueId="+id;
                    }
                }
            }
        }
        return urlParam;
    }
}
