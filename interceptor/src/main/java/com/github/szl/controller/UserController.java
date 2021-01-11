package com.github.szl.controller;

import com.github.szl.user.dao.UserMapper;
import com.github.szl.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2021/1/8 4:06 下午
 */
@RestController
@RequestMapping("/user")
public class UserController {
  @Autowired
  UserMapper userMapper;
  @PostMapping
  @ResponseBody
  public Boolean insert(){
    List<User> list = new ArrayList<>();
    for(int i = 0; i < 10; i++){
      list.add(new User());
    }
    userMapper.insertBatch(list);
    //userMapper.insert(new User());
    return true;
  }
  @PutMapping("{id}")
  public Boolean update(@PathVariable Long id){
    User user = new User();
    user.setId(id);
    user.setCode("修改");
    userMapper.updateByPrimaryKey(user);
    return true;
  }
}
