package com.asiancuisine.asiancuisine.service;

import com.asiancuisine.asiancuisine.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IUserService {
    User queryByEmailAddress(String emailAddress);
    int addUser(User user);
    String queryPasswordByEmailAddress(String emailAddress);
    Long queryIdByEmailAddress(String emailAddress);
    void updateNickname(Long id, String nickname);
    void updateUserProfile(Long id, String icon, String nickName, String motto);
    String uploadIconToAWS(MultipartFile file) throws IOException;
    void updatePassword(Long id, String password);
}
