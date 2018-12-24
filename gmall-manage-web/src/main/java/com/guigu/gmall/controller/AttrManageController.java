package com.guigu.gmall.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.guigu.gmall.bean.*;
import com.guigu.gmall.service.AttrManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;
import java.util.Map;

/*================= 属性管理controller ==============================*/
@Controller
public class AttrManageController {
    @Reference   //远程注入  @Autowired本地注入
    private AttrManageService attrManageService;


    /* 一级查询  */
    @RequestMapping("getCatalog1")
    @ResponseBody  //将方法的返回值转成json对象，显示给浏览器
    public  List<BaseCatalog1> getCatalog1(){
       List<BaseCatalog1> baseCatalog1List = attrManageService.getCatalog1();
        return baseCatalog1List;
    }
    /* 二级查询  */
    @RequestMapping("getCatalog2")
    @ResponseBody  //将方法的返回值转成json对象，显示给浏览器
    public  List<BaseCatalog2> getCatalog2(@RequestParam Map<String,String> map) {  //@RequestParam获取请求参数，并将请求参数转成map集合
        // 获取一级分类的id
       String catalog1Id =  map.get("catalog1Id");
        // 根据一级分类的id去查询二级分类值
        List<BaseCatalog2> baseCatalog2List = attrManageService.getCatalog2(catalog1Id);
        return baseCatalog2List ;
    }

    /* 三级查询  */
    @RequestMapping("getCatalog3")
    @ResponseBody  //将方法的返回值转成json对象，显示给浏览器
    public  List<BaseCatalog3> getCatalog3(@RequestParam Map<String,String> map){
        // 获取二级分类的id
        String catalog2Id =  map.get("catalog2Id");
        // 根据一级分类的id去查询二级分类值
        List<BaseCatalog3> baseCatalog3List = attrManageService.getCatalog3(catalog2Id);
        return baseCatalog3List ;
    }

    /*根据三级id查询属性列表*/
    @RequestMapping("getAttrListByCtg3")
    @ResponseBody
    public List<BaseAttrInfo> getAttrListByCtg3(@RequestParam Map<String,String> map){
        // 获取三级分类的id
        String catalog3Id = map.get("catalog3Id");
        List<BaseAttrInfo> attrList = attrManageService.getAttrListByCtg3(catalog3Id);

        return attrList;
    }

    /*添加属性和属性值*/
    @RequestMapping("saveAttr")
    @ResponseBody
    public String saveAttr(BaseAttrInfo baseAttrInfo){
        String result = attrManageService.saveAttr(baseAttrInfo);
        return result;
    }

    /*属性id查询属性列表*/
    @RequestMapping("selectAttrValueById")
    @ResponseBody
    public List<BaseAttrValue> selectAttrValueById(String id){
        return attrManageService.selectAttrValueById(id);
    }

    /*删除*/
    @RequestMapping("delAttrById")
    @ResponseBody
    public String delAttrById(String id){
        //删除指定属性
        attrManageService.delAttrById(id);
        return "success ！！";
    }
}
