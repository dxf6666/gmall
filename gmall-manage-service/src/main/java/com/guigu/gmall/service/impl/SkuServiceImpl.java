package com.guigu.gmall.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.guigu.gmall.RedisUtil;
import com.guigu.gmall.bean.SkuAttrValue;
import com.guigu.gmall.bean.SkuImage;
import com.guigu.gmall.bean.SkuInfo;
import com.guigu.gmall.bean.SkuSaleAttrValue;
import com.guigu.gmall.mapper.SkuAttrValueMapper;
import com.guigu.gmall.mapper.SkuImageMapper;
import com.guigu.gmall.mapper.SkuInfoMapper;
import com.guigu.gmall.mapper.SkuSaleAttrValueMapper;
import com.guigu.gmall.service.SkuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import java.util.List;

@Service
public class SkuServiceImpl implements SkuService {
    @Autowired
    private SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private RedisUtil redisUtil;


    @Override
    public List<SkuInfo> getSkuInfoList(SkuInfo skuInfo) {
        return skuInfoMapper.select(skuInfo);
    }

    @Override
    public void saveSku(SkuInfo skuInfo) {

        // 保存sku信息
        skuInfoMapper.insertSelective(skuInfo);
        String skuId = skuInfo.getId();

        // 保存平台属性关联信息
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuId);
            skuAttrValueMapper.insert(skuAttrValue);
        }

        // 保存销售属性关联信息
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
            skuSaleAttrValue.setSkuId(skuId);
            skuSaleAttrValueMapper.insert(skuSaleAttrValue);
        }


        // 保存sku图片信息
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuId);
            skuImageMapper.insert(skuImage);
        }

    }
    public SkuInfo getSkuByIdFromDB(String skuId){
        SkuInfo  skuInfo = new SkuInfo();
        skuInfo.setId(skuId);
        SkuInfo skuInfo1 = skuInfoMapper.selectOne(skuInfo);

        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImages =  skuImageMapper.select(skuImage);
        /*封装sku图片*/
        skuInfo1.setSkuImageList(skuImages);

        SkuSaleAttrValue skuSaleAttrValue=new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuId);
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.select(skuSaleAttrValue);
        skuInfo1.setSkuSaleAttrValueList(skuSaleAttrValueList);

        return skuInfo1;
    }
    //通过skuId 查询出skuInfo信息和skuImage\SkuSaleAttrValue
    @Override
    public SkuInfo getSkuById(String skuId) {
        SkuInfo skuInfo = null;
        String skuKey = "sku:"+skuId+":info"; //如 sku99info  --redis键       redis以键值存储
        // 【1： redis查询缓存 】
        Jedis jedis = redisUtil.getJedis(); //获取于redis客户端的连接
        String s = jedis.get(skuKey);      //查询缓存，得到json字符串
        //缓存结果处理
        if(StringUtils.isNotBlank(s) && !"empty".equals(s)){  //查询到缓存
            // 【2： 命中直接返回 】
            skuInfo = JSON.parseObject(s, SkuInfo.class); //将json字符串转成bean对象
        }else{                                                // 没有查询到缓存
            //【3： 未命中 】 -----> 【4：查询数据库】 --> 【5：更新缓存】

            //【4：查询数据库】
            // 获取分布式锁--- 另外一个redis连接，专门用来做redis分布式锁的，只有获取分布式锁和释放，由于内存有限，我这就使用了同一个redis连接
            String OK = jedis.set("sku:" + skuId + ":lock", "1", "nx", "px", 10000);
            /*
    EX second ：设置键的过期时间为 second 秒。 SET key value EX second 效果等同于 SETEX key second value 。
    PX millisecond ：设置键的过期时间为 millisecond 毫秒。 SET key value PX millisecond 效果等同于 PSETEX key millisecond value 。
    NX ：只在键不存在时，才对键进行设置操作。 SET key value NX 效果等同于 SETNX key value 。
    XX ：只在键已经存在时，才对键进行设置操作。
*/
            if(StringUtils.isNotBlank(OK)){
                //线程得到分布式锁，开始访问数据库
                skuInfo = getSkuByIdFromDB(skuId);
                if(null!=skuInfo){
                    //线程成功访问数据库，删除分布式锁
                    jedis.del("sku:" + skuId + ":lock");
                }
            }else{
                //线程需要访问数据库，但是未得到分布式锁，开始自旋
                jedis.close(); //关闭redis连接
                return getSkuById(skuId);
            }
            // 同步redis【5：更新缓存】
            if(null==skuInfo){
                //线程访问数据库后，发现数据库为空，将空值同步redis    -- 数据库查询发现没有该对象，那么下次直接查缓存就好
                jedis.set(skuKey,"empty");
            }
            if(null!=skuInfo&&!"empty".equals(s)){
                jedis.set(skuKey,JSON.toJSONString(skuInfo));//将bean对象转成json字符串，存储到redis内存中
            }
        }
        //线程结束访问返回
        jedis.close(); //关闭redis连接
        return skuInfo;
    }

     @Override
    public List<SkuInfo> getSkuByCatalog3Id(int catalog3Id) {
        //查询sku信息
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setCatalog3Id(catalog3Id+"");
        List<SkuInfo> skulist = skuInfoMapper.select(skuInfo);

        for (SkuInfo info : skulist) {
            String id = info.getId();
            //查询图片集合
            SkuImage skuImage = new SkuImage();
            skuImage.setSkuId(id);
            List<SkuImage> skuImages = skuImageMapper.select(skuImage);
            info.setSkuImageList(skuImages);

            //平台属性集合
            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(id);
            List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);
            info.setSkuAttrValueList(skuAttrValues);
        }
        System.out.print(skulist);
        return skulist;
    }
    @Override
    public List<SkuInfo> getMySkuInfo(String catalog3Id) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setCatalog3Id(catalog3Id);
        List<SkuInfo> skuInfos = skuInfoMapper.select(skuInfo);

        for (SkuInfo info : skuInfos) {
            String skuId = info.getId();

            SkuAttrValue skuAttrValue = new SkuAttrValue();
            skuAttrValue.setSkuId(skuId);
            List<SkuAttrValue> skuAttrValues = skuAttrValueMapper.select(skuAttrValue);
            info.setSkuAttrValueList(skuAttrValues);
        }
        return skuInfos;
    }

}
