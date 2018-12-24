package com.guigu.gmall.service;

import com.guigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {
    CartInfo ifCartExits(CartInfo cartInfo);

    void insertCart(CartInfo cartInfo);

    void updateCart(CartInfo cartInfoDb);

    void flushCartCacheByUserId(String userId);

    List<CartInfo> getCartInfosFromCacheByUserId(String userId);

    void updateCartByUserId(CartInfo cartInfo);

    void combine(String userId, List<CartInfo> listCartCookie);

    void deleteCart(String join, String userId);
}
