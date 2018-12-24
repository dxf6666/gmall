package com.guigu.gmall.utils;

import io.jsonwebtoken.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @param
 * @return
 */
public class JwtUtil {

    public static void main(String[] args){
        Map<String,String> map = new HashMap<>();
        map.put("userId","2");
        map.put("nickName","boge");
        String salt = "192.168.2.1";
        String gmallguigu = encode("gmallguigu", map, salt);

        System.out.println(gmallguigu);

        Map atguigugmall05081 = decode("gmallguigu", gmallguigu, "192.168.2.1");

        System.out.println(atguigugmall05081);

    }
    /***
     * jwt加密
     * @param key  服务器密钥
     * @param map   用户私人数据
     * @param salt   盐值
     * @return  token
     */
    public static String encode(String key,Map map,String salt){
        if(salt!=null){
            key+=salt;
        }
        JwtBuilder jwtBuilder = Jwts.builder().signWith(SignatureAlgorithm.HS256, key);
        jwtBuilder.addClaims(map);

        String token = jwtBuilder.compact();
        return token;
    }

    /***
     * jwt解密
     * @param key
     * @param token
     * @param salt
     * @return claims
     * @throws SignatureException
     */
    public static  Map decode(String key,String token,String salt)throws SignatureException {
        if(salt!=null){
            key+=salt;
        }
        Claims map = null;
        map = Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        System.out.println("map.toString() = " + map.toString());
        return map;

    }
}
