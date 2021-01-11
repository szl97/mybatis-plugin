package com.github.szl.plugin.annotation;



import com.github.szl.plugin.enums.Code;
import com.github.szl.plugin.enums.CommandType;
import com.github.szl.plugin.enums.DbType;

import java.lang.annotation.*;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/7 12:02 下午
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AutoSet {
  //属性对应table中的column
  String name();
  //和Setter对应，每个Setter拥有一个code，根据code去判断如何set
  Code code();
  //all, insert, update
  CommandType command() default CommandType.ALL;
  //该属性在数据库中的数据类型
  DbType type();
  //实体类中的属性名称，mybatis执行sql会调用对应的get方法去获取值
  String propertyName() default "";
}
