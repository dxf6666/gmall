package com.guigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.guigu.gmall.bean.UserInfo;
import com.guigu.gmall.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Controller
public class UserController {
    // 不在使用@Autowired  why:  已经是分布式架构了，调用的不在是本地方法，而是远程方法
    // @Reference是远程注入，阿里巴巴dubbo
    // @Autowired是本地注入，spring家族的
    @Reference
    private UserService userService;  //去调用远程服务zookeeper上注册的接口，  像调用远程方法一样调用本地方法

    @RequestMapping("userList")
    public List<UserInfo> showUserList(){
        return userService.getUserList();
    }


}
