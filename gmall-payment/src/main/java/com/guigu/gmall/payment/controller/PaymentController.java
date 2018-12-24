package com.guigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.guigu.gmall.bean.OrderInfo;
import com.guigu.gmall.bean.PaymentInfo;
import com.guigu.gmall.payment.conf.AlipayConfig;
import com.guigu.gmall.service.OrderService;
import com.guigu.gmall.payment.service.PaymentService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;
import java.util.HashMap;

@Controller
public class PaymentController {
    @Reference
    OrderService orderService;
    @Autowired
    AlipayClient alipayClient;
    @Autowired  /*使用了本地注入*/
    PaymentService paymentService;
    @RequestMapping("index")
    public String index(String outTradeNo,String totalAmount, ModelMap modelMap){
        modelMap.put("outTradeNo",outTradeNo);
        modelMap.put("totalAmount",totalAmount);
        return "index";
    }

    // 去结算，显示支付页面(显示出二维码)
    @RequestMapping("alipay/submit")
    @ResponseBody //表示该方法的返回结果直接写入 HTTP response body 中,所以是html代码写入浏览器直接解析
    public String alipay(String outTradeNo){
         System.out.println(outTradeNo);

        // 通过外部订单号获取订单信息
        OrderInfo orderInfo = orderService.getOrderInfoByOutTradeNo(outTradeNo);
        // 设置支付宝page.pay的请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建支付宝交易页面支付请求
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);// 支付成功的回调函数
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//异步通知地址

        //封装请求参数
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("out_trade_no",outTradeNo);//外部订单号
        paramMap.put("product_code","FAST_INSTANT_TRADE_PAY");
//        paramMap.put("total_amount",orderInfo.getTotalAmount());//待支付金额
        paramMap.put("total_amount",0.01);
        paramMap.put("subject",orderInfo.getOrderDetailList().get(0).getSkuName());//支付标题
        paramMap.put("body","硅谷支付产品测试");// 支付详情说明

        String s = JSON.toJSONString(paramMap);//将集合转成json，用于传递数据
        //填充业务参数
        alipayRequest.setBizContent(s);

        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单  <form action=...../>
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        // 保存支付、交易信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);//外部订单号
        paymentInfo.setCreateTime(new Date());//创建时间
        paymentInfo.setPaymentStatus("未支付");
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());//总金额
        paymentInfo.setSubject(orderInfo.getOrderDetailList().get(0).getSkuName());//标题
        paymentInfo.setOrderId(orderInfo.getId());
        paymentService.savePaymentService(paymentInfo);
        System.out.println(form);


        // 用户一旦打开支付页面，不管用户有没有支付，电商支付系统都不断积极去询问支付宝用户付款没，你有没有收到钱
        // 需要解决难题：1 怎么去查询该订单是否支付  方案：alipay.trade.query(统一收单线下交易查询)该接口提供所有支付宝支付订单的查询 ，
        // 查询方式有很多，常用做法请求参数中传入：out_trade_no商户订单号 、trade_no支付宝交易号、流水号    --out_trade_no是电商系统生成的，trade_no是支付宝回调后产生的。因为有可能一直就没收到支付宝的回调，也就没有trade_no，所以咱们这里使用out_trade_no。

        // 发送定时检查的延迟队列
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),5);
        // 参数1：通过商户订单号去查询支付宝订单           参数2：不管用户有没有支付，都去支付宝询问，5是询问次数

        return form;
    }

    //支付成功的回调函数
    @RequestMapping(value = "alipay/callback/return")
    //consumes = "接收客户端的参数格式",produces = "返回给客户端的结果格式"
    public String alipayReturn(HttpServletRequest request){//alipay回调参数 同步请求的通过request获取，异步是通过paramsMap
        // 回调接口首先需要验证阿里的签名  -- 确保这是阿里的回调函数
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(null, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
            if(signVerified){
                // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            }else{
                // TODO 验签失败则记录异常日志，并在response中返回failure.
            }
        } catch (Exception e) {
            System.out.println("验证阿里的签名");
        }

        // 获取支付宝的回调参数
        String tradeNo = request.getParameter("trade_no");// 支付宝流水号
        String callback = request.getQueryString();
        String outTradeNo = request.getParameter("out_trade_no");
        // 更新支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus("已支付");
        paymentInfo.setAlipayTradeNo(tradeNo);
        paymentInfo.setCallbackTime(new Date());//回调时间
        paymentInfo.setCallbackContent(callback);//回调数据
        paymentInfo.setOutTradeNo(outTradeNo);
        paymentService.updatePayment(paymentInfo);

        //支付成功后， 通知订单系统，更新订单信息【消息的发送者】  -- 在订单模块中，有消息的消费者，位于gmall-order-service的orderMq
        paymentService.sendPaymentSuccessQueue(paymentInfo);

        return "finish";
    }

}