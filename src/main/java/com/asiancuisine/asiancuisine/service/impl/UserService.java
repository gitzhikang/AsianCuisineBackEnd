package com.asiancuisine.asiancuisine.service.impl;

import com.asiancuisine.asiancuisine.entity.User;
import com.asiancuisine.asiancuisine.mapper.IUserMapper;
import com.asiancuisine.asiancuisine.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserService implements IUserService {

    @Autowired
    private IUserMapper userMapper;

    @Override
    public User queryByEmailAddress(String emailAddress) {
        return userMapper.queryByEmailAddress(emailAddress);
    }

    @Override
    public int addUser(User user) { return userMapper.addUser(user); }

    @Override
    public String queryPasswordByEmailAddress(String emailAddress) {
        return userMapper.queryPasswordByEmailAddress(emailAddress);
    }

    @Override
    public Long queryIdByEmailAddress(String emailAddress) {
        return userMapper.queryIdByEmailAddress(emailAddress);
    }
}
