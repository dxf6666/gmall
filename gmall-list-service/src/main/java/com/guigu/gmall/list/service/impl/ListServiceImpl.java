package com.guigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.guigu.gmall.bean.BaseAttrInfo;
import com.guigu.gmall.bean.SkuLsInfo;
import com.guigu.gmall.bean.SkuLsParam;
import com.guigu.gmall.list.mapper.AttrMapper;
import com.guigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {
    //   =====【 获取es数据 】===

    //es索引(库)     -- 实际开发中需要写在常量类中
    private static final String index_name_gmall = "gmall";
    //es type(表)
    private static final String type_name_gmall = "SkuLsInfo";
    @Autowired
    private JestClient jestClient;//es java客户端
    @Autowired
    private AttrMapper attrMapper;

    @Override
    public List<SkuLsInfo> search(SkuLsParam skuLsParam) { //SkuLsParam封装各种搜索方式
        List<SkuLsInfo> skuLsInfos = new ArrayList<>();
        //创建、构建 es语句                        查询语句        addIndex指定数据库                addType指定表
        Search search = new Search.Builder(getMyDsl(skuLsParam)).addIndex(index_name_gmall).addType(type_name_gmall).build();
        SearchResult execute = null;
        try {
            // 执行 es 语句
            execute = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 结果处理                                             Hits命中结果并指定结果类型
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = execute.getHits(SkuLsInfo.class);
        if (hits != null && hits.size()>0) {
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {  // 检索出来的数据
                SkuLsInfo source = hit.source; // 获取每一行数据

                // source(每一行数据) 和 highlight(高亮显示的数据) 是平级关系
                Map<String, List<String>> highlight = hit.highlight;  //获取高亮数据  -- map集合，可能有多个高亮字段
                // 获取高亮显示的数据，替换source中的数据
                if (highlight != null) {
                    List<String> skuName = highlight.get("skuName"); //获取高亮显示的skuName字段
                    if (StringUtils.isNotBlank(skuName.get(0))) {  //如果有值的话就使用高亮字段替换source中非高亮字段显示
                        source.setSkuName(skuName.get(0)); //替换成高亮字段
                    }
                }
                skuLsInfos.add(source);  // 将数据添加到集合中
            }
        }
        return skuLsInfos;
    }

    @Override
    public List<BaseAttrInfo> getAttrListByValueId(String join) {
        List<BaseAttrInfo> attrList = attrMapper.selectAttrListByValueId(join);
        return attrList;
    }


    // es查询语句
    public String getMyDsl(SkuLsParam skuLsParam){
        String keyword = skuLsParam.getKeyword(); // 输入框输入查询
        String catalog3Id = skuLsParam.getCatalog3Id(); // 通过三级Id 查询
        String[] valueId = skuLsParam.getValueId(); // 平台属性值id 查询
        //创建查询语句
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // bool查询(支持过滤、搜索等deng)   -- 属于 属性参数
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        // 三级过滤查询
        if(StringUtils.isNotBlank(catalog3Id)){                      //name es数据库表中字段
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",catalog3Id);
            boolQueryBuilder.filter(termQueryBuilder);
        }
        // 平台属性查询
        if(null!=valueId && valueId.length>0){
            for (int i=0;i<valueId.length;i++){
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId[i]);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        // 关键字搜索
        if(StringUtils.isNotBlank(keyword)){
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",keyword);
            boolQueryBuilder.must(matchQueryBuilder);
        }

        // 将属性参数放入查询
        searchSourceBuilder.query(boolQueryBuilder);
        // 分页查询
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(100);

        // 设置高亮[将字段高亮显示]   -- 根据关键词搜索，将搜索到的关键词高亮显示        【查询时设置高亮，查询结果将高亮实现的字段替换source中非高亮字段】
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        //前缀
        highlightBuilder.preTags("<span style='color:red;font-weight:800'>");
        highlightBuilder.field("skuName"); //设置需要高亮的字段
        //后缀
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);

        return searchSourceBuilder.toString();
    }
}
