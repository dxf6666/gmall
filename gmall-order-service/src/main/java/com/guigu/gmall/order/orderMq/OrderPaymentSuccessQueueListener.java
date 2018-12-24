package com.guigu.gmall.order.orderMq;

import com.guigu.gmall.bean.OrderInfo;
import com.guigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.text.SimpleDateFormat;
import java.util.Calendar;

@Component
public class OrderPaymentSuccessQueueListener {
    @Autowired
    OrderService orderService;

    // spring的jms监听
    // destination: 监听目标  监听哪个消息的发送者
    // containerFactory: 监听类型  @Bean(name = "jmsQueueListener")
    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage) {
        try {
            // 获取消息内容
            String outTradeNo = mapMessage.getString("outTradeNo");
            String trackingNo = mapMessage.getString("trackingNo");
            System.err.println(outTradeNo+"该订单已经支付成功，根据这个消息，进行订单的后续业务");
            // 订单状态、支付方式、预计送达时间、支付宝交易号、整体状态

            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOutTradeNo(outTradeNo);
            orderInfo.setTrackingNo(trackingNo);//支付成功支付宝会返回流水号
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE,3);
            orderInfo.setExpireTime(c.getTime());//预计送达时间
            orderInfo.setOrderStatus("订单已支付");//未支付、已支付
            orderInfo.setProcessStatus("订单已支付");//订单的总体状态：已下单、已支付、已送达
            // 修改订单信息
            OrderInfo orderInfo1 = orderService.updateOrder(orderInfo);


            // 发送订单状态通知给库存系统
            orderService.sendOrderResultQueue(orderInfo1);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
