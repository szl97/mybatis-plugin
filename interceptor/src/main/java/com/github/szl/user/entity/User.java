package com.github.szl.user.entity;

import com.github.szl.plugin.annotation.AutoSet;
import com.github.szl.plugin.enums.Code;
import com.github.szl.plugin.enums.CommandType;
import com.github.szl.plugin.enums.DbType;
import lombok.Data;


@Data
public class User {

    public User(){
        this.userName = "afscafcafasas";
        this.code = "asdas";}
    @AutoSet(code = Code.ID, command = CommandType.INSERT, name = "id", type = DbType.LONG)
    private Long id;

    private String userName;

    private String code;

    @AutoSet(code = Code.TIME_NOW_LONG, command = CommandType.INSERT, name = "create_time", type = DbType.LONG, propertyName = "createTime")
    private Long createTime;
    @AutoSet(code = Code.TIME_NOW_LONG, command = CommandType.ALL, name = "update_time", type = DbType.LONG, propertyName = "updateTime")
    private Long updateTime;

//    public Long getCreate_time(){
//        return getCreateTime();
//    }
//    public Long getUpdate_time(){
//        return getUpdateTime();
//    }
}