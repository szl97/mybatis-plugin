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
  String name();
  Code code();
  CommandType command() default CommandType.ALL;
  DbType type();
  String propertyName() default "";
}
