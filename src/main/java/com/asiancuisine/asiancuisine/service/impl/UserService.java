package com.asiancuisine.asiancuisine.service.impl;

import com.asiancuisine.asiancuisine.entity.User;
import com.asiancuisine.asiancuisine.mapper.IUserMapper;
import com.asiancuisine.asiancuisine.service.IUserService;
import com.asiancuisine.asiancuisine.util.AwsS3Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class UserService implements IUserService {

    @Autowired
    private IUserMapper userMapper;

    @Autowired
    AwsS3Util awsS3Util;

    @Value("${ac.aws.s3.bucketName}")
    private String bucketName;

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

    @Override
    public void updateNickname(Long id, String nickName) {
        userMapper.updateNickname(id, nickName);
    }

    @Override
    public void updateUserProfile(Long id, String icon, String nickName, String motto) {
        userMapper.updateUserProfile(id, icon, nickName, motto);
    }

    @Override
    public String uploadIconToAWS(MultipartFile file) throws IOException {
        return awsS3Util.uploadFile(file,bucketName);
    }

    @Override
    public void updatePassword(Long id, String password) {
        userMapper.updatePassword(id, password);
    }
}
