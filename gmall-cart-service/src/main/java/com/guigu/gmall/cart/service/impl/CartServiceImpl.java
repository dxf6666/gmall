package com.guigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.guigu.gmall.RedisUtil;
import com.guigu.gmall.bean.CartInfo;
import com.guigu.gmall.cart.mapper.CartInfoMapper;
import com.guigu.gmall.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class CartServiceImpl implements CartService {
    @Autowired
    CartInfoMapper cartInfoMapper;
    @Autowired
    RedisUtil redisUtil;
    @Override
    public CartInfo ifCartExits(CartInfo cartInfo) {
        // 通过商品的[skuId、userId]查询表 ,如果购物车表中存在返回该对象，不存在返回null

        Example e = new Example(CartInfo.class);

        e.createCriteria().andEqualTo("userId", cartInfo.getUserId()).andEqualTo("skuId", cartInfo.getSkuId());

        CartInfo cartInfoReturn = cartInfoMapper.selectOneByExample(e);

        return cartInfoReturn;
    }

    @Override
    public void insertCart(CartInfo cartInfo) {
        cartInfoMapper.insertSelective(cartInfo);

        // 同步缓存
        flushCartCacheByUserId(cartInfo.getUserId());
    }

    @Override
    public void updateCart(CartInfo cartInfoDb) {
        cartInfoMapper.updateByPrimaryKeySelective(cartInfoDb);
        // 同步缓存
        flushCartCacheByUserId(cartInfoDb.getUserId());
    }

    /*刷新购物车缓存*/
    @Override
    public void flushCartCacheByUserId(String userId) {
        // 根据userId 查询用户在购物车中添加的所有商品
        // 查询userId对应的购物车集合
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        List<CartInfo> cartInfos = cartInfoMapper.select(cartInfo);

        if (null != cartInfos && cartInfos.size() > 0) {
            // 将购物车集合转化为map
            Map<String, String> map = new HashMap<String, String>();
            for (CartInfo info : cartInfos) {
                map.put(info.getId(), JSON.toJSONString(info));
            }

            Jedis jedis = redisUtil.getJedis();
            // 将购物车的hashMap放入redis
            jedis.del("cart:" + userId + ":Map");
            jedis.hmset("cart:" + userId + ":Map", map);
            jedis.close();
        } else {
            // 清理redis
            Jedis jedis = redisUtil.getJedis();
            // 将购物车的hashMap放入redis
            Map<String,String> map = new HashMap<>();
            jedis.del("cart:" + userId + ":Map");
            jedis.close();
        }
    }

    @Override
    public List<CartInfo> getCartInfosFromCacheByUserId(String userId) {
        ArrayList<CartInfo> cartInfos = new ArrayList<>();
        // 从缓存中查出数据
        Jedis jedis = redisUtil.getJedis();
        // 通过key查询value，返回json字符串
        List<String> hvals = jedis.hvals("cart:" + userId + ":Map");
        if (hvals.size()>0 && hvals != null){
            for (String hval : hvals) {
                CartInfo cartInfo = JSON.parseObject(hval, CartInfo.class);
                cartInfos.add(cartInfo);
            }
        }
        jedis.close();
        return cartInfos;
    }
    // 购物车中用户点击或取消商品的选中状态
    @Override
    public void updateCartByUserId(CartInfo cartInfo) {
        // 根据什么字段进行修改
        Example e = new Example(CartInfo.class);  // 单词：作为…的例子
        e.createCriteria().andEqualTo("skuId",cartInfo.getSkuId()).andEqualTo("userId",cartInfo.getUserId());

        cartInfoMapper.updateByExampleSelective(cartInfo,e);
        // 同步缓存
        flushCartCacheByUserId(cartInfo.getUserId());
    }

    /*将cookie中的购物车添加到用户已登录的购物车中*/
    @Override
    public void combine(String userId, List<CartInfo> listCartCookie) {

        // 查询该用户下的购物车（购物车中包含多种商品）
        CartInfo cartInfo = new CartInfo();
        cartInfo.setUserId(userId);
        List<CartInfo> listCartDb = cartInfoMapper.select(cartInfo);

        // 判断从cookie中取出的购物车是否为空
        if(null!=listCartCookie&&listCartCookie.size()>0){
            // 进cookie中的每一件商品添加到用户登录状态的购物车中(cookie只是临时的，用户登录才是存储在db中的)
            for (CartInfo cartCookie : listCartCookie) {
                String skuIdCookie = cartCookie.getSkuId();
                //查看cookie中的商品和用户登录状态下购物车的商品，是否sku一样，一样说明是同一件商品，那么就修改数量和价格，如果不一样，那么就添加
                boolean b = true;
                if(null!=listCartDb&&listCartDb.size()>0){
                    b = if_new_cart(listCartDb, cartCookie);
                }

                if (!b) {  // 一样，需要更新
                    CartInfo cartDb = new CartInfo();
                    // 更新
                    for (CartInfo info : listCartDb) {
                        if (info.getSkuId().equals(cartCookie.getSkuId())) {
                            cartDb = info;
                        }
                    }
                    cartDb.setSkuNum(cartCookie.getSkuNum());
                    cartDb.setIsChecked(cartCookie.getIsChecked());
                    cartDb.setCartPrice(cartDb.getSkuPrice().multiply(new BigDecimal(cartDb.getSkuNum())));
                    cartInfoMapper.updateByPrimaryKeySelective(cartDb);
                } else {
                    //不一样，需要 添加
                    cartCookie.setUserId(userId);
                    cartInfoMapper.insertSelective(cartCookie);
                }
            }
        }
        // 同步刷新缓存
        flushCartCacheByUserId(userId);
    }

    /* 用户购买完商品，把该商品从购物车中删除 */
    @Override
    public void deleteCart(String join, String userId) {
        // 删除购物车已经下单的数据
        cartInfoMapper.deleteCartsById(join);

        // 同步购物车缓存
        flushCartCacheByUserId(userId);
    }

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
}
