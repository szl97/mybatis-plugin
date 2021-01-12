package com.github.szl.plugin.handlers;


import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.github.szl.plugin.annotation.AutoSet;
import com.github.szl.plugin.enums.CommandType;
import com.github.szl.plugin.model.BoundSqlHelper;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/7 4:00 下午
 */
@NoArgsConstructor
@Component
public class UpdateHandler extends Handler {

  @Override
  public BoundSqlHelper prepareAutoSetAndNewSql(MappedStatement mappedStatement, List<Object> entitySet, BoundSql boundSql) throws NoSuchFieldException, IllegalAccessException {
    MySqlStatementParser mySqlStatementParser = new MySqlStatementParser(boundSql.getSql());
    SQLStatement statement = mySqlStatementParser.parseStatement();
    MySqlUpdateStatement updateStatement = (MySqlUpdateStatement) statement;
    List<SQLUpdateSetItem> columns = updateStatement.getItems();
    SQLExpr where = updateStatement.getWhere();
    SQLTableSource sqlTableSource = updateStatement.getTableSource();
    String tableName = sqlTableSource.toString();
    StringBuilder newSqlColumnsSb = new StringBuilder();
    Object firstEntity = entitySet.iterator().next();
    handle(firstEntity);
    List<AutoSet> autoSetFields = getField(firstEntity);
    List<String> columnList = columns.stream().map(c->c.toString().replace(" ","").toLowerCase()).collect(Collectors.toList());
    Set<String> columnSet = columnList.stream().collect(Collectors.toSet());
    AtomicBoolean isAlreadyIncludeAllFields = new AtomicBoolean(true);
    AtomicInteger fieldCountInColumn = new AtomicInteger(0);
    StringBuilder fieldNameStr = new StringBuilder();
    autoSetFields.stream().forEach(
        field-> {
          if(columnSet.contains(field.name().toLowerCase()+"=?")){
            fieldCountInColumn.addAndGet(1);
          }
          else{
            fieldNameStr.append(field.name()).append("=?,");
            isAlreadyIncludeAllFields.set(false);
          }
        }
    );
    if(fieldNameStr.length()>0){
      fieldNameStr.delete(fieldNameStr.length()-1,fieldNameStr.length());
    }
    columnList.stream().forEach(c->newSqlColumnsSb.append(c).append(","));
    newSqlColumnsSb.append(fieldNameStr);
    //如果所有属性都已经存在，则无需向下执行构造新sql，沿用旧sql，并填充属性值即可
    if(isAlreadyIncludeAllFields.get()){
      BoundSqlHelper boundSqlHelper = new BoundSqlHelper();
      boundSqlHelper.setAlreadyIncludeAllFields(isAlreadyIncludeAllFields.get());
      return boundSqlHelper;
    }
    StringBuilder newSql = new StringBuilder();
    newSql.append("update ").append(tableName).append(" set ").append(newSqlColumnsSb);
    if(StringUtils.isNotBlank(where.toString())){
      newSql.append(" where ").append(where.toString());
    }
    BoundSqlHelper boundSqlHelper = new BoundSqlHelper();
    boundSqlHelper.setFields(autoSetFields);
    boundSqlHelper.setBoundSql(boundSql);
    boundSqlHelper.setSql(newSql.toString());
    boundSqlHelper.setConfiguration(mappedStatement.getConfiguration());
    boundSqlHelper.setBatch(false);
    if(StringUtils.isNotBlank(where.toString())) {
      boundSqlHelper.setWhereCount(getWhereConditionCount(where.toString()));
    }
    return boundSqlHelper;
  }

  @Override
  public Map<Field, AutoSet> getHandlerField(Object o) {
    return Arrays.stream(o.getClass().getDeclaredFields()).filter(field -> {
          if(!field.isAnnotationPresent(AutoSet.class)) return false;
          AutoSet autoSet = field.getAnnotation(AutoSet.class);
          return autoSet.command()== CommandType.ALL || autoSet.command() == CommandType.UPDATE;
        }
    ).collect(Collectors.toMap(field->field, field -> field.getAnnotation(AutoSet.class)));
  }


}
