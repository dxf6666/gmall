package com.guigu.gmall.conf;

import com.guigu.gmall.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration  // 定义配置类，可替换xml配置文件
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {  //WebMvcConfigurerAdapter等价于web.xml

    @Autowired
    AuthInterceptor authInterceptor;  //继承WebMvcConfigurerAdapter采用JavaBean形式实现个性化配置定制。

    @Override
    public void addInterceptors(InterceptorRegistry registry){   // 添加拦截器
        //把自定义的拦截器类添加进来即可
        registry.addInterceptor(authInterceptor).addPathPatterns("/**"); //配置拦截路径, 只拦截视图，就是@requestMapper(),所以静态资源并不拦截
        super.addInterceptors(registry);
    }
}
/*
*
* 实现拦截器功能需要完成以下2个步骤：

    创建我们自己的拦截器类并实现 HandlerInterceptor 接口
    其实重写WebMvcConfigurerAdapter中的addInterceptors方法把自定义的拦截器类添加进来即可

*/