package com.guigu.gmall.passport.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.guigu.gmall.bean.CartInfo;
import com.guigu.gmall.bean.UserInfo;
import com.guigu.gmall.service.CartService;
import com.guigu.gmall.service.UserService;
import com.guigu.gmall.utils.CookieUtil;
import com.guigu.gmall.utils.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;

    // 跳入登陆页面
    @RequestMapping("index")
    public String index(String returnUrl, ModelMap map){
        map.put("originUrl",returnUrl);
        return "index";
    }

    // 用户登录
    @ResponseBody
    @RequestMapping("login")
    public String login(UserInfo userInfo, HttpServletRequest request, HttpServletResponse response, ModelMap map){
        // 验证用户名和密码
        UserInfo user = userService.login(userInfo);
        if(null==user){
            // 提示用户用户名或者密码错误
            return "err";
        }else{
            // 如果验证成功，根据用户名和密码生成token，然后将该用户的用户信息从db中提取到redis，设置该用户的过期时间
            Map<String,String> userMap = new HashMap<>();
            userMap.put("userId",user.getId());
            userMap.put("nickName",user.getNickName());
            String ip = getMyIpFromRequest(request);
            String token = JwtUtil.encode("gmallguigu", userMap, ip);

            // 合并购物车
            String listCartCookie = CookieUtil.getCookieValue(request, "listCartCookie", true);
            // 通过消息服务，发送异步的消息通知，并行处理购物车合并业务
            cartService.combine(user.getId(), JSON.parseArray(listCartCookie, CartInfo.class));
            // 删除cookie中的购物车
            CookieUtil.deleteCookie(request,response,"listCartCookie");

            return token;
        }
    }

    /***
     * 验证用户token
     * @param request
     * @param map
     * @return
     */
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request,String token,String currentIp, ModelMap map){
        // 验证token的真伪
        try{
            Map gmallguigu = JwtUtil.decode("gmallguigu", token, currentIp);
            if(null!=gmallguigu){
                // 验证token对应的用户信息的过期时间

                return "success";
            }else{
                return "fail";
            }
        }catch (Exception e){
            return "fail";
        }
    }

    /***
     * 获得客户端ip
     * @param request
     * @return
     */
    private String getMyIpFromRequest(HttpServletRequest request) {
        String ip = "";
        ip = request.getRemoteAddr();
        if(StringUtils.isBlank(ip)){
            ip = request.getHeader("x-forwarder-for");
            if(StringUtils.isBlank(ip)){
                ip = "127.0.0.1";
            }
        }
        return ip;
    }

}
