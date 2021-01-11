package com.github.szl.plugin.model;


import com.github.szl.plugin.annotation.AutoSet;
import lombok.Data;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;

import java.util.List;
import java.util.Map;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/7 7:08 下午
 */
@Data
public class BoundSqlHelper {
  private BoundSql boundSql;

  private String sql;

  private List<AutoSet> fields;

  private Configuration configuration;

  private boolean isAlreadyIncludeAllFields;

  //是否批量操作
  private boolean isBatch;

  private List<Map<AutoSet, String>> batchFieldNameList;

  private int whereCount;
}
