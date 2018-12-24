package com.guigu.gmall.user.mapper;


import com.guigu.gmall.bean.UserInfo;
import tk.mybatis.mapper.common.Mapper;

public interface UserInfoMapper extends Mapper<UserInfo> {
    /*
    * 以前做法：  写一个接口方法，然后在XxxMapper.xml 中配置下该方法获取数据
    *
    * 现在做法：  继承Mapper接口，该接口是别人帮我们写好的对表单的基本sql操作[通用mybatis]     --位于【tk.mybatis.mapper.common.Mapper】包下

    * */
}
