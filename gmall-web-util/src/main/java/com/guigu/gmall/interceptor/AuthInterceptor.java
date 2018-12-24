package com.guigu.gmall.interceptor;

import com.guigu.gmall.annotation.LoginRequire;
import com.guigu.gmall.utils.CookieUtil;
import com.guigu.gmall.utils.HttpClientUtil;
import com.guigu.gmall.utils.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component  //bean对象，实现个性化配置
public class AuthInterceptor extends HandlerInterceptorAdapter {   //继承拦截器

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);

        // 不需要验证是否登陆
        if (null == methodAnnotation) {
            return true;
        }


        boolean b = methodAnnotation.needSuccess();// 获取运行时注解的名称，boolean
        String token = "";
        String oldToken = CookieUtil.getCookieValue(request,"oldToken",true);// 从用户cookie中获得token,说明用户登陆过一次
        String newToken = request.getParameter("token");// 从浏览器地址栏中获取token，说明该用户第一次登陆

        // oldToken空，newToken空，从未登陆
        // oldToken空，newToken不空，第一次登陆
        if(StringUtils.isBlank(oldToken)&&StringUtils.isNotBlank(newToken)){
            token = newToken;
        }
        // oldToken不空，newToken空，之前登陆过
        if(StringUtils.isNotBlank(oldToken)&&StringUtils.isBlank(newToken)){
            token = oldToken;
        }
        // oldToken不空，newToken不空，cookie中的token过期
        if(StringUtils.isNotBlank(oldToken)&&StringUtils.isNotBlank(newToken)){
            token = newToken;
        }


        // 验证
        if (StringUtils.isNotBlank(token)) {
            // 获得当前请求的ip
            String ip = getMyIpFromRequest(request);

            // 远程调用认证中心，验证token   [通过java发起请求]   -- token验证成功返回success
            String url = "http://localhost:8085/verify?token="+token+"&currentIp="+ip;// 一个基于http的rest风格的webservice请求
            String success = HttpClientUtil.doGet(url);

            if(success.equals("success")){  // token验证通过
                // 将token重新写入浏览器cookie，刷新用户token的过期时间
                CookieUtil.setCookie(request,response,"oldToken",token,60*60*24,true);

                // 将用户信息放入请求中
                Map gmallguigu = JwtUtil.decode("gmallguigu", token, ip);
                request.setAttribute("userId",gmallguigu.get("userId"));
                request.setAttribute("nickName",gmallguigu.get("nickName"));
                return true;
            }
        }

        if (b) {
            // token为空，并且需要登陆
            response.sendRedirect("http://localhost:8085/index?returnUrl=" + request.getRequestURL());
            return false;
        } else {
            // token为空，并且不需要验证
            return true;
        }
    }

    /***
     * 获得客户端ip
     * @param request
     * @return
     */
    private String getMyIpFromRequest(HttpServletRequest request) {
        String ip = "";
        ip = request.getRemoteAddr();//获取ip地址
        if(StringUtils.isBlank(ip)){
            ip = request.getHeader("x-forwarder-for"); // 如果请求域地址中获取不到，就去请求头中获取
            if(StringUtils.isBlank(ip)){
                ip = "127.0.0.1"; // 还是获取不到，随便给个，避免为null
            }
        }
        return ip;
    }


}
