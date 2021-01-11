package com.github.szl.plugin;


import com.github.szl.plugin.enums.DbType;
import com.github.szl.plugin.model.BoundSqlHelper;
import lombok.experimental.UtilityClass;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.type.*;

import java.sql.SQLException;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/8 1:57 下午
 */
@UtilityClass
public class ResetSqlUtil {
  /**
   * 重置sql
   * @param invocation
   * @param boundSqlHelper
   * @param sqlSource
   * @throws SQLException
   */
  public void resetSql2Invocation(Invocation invocation, BoundSqlHelper boundSqlHelper, SqlSource sqlSource) throws SQLException {
    final Object[] args = invocation.getArgs();
    MappedStatement statement = (MappedStatement) args[0];
    MappedStatement newStatement = newMappedStatement(statement, sqlSource);
    MetaObject msObject =  MetaObject.forObject(newStatement, new DefaultObjectFactory(), new DefaultObjectWrapperFactory(),new DefaultReflectorFactory());
    msObject.setValue("sqlSource.boundSqlHelper.boundSql.sql", boundSqlHelper.getSql());
    args[0] = newStatement;
  }

  private MappedStatement newMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
    MappedStatement.Builder builder =
        new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
    builder.resource(ms.getResource());
    builder.fetchSize(ms.getFetchSize());
    builder.statementType(ms.getStatementType());
    builder.keyGenerator(ms.getKeyGenerator());
    if (ms.getKeyProperties() != null && ms.getKeyProperties().length != 0) {
      StringBuilder keyProperties = new StringBuilder();
      for (String keyProperty : ms.getKeyProperties()) {
        keyProperties.append(keyProperty).append(",");
      }
      keyProperties.delete(keyProperties.length() - 1, keyProperties.length());
      builder.keyProperty(keyProperties.toString());
    }
    builder.timeout(ms.getTimeout());
    builder.parameterMap(ms.getParameterMap());
    builder.resultMaps(ms.getResultMaps());
    builder.resultSetType(ms.getResultSetType());
    builder.cache(ms.getCache());
    builder.flushCacheRequired(ms.isFlushCacheRequired());
    builder.useCache(ms.isUseCache());

    return builder.build();
  }

  public TypeHandler getTargetTypeHandler(DbType dbType){
    switch (dbType){
      case INT:
        return new IntegerTypeHandler();
      case BYTE:
        return new ByteArrayTypeHandler();
      case BLOB_BYTE:
        return new BlobByteObjectArrayTypeHandler();
      case LONG:
        return new LongTypeHandler();
      case SHORT:
        return new ShortTypeHandler();
      case DOUBLE:
        return new DoubleTypeHandler();
      case FLOAT:
        return new FloatTypeHandler();
      case BOOLEAN:
        return new BooleanTypeHandler();
      case STRING:
        return new StringTypeHandler();
      case ARRAY:
        return new ArrayTypeHandler();
      case DATE:
        return new DateOnlyTypeHandler();
      case DATE_TIME:
        return new ZonedDateTimeTypeHandler();
      case LOCAL_DATE:
        return new LocalDateTypeHandler();
      case LOCAL_DATE_TIME:
        return new LocalDateTimeTypeHandler();
      case TIME_ONLY:
        return new TimeOnlyTypeHandler();
      case YEAR_MONTH:
        return new YearMonthTypeHandler();
      case YEAR:
        return new YearTypeHandler();
      default:
        return new UnknownTypeHandler(new TypeHandlerRegistry());
    }
  }
}
