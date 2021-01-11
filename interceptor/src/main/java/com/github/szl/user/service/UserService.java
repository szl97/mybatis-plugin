package com.github.szl.user.service;

import com.github.szl.user.entity.User;

import java.util.List;

public interface UserService {

    boolean save(User record);

    boolean saveBatch(List<User> records);
}
