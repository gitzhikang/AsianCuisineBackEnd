package com.asiancuisine.asiancuisine.service;

import com.asiancuisine.asiancuisine.entity.User;

public interface IUserService {
    User queryByEmailAddress(String emailAddress);
    int addUser(User user);
}
