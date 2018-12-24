package com.guigu.gmall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages ="com.guigu.gmall.gware.mapper")
public class GmallGwareManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallGwareManageApplication.class, args);
    }

}

