package cn.yiidii.openapi;

import cn.yiidii.pigeon.common.security.annotation.EnablePigeonResourceServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Openapi启动类
 *
 * @author YiiDii Wang
 * @date 2021/6/1 9:44:38
 */
@SpringBootApplication
@ComponentScan(basePackages = "cn.yiidii")
@EnableScheduling
@EnablePigeonResourceServer
public class OpenapiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenapiApplication.class, args);
    }

}
