package com.guigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.guigu.gmall.ActiveMQUtil;
import com.guigu.gmall.bean.PaymentInfo;
import com.guigu.gmall.payment.mapper.PaymentInfoMapper;
import com.guigu.gmall.payment.service.PaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service  /*springboot的service*/
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentService(PaymentInfo paymentInfo) {
        System.out.println("====================老子调用了"+paymentInfo.getOutTradeNo());
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        // 更新数据，总不能把全部数据都更新了吧，而是根据 paymentInfo.getOutTradeNo()去更新数据表中的outTradeNo列
        Example e = new Example(PaymentInfo.class);
        e.createCriteria().andEqualTo("outTradeNo",paymentInfo.getOutTradeNo());
        paymentInfoMapper.updateByExampleSelective(paymentInfo,e);
    }


    @Override
    public void sendPaymentSuccessQueue(PaymentInfo paymentInfo) {
        // 发送支付成功消息队列
        // 队列模式的消息生产者
        Connection connection = activeMQUtil.getConnection();//获取连接
        try{
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);//创建事务类型的会话

            // 创建支付成功消息队列
            Queue testqueue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
            // 消息对象
            MessageProducer producer = session.createProducer(testqueue);

            // 消息内容   有两种，一种text,支持json格式，一种是map
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo",paymentInfo.getOutTradeNo());
            mapMessage.setString("trackingNo",paymentInfo.getAlipayTradeNo());
            mapMessage.setString("result",paymentInfo.getPaymentStatus());
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);// 持久化

            // 发出消息
            producer.send(mapMessage);
            session.commit();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void sendDelayPaymentResult(String outTradeNo, int count) {
        System.err.println("开始发送延迟检查支付状态的队列。。。");
        Connection connection = activeMQUtil.getConnection();

        try {
            connection.start();
            //第一个值表示是否使用事务，如果选择true，第二个值相当于选择0
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // 消息对象
            Queue queue = session.createQueue("PAYMENT_CHECK_QUEUE");//创建付款检查消息队列
            MessageProducer producer = session.createProducer(queue);

            // 消息内容
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("outTradeNo",outTradeNo);
            mapMessage.setInt("count",count);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);//持久化

            // 设置成延迟发送，延迟20秒发再发送队列    -- 不然的话直接发送
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,1000*20);

            // 发出消息
            producer.send(mapMessage);
            session.commit();
            connection.close();;
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, String> checkAlipayPayment(String outTradeNo) {
        // 通过商家订单号查询 支付宝的支付状态
        HashMap<String, String> returnMap = new HashMap<>();
        System.err.println("开始检查支付宝的支付状态。。第n次，返回支付结果");

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();//创建  Alipay交易查询请求

        Map<String,String> map = new HashMap<>();
        map.put("out_trade_no",outTradeNo);
        request.setBizContent(JSON.toJSONString(map)); // 将订单号set进请求参数中

        AlipayTradeQueryResponse response = null; // Alipay交易查询响应、结果
        try {
            response = alipayClient.execute(request); // 查询
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if (response.isSuccess()){
            // 用户扫码了
            String tradeNo = response.getTradeNo();//支付成功的话，支付宝会返回交易号/流水号
            String tradeStatus = response.getTradeStatus();// 支付宝返回交易的状态
            //   交易状态：WAIT_BUYER_PAY（交易创建，等待买家付款）、TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、TRADE_SUCCESS（交易支付成功）、TRADE_FINISHED（交易结束，不可退款）
            if (tradeStatus.equals("TRADE_SUCCESS")){  // 支付成功
                // 用户已经支付了
                returnMap.put("status",tradeStatus);
                returnMap.put("alipayTradeNo",tradeNo);
                returnMap.put("callback",response.getMsg());
                return returnMap;
            }else {
                // 用户犹豫中，还没有支付    // 等待支付
                returnMap.put("status","fail");
                return returnMap;
            }
        }else {
            // 用户没有扫码
            System.err.println("用户未创建交易");
            returnMap.put("status","fail");
            return returnMap;
        }
    }

    @Override
    public boolean checkStatus(String outTradeNo) {
        boolean b = false;

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentInfo = paymentInfoMapper.selectOne(paymentInfo);

        if(paymentInfo.getPaymentStatus().equals("已支付")){
            b = true;
        }
        return b;
    }

}
