package com.github.szl.plugin.setter;


import com.github.szl.plugin.enums.Code;

import java.lang.reflect.Field;

/**
 * @description: 每增加一种需要AutoSet的类型只需要增加一个Setter的实现类，并添加对应的Code，通过Code选择Setter
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/8 2:21 下午
 */
public abstract class Setter {
  public abstract Code code();
  public boolean accept(Code code){
    return code == code();
  }
  public abstract void set(Field field, Object o) throws IllegalAccessException;
}
