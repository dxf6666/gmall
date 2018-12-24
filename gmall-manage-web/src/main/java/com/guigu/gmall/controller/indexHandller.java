package com.guigu.gmall.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class indexHandller {
    @RequestMapping("index")
    public String index(){
        return "index";
        /*亲儿子就是亲儿子，自带后缀html*/
    }

    /*平台属性管理页面*/
    @RequestMapping("attrListPage")
    public String attrListPage(){
        return "attrListPage";
    }

    /*商品SPU管理页面*/
    @RequestMapping("spuListPage")
    public String spuListPage(){
        return "spuListPage";
    }
}

