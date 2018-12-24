package com.guigu.gmall.user.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.guigu.gmall.RedisUtil;
import com.guigu.gmall.bean.UserAddress;
import com.guigu.gmall.bean.UserInfo;
import com.guigu.gmall.service.UserService;
import com.guigu.gmall.user.mapper.UserAddressMapper;
import com.guigu.gmall.user.mapper.UserInfoMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;


import java.util.List;
/* 导入的是 alibaba.dubbo 的@Service注解
   [远程实现类] com.alibaba.dubbo.config.annotation.Service：用于dubbo接口,dubbo是阿里巴巴公司开源的一个高性能优秀的服务框架，
   [本地实现类] org.springframework.stereotype.Service：用于各种Service的实现类,对应的是业务层Bean
*/

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserInfoMapper userInfoMapper;

    @Autowired
    UserAddressMapper userAddressMapper;

    @Autowired
    RedisUtil redisUtil;
    @Override
    public List<UserInfo> getUserList() {
        /* 调用通用mybatis方法 */
        List<UserInfo> userInfoList = userInfoMapper.selectAll();
        return userInfoList;
    }

    @Override
    public List<UserAddress> getAddressList() {
        return userAddressMapper.selectAll();
    }



    @Override    //核对后台登录信息+用户登录信息载入缓存
    public UserInfo login(UserInfo userInfo) {
        //获取用户输入的密码，经过加密后去数据库查询
        String pwd = DigestUtils.md5Hex(userInfo.getPasswd());
        userInfo.setPasswd(pwd);

        UserInfo userInfoResult  = userInfoMapper.selectOne(userInfo);
        if (userInfoResult != null){  // 验证通过
            // 加入缓存
            Jedis jedis = redisUtil.getJedis();  // 获取缓存连接
            String userInfoKey="user:"+userInfoResult.getId()+"info"; // key
            String userInfoJson = JSON.toJSONString(userInfoResult); // value  :json字符串格式
            jedis.set(userInfoKey,userInfoJson);
            
            return userInfoResult;
        }else {  // 验证失败
            return null;
        }
    }


    @Override
    public List<UserAddress> getAddressListByUserId(String userId) {

        UserAddress userAddress =  new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> userAddresses = userAddressMapper.select(userAddress);

        return userAddresses;
    }
}
