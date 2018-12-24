package com.guigu.gmall.payment.payMq;

import com.guigu.gmall.bean.PaymentInfo;
import com.guigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentCheckQueueListener {
    @Autowired
    PaymentService paymentService;

    // 消息的消费端
    @JmsListener(destination = "PAYMENT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeCheckResult(MapMessage mapMessage) throws JMSException {
        // 获取消息发出者的内容
        int count = mapMessage.getInt("count");// 检查次数
        String outTradeNo = mapMessage.getString("outTradeNo");

        // 调用支付宝检查接口，得到支付状态  --> 如果支付成功支付状态：TRADE_SUCCESS
        Map<String, String> statusMap = paymentService.checkAlipayPayment(outTradeNo);

        // 根据支付情况决定是否调用支付成功队列，还是继续延迟检查
        if (statusMap.get("status").equals("TRADE_SUCCESS")){
            // 用户支付成功

            // 支付状态的幂等性判断    == 通过商品订单号去payment_info表查询该条数据的支付状态，如果'已支付'返回true,否则false
            boolean b = paymentService.checkStatus(outTradeNo);

            if(!b){
                // 刚刚交易成功
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setAlipayTradeNo(statusMap.get("alipayTradeNo"));
                paymentInfo.setPaymentStatus("已支付");
                paymentInfo.setCallbackTime(new Date());
                paymentInfo.setCallbackContent(statusMap.get("callback"));
                paymentInfo.setOutTradeNo(outTradeNo);
                // 更新支付信息
                System.err.println("进行第"+(6-count)+"次检查订单的支付状态，已经支付，更新支付信息发送成功过的队列");
                paymentService.updatePayment(paymentInfo);

                // 发送支付成功的队列 -- 给库存消费
                paymentService.sendPaymentSuccessQueue(paymentInfo);
            }else{
                System.err.println("检查到该比交易已经支付完毕，直接返回结果，消息队列任务结束");
            }
        }else {
            // 用户没有支付
            if(count>0){
                System.err.println("进行第"+(6-count)+"次检查订单的支付状态，未支付，继续发送延迟队列");
                paymentService.sendDelayPaymentResult(outTradeNo,count-1);//再次询问支付宝用户支付状态
            }else{
                System.err.println("检查次数上限，用户在规定时间内，没有支付");
            }
        }
    }
}
