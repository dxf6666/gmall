package com.guigu.gmall.service;

import com.guigu.gmall.bean.UserAddress;
import com.guigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserService {

    public List<UserInfo> getUserList();

    List<UserAddress> getAddressList();

    UserInfo login(UserInfo userInfo);

    List<UserAddress> getAddressListByUserId(String userId);
}
