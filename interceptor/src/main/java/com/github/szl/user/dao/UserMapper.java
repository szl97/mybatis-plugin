package com.github.szl.user.dao;

import com.github.szl.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    int deleteByPrimaryKey(Long id);

//    @Insert("insert into t_user (user_name) values (" +
//        "#{userName})")
    int insert(User record);

    int insertSelective(User record);

    int insertBatch(List<User> records);

    User selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);
}