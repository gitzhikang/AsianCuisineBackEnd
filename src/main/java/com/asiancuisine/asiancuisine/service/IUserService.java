package com.asiancuisine.asiancuisine.service;

import com.asiancuisine.asiancuisine.entity.User;

public interface IUserService {
    User queryById(Long id);
    int addUser(User user);
}
