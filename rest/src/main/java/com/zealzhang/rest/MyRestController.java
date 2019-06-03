package com.zealzhang.rest;

import com.zealzhang.config.MyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Created by ao.zhang/ao.zhang@iluvatar.ai.<br/>
 * @version Version: 0.0.1
 * @date DateTime: 2019/06/03 16:24:00<br/>
 */
@RestController
public class MyRestController {
    @Value("${test}")
    private String test;
    @Autowired
    private MyConfig myConfig;

    @RequestMapping(value = "/config")
    public String getConfig(){
        return this.myConfig.getValue() + ":" + test;
    }
}
