package com.guigu.gmall.payment.service;

import com.guigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    void savePaymentService(PaymentInfo paymentInfo);

    void updatePayment(PaymentInfo paymentInfo);

    void sendPaymentSuccessQueue(PaymentInfo paymentInfo);

    void sendDelayPaymentResult(String outTradeNo, int i);

    Map<String, String> checkAlipayPayment(String outTradeNo);

    boolean checkStatus(String outTradeNo);
}
