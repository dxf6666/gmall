package com.guigu.gmall.gware.bean;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

// 仓库信息
public class WareInfo {

    @Column
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private String id;

    @Column
    private String name; // 仓库名称

    @Column
    private String address; //仓库地址

    @Column
    private String areacode;// 仓库编号


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAreacode() {
        return areacode;
    }

    public void setAreacode(String areacode) {
        this.areacode = areacode;
    }
}
