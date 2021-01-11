package com.github.szl.plugin.setter;

import com.github.szl.plugin.enums.Code;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/8 6:03 下午
 */
@Component
public class IdSetter extends Setter{
  @Override
  public Code code() {
    return Code.ID;
  }

  @Override
  public void set(Field field, Object o) throws IllegalAccessException {
    field.setAccessible(true);
    //field.set(o,1610098902264l);
    field.set(o, LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - new Random().nextInt(999999));
  }
}
