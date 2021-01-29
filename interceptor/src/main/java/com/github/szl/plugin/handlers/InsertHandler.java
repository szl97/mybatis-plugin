package com.github.szl.plugin.handlers;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.github.szl.plugin.annotation.AutoSet;
import com.github.szl.plugin.constant.Constant;
import com.github.szl.plugin.enums.CommandType;
import com.github.szl.plugin.model.BoundSqlHelper;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/7 3:32 下午
 */
@NoArgsConstructor
@Component
public class InsertHandler extends Handler {

  @Override
  public Map<Field, AutoSet> getHandlerField(Object o) {
    return Arrays.stream(o.getClass().getDeclaredFields()).filter(field -> {
          if (!field.isAnnotationPresent(AutoSet.class)) return false;
          AutoSet autoSet = field.getAnnotation(AutoSet.class);
          return autoSet.command() == CommandType.ALL || autoSet.command() == CommandType.INSERT;
        }
    ).collect(Collectors.toMap(field->field, field -> field.getAnnotation(AutoSet.class)));
  }

  /**
   * 改变插入语句
   * @return
   */
  public BoundSqlHelper prepareAutoSetAndNewSql(MappedStatement mappedStatement, List<Object> entitySet, BoundSql boundSql) throws NoSuchFieldException, IllegalAccessException {
    String originSql = boundSql.getSql();
    MySqlStatementParser mySqlStatementParser = new MySqlStatementParser(originSql);
    SQLStatement statement = mySqlStatementParser.parseStatement();
    MySqlInsertStatement sqlInsertStatement = (MySqlInsertStatement) statement;
    SQLExprTableSource sqlTableSource = sqlInsertStatement.getTableSource();
    List<SQLExpr> columns = sqlInsertStatement.getColumns();
    String tableName = sqlTableSource.toString();

    StringBuilder newSqlColumnsSb = new StringBuilder();
    AtomicBoolean isAlreadyIncludeAllFields = new AtomicBoolean(true);
    AtomicInteger fieldCountInColumn = new AtomicInteger(0);
    Object firstEntity = entitySet.iterator().next();
    List<AutoSet> autoSetFields = getField(firstEntity);

    List<String> columnList = columns.stream().map(c->c.toString().toLowerCase()).collect(Collectors.toList());
    Set<String> columnSet = columnList.stream().collect(Collectors.toSet());

    StringBuilder fieldNameStr = new StringBuilder();
    autoSetFields.stream().forEach(
        field-> {
          if(columnSet.contains(field.name().toLowerCase()) || columnSet.contains("`"+field.name().toLowerCase()+"`")){
            fieldCountInColumn.addAndGet(1);
          }
          else{
            fieldNameStr.append(field.name()).append(",");
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
      for (Object o : entitySet) {
        handle(o);
      }
      return boundSqlHelper;
    }

    //构造出 insert into table （column1，column2，...）values
    StringBuilder newSqlPrefixSb = new StringBuilder();
    String[] sqls = boundSql.getSql().split(" ");
    for(String s : sqls){
      if(s.equals(tableName)){
        break;
      }
      newSqlPrefixSb.append(s+" ");
    }
    newSqlPrefixSb.append(tableName).append("(").append(newSqlColumnsSb).append(") ").append("values ");

    //构造占位符，形如(?,?,?)
    String newSqlValuePlaceholder = StringUtils.repeat("?,",columns.size() + autoSetFields.size() - fieldCountInColumn.get() - 1);
    newSqlValuePlaceholder = newSqlValuePlaceholder+"?";
    StringBuilder newSqlValuePlaceholderSb = new StringBuilder();
    newSqlValuePlaceholderSb.append("(").append(newSqlValuePlaceholder).append(")");
    StringBuilder newSqlSuffixSb = new StringBuilder();
    boolean isInsertBatch = isBatchInsert(boundSql);
    AtomicInteger index = new AtomicInteger(0);
    List<Map<AutoSet, String>> fieldList = new ArrayList<>();
    String insertBatchKeyPrefix = getBatchKeyPrefix(boundSql);
    for(Object o : entitySet){
      if (isInsertBatch) {
        fieldList.add(autoSetFields.stream().collect(Collectors.toMap(
            f->f, f -> insertBatchKeyPrefix + index.get()
                + Constant.INSERT_BATCH_KEY_SPILT
                + (StringUtils.isBlank(f.propertyName())?f.name():f.propertyName())
        )));
      }
      index.addAndGet(1);
      handle(o);
      newSqlSuffixSb.append(newSqlValuePlaceholderSb).append(",");
    }
    newSqlSuffixSb.delete(newSqlSuffixSb.length()-1,newSqlSuffixSb.length());

    StringBuilder duplicateKeyUpdateSql = new StringBuilder();
    if(sqlInsertStatement.getDuplicateKeyUpdate().size() != 0){
      duplicateKeyUpdateSql.append(" ON DUPLICATE KEY UPDATE ");
      sqlInsertStatement.getDuplicateKeyUpdate().stream().forEach(
          d->{
            duplicateKeyUpdateSql.append(d.toString()).append(",");
          }
      );
      duplicateKeyUpdateSql.delete(duplicateKeyUpdateSql.length()-1, duplicateKeyUpdateSql.length());
    }
    String newSql = newSqlPrefixSb.append(newSqlSuffixSb).append(duplicateKeyUpdateSql).toString();

    //sql辅助类设值
    BoundSqlHelper boundSqlHelper = new BoundSqlHelper();
    boundSqlHelper.setFields(autoSetFields);
    boundSqlHelper.setBoundSql(boundSql);
    boundSqlHelper.setSql(newSql);
    boundSqlHelper.setConfiguration(mappedStatement.getConfiguration());
    boundSqlHelper.setBatch(isInsertBatch);
    boundSqlHelper.setBatchFieldNameList(fieldList);
    return boundSqlHelper;
  }



}
