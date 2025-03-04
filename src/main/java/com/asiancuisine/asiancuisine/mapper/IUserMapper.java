package com.asiancuisine.asiancuisine.mapper;

import com.asiancuisine.asiancuisine.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface IUserMapper {
    User queryById(Long id);
    User queryByEmailAddress(String emailAddress);
    int addUser(User user);
    String queryPasswordByEmailAddress(String emailAddress);
    Long queryIdByEmailAddress(String emailAddress);
    void updateNickname(@Param("id") Long id, @Param("nickName") String nickName);
    void updateUserProfile(@Param("id") Long id, @Param("icon") String icon, @Param("nickName") String nickName, @Param("motto") String motto);
    void updatePassword(@Param("id") Long id, @Param("password") String password);
}