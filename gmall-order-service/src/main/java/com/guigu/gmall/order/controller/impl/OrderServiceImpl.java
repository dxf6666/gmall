package com.guigu.gmall.order.controller.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.guigu.gmall.ActiveMQUtil;
import com.guigu.gmall.RedisUtil;
import com.guigu.gmall.bean.OrderDetail;
import com.guigu.gmall.bean.OrderInfo;
import com.guigu.gmall.bean.UserAddress;
import com.guigu.gmall.order.mapper.OrderDetailMapper;
import com.guigu.gmall.order.mapper.OrderInfoMapper;
import com.guigu.gmall.order.mapper.UserAddressMapper;
import com.guigu.gmall.service.OrderService;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    UserAddressMapper userAddressMapper;
    @Autowired
    OrderInfoMapper orderInfoMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public String genTradeCode(String userId) {
        // UUID用它可以产生一个号称全球唯一的ID. UUID是由一个十六位的数字组成
        String uuid = UUID.randomUUID().toString();

        Jedis jedis = redisUtil.getJedis();
        jedis.setex("user:"+userId+":tradeCode",1000*60*15,uuid);
        jedis.close();

        return uuid;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCode) {
        boolean b = false;
        Jedis jedis = redisUtil.getJedis();
        String tradeCodeRedis = jedis.get("user:"+userId+":tradeCode");
        if (tradeCodeRedis != null && tradeCode.equals(tradeCode)){
            b = true;
            jedis.del("user:" + userId + ":tradeCode");
        }
        jedis.close();
        return b;
    }

    @Override
    public UserAddress getUserAddressById(String addressId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setId(addressId);
        return userAddressMapper.selectOne(userAddress) ;
    }

    @Override
    public void saveOrder(OrderInfo orderInfo) {
        orderInfoMapper.insertSelective(orderInfo);

        String orderId = orderInfo.getId();

        //根据orderId插入订单详情表
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();

        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderId);
            orderDetailMapper.insertSelective(orderDetail);
        }
    }

    @Override
    public OrderInfo getOrderInfoByOutTradeNo(String outTradeNo) {
        System.out.println("=="+outTradeNo);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOutTradeNo(outTradeNo);
        orderInfo = orderInfoMapper.selectOne(orderInfo);

        String orderId = orderInfo.getId();

        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetails = orderDetailMapper.select(orderDetail);

        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }

    @Override
    public OrderInfo updateOrder(OrderInfo orderInfo) {
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("outTradeNo",orderInfo.getOutTradeNo() );

        orderInfoMapper.updateByExampleSelective(orderInfo, example);
        return orderInfoMapper.selectOne(orderInfo);
    }

    @Override
    public void sendOrderResultQueue(OrderInfo orderInfo) {
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // 消息对象                                订单结果消息队列
            Queue testqueue = session.createQueue("ORDER_RESULT_QUEUE");
            MessageProducer producer = session.createProducer(testqueue);

            // 消息内容   有两种，一种text,支持json格式，一种是map
            TextMessage textMessage=new ActiveMQTextMessage();

            textMessage.setText(JSON.toJSONString(orderInfo));

            producer.setDeliveryMode(DeliveryMode.PERSISTENT);// 持久化

            // 发出消息
            producer.send(textMessage);
            session.commit();
            connection.close();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
