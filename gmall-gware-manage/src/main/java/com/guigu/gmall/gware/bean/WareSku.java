package com.guigu.gmall.gware.bean;

import javax.persistence.*;

// sku和库存的中间表    多对多的关系
public class WareSku {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    @Id
    private  String id ;

    @Column
    private String skuId;

    @Column
    private String warehouseId;//所属仓库编号

    @Column
    private Integer stock=0; // 库存 -- 还剩多少

    @Column
    private  String stockName; // 库存名

    @Column
    private Integer stockLocked; // 库存锁定  -- 被别人买了，等待发货

    @Transient
    private  String warehouseName;//所属仓库名称

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public Integer getStockLocked() {
        return stockLocked;
    }

    public void setStockLocked(Integer stockLocked) {
        this.stockLocked = stockLocked;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }
}
