package com.github.szl.plugin.setter;


import com.github.szl.plugin.enums.Code;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/8 2:21 下午
 */
@Component
public class TimeNowLongSetter extends Setter {
  @Override
  public Code code() {
    return Code.TIME_NOW_LONG;
  }
  @Override
  public void set(Field field, Object o) throws IllegalAccessException {
    field.setAccessible(true);
    field.set(o, LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
  }
}
