package com.asiancuisine.asiancuisine.mapper;

import com.asiancuisine.asiancuisine.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserMapper {
    public User queryById(Long id);
    public int addUser(User user);
}
