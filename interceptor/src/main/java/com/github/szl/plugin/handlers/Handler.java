package com.github.szl.plugin.handlers;

import com.github.szl.plugin.SetterUtils;
import com.github.szl.plugin.annotation.AutoSet;
import com.github.szl.plugin.constant.Constant;
import com.github.szl.plugin.enums.Code;
import com.github.szl.plugin.model.BoundSqlHelper;
import com.github.szl.plugin.setter.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/7 12:20 下午
 */
public abstract class Handler {
  @Autowired
  private List<Setter> setters;
  //记录每个类要去处理哪些属性，不需要每次处理的时候都进行反射查询要处理的属性
  private ConcurrentHashMap<Class, Map<Field, AutoSet>> fieldMap = new ConcurrentHashMap<>();


  private void process(Object o, Map<Field, AutoSet> map) throws IllegalAccessException, NoSuchFieldException {
    for (Map.Entry<Field, AutoSet> entry : map.entrySet()) {
      Setter setter = SetterUtils.getSetters(entry.getValue().code(), code -> getAcceptableSetter(code));
      setter.set(entry.getKey(), o);
    }
  }

  protected int getWhereConditionCount(String where){
    String[] array = where.toUpperCase().replace("\n"," ").split(" ");
    return (int) Arrays.stream(array).filter(a->a.equals(Constant.WHERE_CONDITION_AND)||a.equals(Constant.WHERE_CONDITION_OR)).count() + 1;
  }

  public abstract BoundSqlHelper prepareAutoSetAndNewSql(MappedStatement mappedStatement, List<Object> entitySet, BoundSql boundSql) throws NoSuchFieldException, IllegalAccessException;

  protected abstract Map<Field, AutoSet> getHandlerField(Object o);

  protected void handle(Object object) throws NoSuchFieldException, IllegalAccessException {
    process(object, getFieldMap(object));
  }

  private Map<Field, AutoSet> getFieldMap(Object o) {
    if (!fieldMap.containsKey(o.getClass())) {
      synchronized (this) {
        if (!fieldMap.containsKey(o.getClass())) {
          fieldMap.put(o.getClass(), getHandlerField(o));
        }
      }
    }
    return fieldMap.get(o.getClass());
  }

  protected List<AutoSet> getField(Object o) {
    return getFieldMap(o).values().stream().collect(Collectors.toList());
  }


  /**
   * 判断是否是批量插入，如果是批量插入，其ParameterMapping的key形如：__frch_item_0.userName
   *
   * @param boundSql
   * @return
   */
  protected boolean isBatchInsert(BoundSql boundSql) {
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if (!CollectionUtils.isEmpty(parameterMappings)) {
      ParameterMapping parameterMapping = parameterMappings.get(0);
      String prop = parameterMapping.getProperty();
      return prop.contains(Constant.INSERT_BATCH_KEY_SPILT);
    }
    return false;
  }

  /**
   * 获取批量插入key前缀，比如批量插入的key为__frch_user_0.userName
   *
   * @return 返回__frch_user_
   */
  protected String getBatchKeyPrefix(BoundSql boundSql) {
    boolean isInsertBatch = isBatchInsert(boundSql);
    if (!isInsertBatch) {
      return null;
    }
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if (!CollectionUtils.isEmpty(parameterMappings)) {
      ParameterMapping parameterMapping = parameterMappings.get(0);
      String prop = parameterMapping.getProperty();
      String prefix = StringUtils.substring(prop, 0, StringUtils.lastIndexOf(prop, Constant.INSERT_BATCH_KEY_SPILT));
      String insertBatchKeyPrefix = StringUtils.substring(prefix, 0, prefix.length() - 1);
      return insertBatchKeyPrefix;
    }
    return null;
  }

  protected Setter getAcceptableSetter(Code code) {
    return setters.stream().filter(setter -> setter.accept(code)).findAny().get();
  }
}
