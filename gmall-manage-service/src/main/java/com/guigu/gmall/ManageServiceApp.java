package com.guigu.gmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
//扫描Mapper接口，因为是通用mybatis，所以导【tk.mybatis】包
@MapperScan(basePackages = "com.guigu.gmall.mapper")
public class ManageServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(ManageServiceApp.class, args);
    }
}
