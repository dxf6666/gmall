package com.guigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.guigu.gmall.annotation.LoginRequire;
import com.guigu.gmall.bean.CartInfo;
import com.guigu.gmall.bean.SkuInfo;
import com.guigu.gmall.service.SkuService;
import com.guigu.gmall.service.CartService;
import com.guigu.gmall.utils.CookieUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class CartController {
    /* 如果单单addToCart()就跳转到购物车，转发地址栏不变，还是http:....addToCart  那么每次刷新就再次提交

       用户添加购物车addToCart --> 重定向到 cartSuccess(购物车页面) ，重定向地址栏发生变化，每次刷新购物车就不会再次提交
       避免： 用户刷新再次提交购物车
    * */
    @Reference
    SkuService skuService;
    @Reference
    CartService cartService;

    @LoginRequire(needSuccess = false)
    @RequestMapping("addToCart")
    public String addToCart(HttpSession session, HttpServletRequest request, HttpServletResponse response, @RequestParam Map<String,String>  map){
        // 【添加购物车的业务逻辑： 用户没有登录，使用cookie技术，cookie以key=value形式存放，key就代表购物车名称，value就是购物车存放的所有商品cartInfo】
        //  用户登录，查db,放redis技术，
        String skuId = map.get("skuId"); // 添加到购物车的商品skuid
        Integer num = Integer.parseInt(map.get("num"));  // 添加数量
        SkuInfo skuInfo = skuService.getSkuById(skuId);  // 通过skuid去获取具体的sku商品

        CartInfo cartInfo = new CartInfo(); //购物车中每一件商品
        cartInfo.setCartPrice(skuInfo.getPrice().multiply(new BigDecimal(num))); //商品价格*数量
        cartInfo.setSkuNum(num);
        cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
        cartInfo.setSkuPrice(skuInfo.getPrice());
        cartInfo.setSkuName(skuInfo.getSkuName());
        cartInfo.setSkuId(skuInfo.getId());
        cartInfo.setIsChecked("1");// 选中待支付状态

        String userId = (String)request.getAttribute("userId");// "2";
        // 购物车，用来存放商品，到时候一整车商品 添加到cookie中
        List<CartInfo> cartInfos = new ArrayList<>();
        // 【用户未登录时添加购物车  -- 使用cookie技术】
        if (StringUtils.isBlank(userId)){
            //1: 获取cookie数据      数据为null：说明购物车空空如也     不为null：购物车有商品
            String cookieValue = CookieUtil.getCookieValue(request, "listCartCookie", true);//获取指定cookie的value值
            if (StringUtils.isNotBlank(cookieValue)){
                //2: 购物车有商品，将cookie中的数据(json)转成商品对象(bean)存放到购物车   -- 判断要添加的商品在购物车中是否存在，存在就更新(数量、价格变化), 不存在就添加)
                cartInfos = JSON.parseArray(cookieValue, CartInfo.class); //将json字符串转成bean对象集合
                // 判断是更新还是添加该商品  (流程：把购物车中所有的商品循环，当循环到商品sku和你要添加的商品sku是同一个，说明是同一件，那就更新，如果不是就添加)
                boolean b = if_new_cart(cartInfos,cartInfo);
                if(b){
                    // 添加
                    cartInfos.add(cartInfo);
                }else{
                    // 更新
                    for (CartInfo info : cartInfos) { // 循环购物车中所有商品
                        if(info.getSkuId().equals(cartInfo.getSkuId())){  // 如果要添加的sku在购物车中也有该sku，那么说明是同一件商品，那么就修改数量和价格
                            info.setSkuNum(info.getSkuNum()+cartInfo.getSkuNum());  //数量 （新添加的数量+购物车中之前添加的数量）
                            info.setCartPrice(info.getSkuPrice().multiply(new BigDecimal(info.getSkuNum())));  // 价格
                        }
                    }
                }
            }else {
                //3: 购物车空空如也，直接添加商品到购物车
                cartInfos.add(cartInfo);
            }
            // 将购物车数据放入cookie
            CookieUtil.setCookie(request,response,"listCartCookie",JSON.toJSONString(cartInfos),60*60*24,true);
        }else{  // ===【用户已登录时添加购物车】 ===
            cartInfo.setUserId(userId); // 用户已经登录，该商品属于哪个用户添加的
            CartInfo cartInfoDb = cartService.ifCartExits(cartInfo); // 判断购物车中是否以添加过该商品，之前添加过就返回该商品,没有添加过就返回null
            if(cartInfoDb == null){
                //返回null: 之前没有添加过，那么现在就添加下
                cartService.insertCart(cartInfo);
            }else {
                // 返回商品对象: 之前添加过，那么就更新
                // 更新数量      之前添加的数量+现在又要添加的数量
                cartInfoDb.setSkuNum(cartInfoDb.getSkuNum() + cartInfo.getSkuNum());
                // 更新价格
                cartInfoDb.setCartPrice(cartInfoDb.getSkuPrice().multiply(new BigDecimal(cartInfoDb.getSkuNum())));
                cartService.updateCart(cartInfoDb);
            }

            // 同步缓存  ： 用户添加完购物车就同步下缓存，到时候用户查看购物车就从缓存中获取数据，效率高
            cartService.flushCartCacheByUserId(userId);
        }
        session.setAttribute("cartInfo",cartInfo);
        return "redirect:/cartSuccess";// 避免用户每次刷新就再次提交，  -- 第一次是提交，第二次页面就重定向cartSucuccess，所以只会提交一次
    }

    @LoginRequire(needSuccess = false)
    @RequestMapping("cartSuccess")
    public String Cart(){
        return "success";
    }


    /***
     * 用户点击加入购物车，判断该sku数据更新还是新增
     * @param listCartCookie 获取之前添加的sku
     * @param cartInfo  现在要添加的sku
     * @return
     */
    private boolean if_new_cart(List<CartInfo> listCartCookie, CartInfo cartInfo) {
        boolean b = true; //默认true添加
        for (CartInfo info : listCartCookie) {
            if (info.getSkuId().equals(cartInfo.getSkuId())){
                //同一件商品，重复那就更新
                b = false;
            }
        }
        return b;
    }

    @LoginRequire(needSuccess = false)
    @RequestMapping("cartList")
    public String cartList(HttpServletRequest request, ModelMap map){
        List<CartInfo> cartInfos = new ArrayList<>();
        String userId =  (String)request.getAttribute("userId");// "2";
        if (StringUtils.isBlank(userId)){
            //取 cookie数据
            String listCartCookie = CookieUtil.getCookieValue(request, "listCartCookie", true);
            // 将json字符串转成bean对象
            cartInfos = JSON.parseArray(listCartCookie,CartInfo.class);
        }else {
            //取 redis缓存数据
            cartInfos = cartService.getCartInfosFromCacheByUserId(userId);
        }
        map.put("cartList",cartInfos);
        if(cartInfos != null && cartInfos.size()>0){
            BigDecimal totalPrice = getTotalPrice(cartInfos);
            map.put("totalPrice",totalPrice);
        }

        return "cartList";
    }

    private   BigDecimal getTotalPrice(List<CartInfo> cartInfos) {
        BigDecimal totalPrice = new BigDecimal("0");// 绝对不会用double\float   ，数字比较实用compareTo
        // 加add 减subtract 乘multiply 除divide
        for (CartInfo cartInfo : cartInfos) {
            String isChecked = cartInfo.getIsChecked();//是否选中
            if(isChecked.equals("1")){ //  选中
                totalPrice = totalPrice.add(cartInfo.getCartPrice());
            }
        }
        return totalPrice;
    }

    @LoginRequire(needSuccess = false)
    @RequestMapping("checkCart")
    public String checkCart(@RequestParam Map<String,String> map ,HttpServletRequest request,HttpServletResponse response,ModelMap modelMap){
        String isChecked = map.get("isChecked");
        String skuId = map.get("skuId");
        // 购物车
        List<CartInfo> cartInfos = new ArrayList<>();
        String userId = (String)request.getAttribute("userId");// "2";
        if (StringUtils.isBlank(userId)){
            //用户没有登录 -- 修改cookie
            // 获取cookie数据(json)
            String listCartCookie = CookieUtil.getCookieValue(request, "listCartCookie", true);
            // 将json字符串转成bean对象，且数组形式
            cartInfos = JSON.parseArray(listCartCookie, CartInfo.class);
            for (CartInfo Info : cartInfos) {
                String skuId1 = Info.getSkuId();
                if(skuId1.equals(skuId)){
                    Info.setIsChecked(isChecked);
                }
            }
            CookieUtil.setCookie(request,response,"listCartCookie",JSON.toJSONString(cartInfos),60*60*24,true);
        }else {
            // 用户已登录  -- 修改db 、同步缓存
            CartInfo cartInfo = new CartInfo();
            cartInfo.setIsChecked(isChecked);
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartService.updateCartByUserId(cartInfo);
            cartInfos = cartService.getCartInfosFromCacheByUserId(userId);
        }
        // 返回购物车列表的最新数据
        modelMap.put("cartList",cartInfos);
        BigDecimal totalPrice = getTotalPrice(cartInfos);
        modelMap.put("totalPrice",totalPrice);
        return "cartListInner";
    }
}
/*
*  获取cookie  ：request.getCookie
*  添加cookie  ：response.addCookie
* */