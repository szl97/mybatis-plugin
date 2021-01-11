package com.github.szl;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = {"com.github.szl.**.dao"})
public class InterceptorApplication {

  public static void main(String[] args) {
    SpringApplication.run(InterceptorApplication.class, args);
  }

}
