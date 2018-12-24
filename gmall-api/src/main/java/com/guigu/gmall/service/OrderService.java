package com.guigu.gmall.service;

import com.guigu.gmall.bean.OrderInfo;
import com.guigu.gmall.bean.UserAddress;

public interface OrderService {
     String genTradeCode(String userId);

     boolean checkTradeCode(String userId, String tradeCode);

     UserAddress getUserAddressById(String addressId);

     void saveOrder(OrderInfo orderInfo);

     OrderInfo getOrderInfoByOutTradeNo(String outTradeNo);

    OrderInfo updateOrder(OrderInfo orderInfo);

     void sendOrderResultQueue(OrderInfo orderInfo);
}
