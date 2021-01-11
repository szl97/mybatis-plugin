package com.github.szl.plugin;


import com.github.szl.plugin.enums.Code;
import com.github.szl.plugin.setter.Setter;
import lombok.experimental.UtilityClass;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/7 12:53 下午
 */
@UtilityClass
public class SetterUtils {
  //记录每个类所需的处理器，不需要每次都查找需要哪些处理器
  public static ConcurrentHashMap<Code, Setter> setterMap = new ConcurrentHashMap<>();

  private static Object lock = new Object();

  public Setter getSetters(Code code, Function<Code, Setter> function){
    if(!setterMap.containsKey(code)){
      synchronized (lock){
        if(!setterMap.containsKey(code)){
          setterMap.put(code, function.apply(code));
        }
      }
    }
    return setterMap.get(code);
  }
}
