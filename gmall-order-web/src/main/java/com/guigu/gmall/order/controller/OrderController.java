package com.guigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.guigu.gmall.annotation.LoginRequire;
import com.guigu.gmall.bean.*;
import com.guigu.gmall.bean.enums.PaymentWay;
import com.guigu.gmall.service.CartService;
import com.guigu.gmall.service.SkuService;
import com.guigu.gmall.service.UserService;
import com.guigu.gmall.service.OrderService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    UserService userService;

    @Reference
    CartService cartService;

    @Reference
    OrderService orderService;

    @Reference
    SkuService skuService;

    @LoginRequire(needSuccess = true)
    @RequestMapping("toTrade")
    public String toTrade(HttpServletRequest request, ModelMap map){

        // 验证用户是否登陆
        String userId = (String)request.getAttribute("userId");// "2";
        // 获取用户收获地址列表
        List<UserAddress> userAddresses = userService.getAddressListByUserId(userId);

        // 获取用户订单详情
        List<OrderDetail> orderDetails = new ArrayList<>();

        List<CartInfo> cartInfos = cartService.getCartInfosFromCacheByUserId(userId);

        BigDecimal totalAmount = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfos) {
            if(cartInfo.getIsChecked().equals("1")){ //单选框选中的商品
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                // 订单中每一件商品价格
                orderDetail.setOrderPrice(new BigDecimal(cartInfo.getCartPrice()+""));
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetails.add(orderDetail);

                totalAmount = totalAmount.add(orderDetail.getOrderPrice());
            }
        }
        map.put("userAddressList",userAddresses);
        map.put("orderDetailList",orderDetails);
        // 应付总金额
        map.put("totalAmount",totalAmount);

        // 生成一个唯一的交易码
        String tradeCode = orderService.genTradeCode(userId);
        map.put("tradeCode",tradeCode);
        return "trade";
    }


    @LoginRequire(needSuccess = true)
    @RequestMapping("submitOrder")
    public String submitOrder(HttpServletRequest request,String tradeCode, String addressId ,ModelMap map){
        // 获取用户登录信息
        String userId = (String)request.getAttribute("userId");
        // 获取userAddress信息
        UserAddress userAddress = orderService.getUserAddressById(addressId);

        // 获取该用户购物车信息
        List<CartInfo> cartInfos = cartService.getCartInfosFromCacheByUserId(userId);
        // 用户点击【去支付】生成订单详情页，在订单详情页中点击【去支付】跳转到收银台
        // 在生成订单详情页同时生成一个UUID唯一标识，并存入缓存中，在【去支付】业务中从redis缓存中根据userID获取UUID
        // 如果两个UUID一致
        boolean b = orderService.checkTradeCode(userId,tradeCode);
        if (b){

            //声明订单对象
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setProcessStatus("订单提交");// 订单处于流程中哪个状态
            orderInfo.setOrderStatus("订单未支付");
            // 订单过期日期  ：在生成订单后+1天过期
            Calendar c = Calendar.getInstance();//使用默认时区和区域设置的日历。
            c.add(Calendar.DATE,1);//添加或减去指定的时间给定日历领域
            orderInfo.setExpireTime(c.getTime());
            // 外部订单号
            SimpleDateFormat s = new SimpleDateFormat("yyyyMMddHHmmss");
            String format = s.format(new Date());
            String outTradeNo = "guigu"+format+System.currentTimeMillis();//唯一的
            orderInfo.setOutTradeNo(outTradeNo);

            orderInfo.setConsigneeTel(userAddress.getPhoneNum());// 收件人手机号码
            orderInfo.setCreateTime(new Date());//订单创建时间
            orderInfo.setDeliveryAddress(userAddress.getUserAddress());
            orderInfo.setOrderComment("硅谷快递，即时送达");
            orderInfo.setUserId(userId);
            orderInfo.setTotalAmount(getTotalPrice(cartInfos));
            orderInfo.setPaymentWay("在线支付");//支付方式
            orderInfo.setConsignee(userAddress.getConsignee());// 收件人姓名

            //封装订单信息
            List<String> cartIds = new ArrayList<>();//购物车商品id
            List<OrderDetail> orderDetails = new ArrayList<>();

            for (CartInfo cartInfo : cartInfos) {
                if (cartInfo.getIsChecked().equals("1")){
                    String id = cartInfo.getId();
                    cartIds.add(id);

                    OrderDetail orderDetail = new OrderDetail();
                    orderDetail.setImgUrl(cartInfo.getImgUrl());
                    orderDetail.setSkuName(cartInfo.getSkuName());
                    orderDetail.setSkuNum(cartInfo.getSkuNum());
                    orderDetail.setSkuId(cartInfo.getSkuId());
                    // 验库存
                    orderDetail.setHasStock("1");
                    // 验价格  -- 购物车中的商品价格是否和db中商品价格一致
                    SkuInfo skuInfo = skuService.getSkuById(cartInfo.getSkuId());// db中的商品
                    if(skuInfo.getPrice().compareTo(cartInfo.getSkuPrice())==0){ //BigDecimal要使用 compareTo来比较是否相等，相等返回0，<返回-1  >返回1
                        orderDetail.setOrderPrice(cartInfo.getCartPrice());
                    }else{
                        return "OrderErr";
                    }
                    orderDetails.add(orderDetail);
                }
            }

            orderInfo.setOrderDetailList(orderDetails);

            // 保存订单
            orderService.saveOrder(orderInfo);
            // 删除购物车数据  -- 你都买了，买了就删除购物车中的数据
            cartService.deleteCart(StringUtils.join(cartIds,",") ,userId); // cartIds已经下单的购物商品cartInfo的主键
            // 提交订单后重定向到支付系统
            return "redirect:http://localhost:8088/index?outTradeNo="+outTradeNo+"&totalAmount="+getTotalPrice(cartInfos);

        }else{
            return "orderErr";
        }

    }

    // 计算购物车总价格
    private BigDecimal getTotalPrice(List<CartInfo> cartInfos) {
        BigDecimal totalPrice = new BigDecimal("0");
        for (CartInfo cartInfo : cartInfos) {
            if (cartInfo.getIsChecked().equals("1")){
                totalPrice = totalPrice.add(cartInfo.getCartPrice());
            }
        }
        return totalPrice;
    }
}
