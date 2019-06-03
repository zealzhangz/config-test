package com.zealzhang.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author Created by ao.zhang/ao.zhang@iluvatar.ai.<br/>
 * @version Version: 0.0.1
 * @date DateTime: 2019/06/03 16:21:00<br/>
 */
@SpringBootApplication
@ComponentScan(value="com.zealzhang")
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
