package com.github.szl.plugin;

import com.github.szl.plugin.annotation.AutoSet;
import com.github.szl.plugin.constant.Constant;
import com.github.szl.plugin.handlers.InsertHandler;
import com.github.szl.plugin.handlers.UpdateHandler;
import com.github.szl.plugin.model.BoundSqlHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 插入或修改数据库时跟自动set字段值，支持批量插入和修改
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/7 12:11 下午
 */
@Slf4j
@Intercepts(value={@Signature(type = Executor.class,method = "update",args = {MappedStatement.class,Object.class})})
@Component
public class AutoSetInterceptor implements Interceptor{
  @Autowired
  private InsertHandler inserter;

  @Autowired
  private UpdateHandler updater;

  @PostConstruct
  public void init(){
    log.info("+++++++++++++++++++++拦截器启动+++++++++++++++++++++++");
  }
  @Override
  public Object intercept(Invocation invocation) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException, SQLException {
    log.info("+++++++++++++++++++++进入拦截器+++++++++++++++++++++++");
    Object[] args = invocation.getArgs();
    //args数组对应对象就是上面@Signature注解中args对应的对应类型
    MappedStatement mappedStatement = (MappedStatement) args[0];
    //实体对象
    Object entity = args[1];
    BoundSql sql = mappedStatement.getBoundSql(entity);
    SqlCommandType commandType = mappedStatement.getSqlCommandType();
    if("insert".equalsIgnoreCase(commandType.name())){
      List<Object> list = getEntityList(entity);
      BoundSqlHelper boundSqlHelper = inserter.prepareAutoSetAndNewSql(mappedStatement, list, sql);
      if(!boundSqlHelper.isAlreadyIncludeAllFields()){
        ResetSqlUtil.resetSql2Invocation(invocation, boundSqlHelper,new InsertBoundSqlSqlSource(boundSqlHelper));
      }
      //直接写参数，而不是传入实体类的update方法不做处理，@autoSet不支持parameter
    } else if("update".equalsIgnoreCase(commandType.name()) && !(entity instanceof Map)){
      List<Object> list = new ArrayList<>();
      list.add(entity);
      BoundSqlHelper boundSqlHelper = updater.prepareAutoSetAndNewSql(mappedStatement, list, sql);
      if(!boundSqlHelper.isAlreadyIncludeAllFields()){
        ResetSqlUtil.resetSql2Invocation(invocation, boundSqlHelper, new UpdateBoundSqlSqlSource(boundSqlHelper));
      }
    }
    return invocation.proceed();
  }
  /**
   * entity是需要插入的实体数据,它可能是对象,也可能是批量插入的对象。
   * 如果是单个对象,那么entity就是当前对象
   * 如果是批量插入对象，那么entity就是一个map集合,key值为"list",value为ArrayList集合对象
   */
  private List<Object> getEntityList(Object entity) {
    List<Object> set = new ArrayList<>();
    if (entity instanceof Map) {
      //批量插入对象
      Collection values = (Collection) ((Map) entity).get("list");
      values.stream().forEach(value->{
        if (value instanceof Collection) {
          set.addAll((Collection) value);
        } else {
          set.add(value);
        }
      });
    } else {
      //单个插入对象
      set.add(entity);
    }
    return set;
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {

  }

  class InsertBoundSqlSqlSource implements SqlSource {
    private BoundSqlHelper boundSqlHelper;

    public InsertBoundSqlSqlSource(BoundSqlHelper boundSqlHelper) {
      this.boundSqlHelper = boundSqlHelper;
    }
    @Override
    public BoundSql getBoundSql(Object parameterObject) {
      boolean isAlreadyIncludeAllFields = boundSqlHelper.isAlreadyIncludeAllFields();
      if(!isAlreadyIncludeAllFields){
        if(boundSqlHelper.isBatch()){
          setFieldParameterMappings();
        }else{
          setFieldParameterMapping();
        }
      }
      return boundSqlHelper.getBoundSql();
    }

    /**
     * 设置批量插入的parameterMappings，并对parameterMappings进行按key的索引下标排序。
     * 如果不进行排序，则追加到最后
     */
    private void setFieldParameterMappings() {
      List<Map<AutoSet, String>> fieldNameList = boundSqlHelper.getBatchFieldNameList();
      fieldNameList.stream().forEach(
          fieldMap->{
            setFieldParameterMapping(fieldMap);
          }
      );
      sortParameterMappings();
    }

    private void sortParameterMappings(){
      Collections.sort(boundSqlHelper.getBoundSql().getParameterMappings(), (o1, o2) -> {
        int index1 = getInsertBatchKeyIndex(o1.getProperty());
        int index2 = getInsertBatchKeyIndex(o2.getProperty());
        return index1 - index2;
      });
    }

    private void setFieldParameterMapping() {
      setFieldParameterMapping(null);
    }

    private void setFieldParameterMapping(Map<AutoSet, String> fieldNameMap) {
      Set<String> set = boundSqlHelper.getBoundSql().getParameterMappings().stream().map(p->p.getProperty()).collect(Collectors.toSet());
      boundSqlHelper.getFields().stream().forEach(
          field->{
            String name = fieldNameMap==null ? StringUtils.isBlank(field.propertyName()) ? field.name() : field.propertyName() : fieldNameMap.get(field);
            if(!set.contains(name)) {
              ParameterMapping parameterMapping = new ParameterMapping
                  .Builder(
                  boundSqlHelper.getConfiguration(),
                  name,
                  ResetSqlUtil.getTargetTypeHandler(field.type())
              ).build();
              boundSqlHelper.getBoundSql().getParameterMappings().add(parameterMapping);
            }
          }
      );
    }

    /**
     * 获取批量插入前缀索引，形如：__frch_user_0.userName其索引就为0
     * @param prop
     * @return
     */
    private int getInsertBatchKeyIndex(String prop){
      String prefix = StringUtils.substring(prop,0, StringUtils.lastIndexOf(prop, Constant.INSERT_BATCH_KEY_SPILT));
      String insertBatchKeyIndex = StringUtils.substring(prefix,prefix.lastIndexOf(Constant.BATCH_INSERT_SEQ_SPLIT)+1,prefix.length());
      return Integer.valueOf(insertBatchKeyIndex);
    }
  }

  class UpdateBoundSqlSqlSource implements SqlSource {
    private BoundSqlHelper boundSqlHelper;

    public UpdateBoundSqlSqlSource(BoundSqlHelper boundSqlHelper) {
      this.boundSqlHelper = boundSqlHelper;
    }
    @Override
    public BoundSql getBoundSql(Object parameterObject) {
      boolean isAlreadyIncludeAllFields = boundSqlHelper.isAlreadyIncludeAllFields();
      if(!isAlreadyIncludeAllFields){
        setFieldParameterMapping();
      }
      return boundSqlHelper.getBoundSql();
    }

    private void setFieldParameterMapping() {
      Set<String> set = boundSqlHelper.getBoundSql().getParameterMappings().stream().map(p->p.getProperty()).collect(Collectors.toSet());
      boundSqlHelper.getFields().stream().forEach(
          field->{
            String name = StringUtils.isBlank(field.propertyName()) ? field.name() : field.propertyName();
            if(!set.contains(name)) {
              ParameterMapping parameterMapping = new ParameterMapping
                  .Builder(
                  boundSqlHelper.getConfiguration(),
                  name,
                  ResetSqlUtil.getTargetTypeHandler(field.type())
              ).build();
              List<ParameterMapping> list = boundSqlHelper.getBoundSql().getParameterMappings();
              if (boundSqlHelper.getWhereCount() > 0) {
                int index = list.size() - boundSqlHelper.getWhereCount();
                list.add(index, parameterMapping);
              } else {
                list.add(parameterMapping);
              }
            }
          }
      );
    }
  }
}
