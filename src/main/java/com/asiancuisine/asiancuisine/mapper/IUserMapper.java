package com.asiancuisine.asiancuisine.mapper;

import com.asiancuisine.asiancuisine.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserMapper {
    User queryByEmailAddress(String emailAddress);
    int addUser(User user);
    String queryPasswordByEmailAddress(String emailAddress);
    Long queryIdByEmailAddress(String emailAddress);
    void updateNickName(Long userId, String nickName);
}