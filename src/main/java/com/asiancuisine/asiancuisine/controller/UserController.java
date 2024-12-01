package com.asiancuisine.asiancuisine.controller;

import com.asiancuisine.asiancuisine.Result.Result;
import com.asiancuisine.asiancuisine.constant.JwtClaimsKeyConstant;
import com.asiancuisine.asiancuisine.constant.RedisConstants;
import com.asiancuisine.asiancuisine.entity.User;
import com.asiancuisine.asiancuisine.service.IUserService;
import com.asiancuisine.asiancuisine.util.GmailUtil;
import com.asiancuisine.asiancuisine.util.JwtUtil;
import com.asiancuisine.asiancuisine.util.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.security.SecureRandom;

@Api(tags = "User Api")
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @Value("${jwt.user-secret-key}")
    private String userSecretKey;

    @Value("${jwt.user-ttl-remembered}")
    private long userTtlRemembered;

    @Value("${jwt.user-ttl-not-remembered}")
    private long userTtlNotRemembered;

    @Value("${jwt.reset-password-secret-key}")
    private String resetPasswordSecretKey;

    @Value("${jwt.reset-password-ttl}")
    private long resetPasswordTtl;

    @Autowired
    RedisUtil redisUtil;

    @ApiOperation("Get User Info by Email Address")
    @GetMapping("/{emailAddress}")
    public ResponseEntity<Result<User>> findByEmailAddress(
            @ApiParam(value = "user email address", required = true, example = "example@outlook.com") @PathVariable String emailAddress) {
        User user = userService.queryByEmailAddress(emailAddress);
        if (user != null) {
            return new ResponseEntity<>(Result.success(user), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(Result.error("email address not found"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Add User Info")
    @PostMapping("/adduser")
    public ResponseEntity<Result<String>> addUser(@RequestBody User user) {
        try {
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String hashedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(hashedPassword);

            int result = userService.addUser(user);
            Long id = userService.queryIdByEmailAddress(user.getEmailAddress());
            userService.updateNickname(id, "Jobless User #" + String.format("%04d", id));

            if (result > 0) {
                return new ResponseEntity<>(Result.success(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(Result.error("register user failed, please retry!"), HttpStatus.BAD_REQUEST);
            }
        } catch (DuplicateKeyException e) {
            return new ResponseEntity<>(Result.error("email address already exists"), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(Result.error("register user failed, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Login")
    @PostMapping("/login")
    public ResponseEntity<Result<?>> login(@RequestBody Map<String, String> loginRequest) {
        String emailAddress = loginRequest.get("emailAddress");
        String password = loginRequest.get("password");
        String isRemember = loginRequest.get("isRemembered");
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        String dbPassword = userService.queryPasswordByEmailAddress(emailAddress);
        if (dbPassword != null && passwordEncoder.matches(password, dbPassword)) {
            User currentUser = userService.queryByEmailAddress(emailAddress);
            currentUser.setPassword("");
            Map<String, Object> claims = new HashMap<>();
            claims.put(JwtClaimsKeyConstant.USER_ID, currentUser.getId());
            claims.put(JwtClaimsKeyConstant.EMAIL_ADDRESS, emailAddress);
            String jwtToken;
            if (isRemember.equals("true")) {
                jwtToken = JwtUtil.createJWT(userSecretKey, userTtlRemembered, claims);
            } else {
                jwtToken = JwtUtil.createJWT(userSecretKey, userTtlNotRemembered, claims);
            }

            Map<String, Object> responses = new HashMap<>();
            responses.put("current_user", currentUser);
            responses.put("token", jwtToken);
            return new ResponseEntity<>(Result.success(responses), HttpStatus.OK);
        } else if (dbPassword == null) {
            return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
        } else {
            return new ResponseEntity<>(Result.error("login failed, password is wrong"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Update User Profile")
    @PostMapping("/updateUserProfile")
    public ResponseEntity<Result<?>> updateUserProfile(@RequestParam("iconImage") MultipartFile iconImage, @RequestParam("emailAddress") String emailAddress, @RequestParam("nickName") String nickName, @RequestParam("motto") String motto) {
        try {
            String iconUri = userService.uploadIconToAWS(iconImage);

            User currentUser = userService.queryByEmailAddress(emailAddress);
            if (currentUser == null) {
                return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
            }
            userService.updateUserProfile(currentUser.getId(), iconUri, nickName, motto);

            // delete cache in redis
            redisUtil.deleteUserInfoPreview(currentUser.getId());

            // get the updated user
            User updatedUser = userService.queryByEmailAddress(emailAddress);
            Map<String, Object> responses = new HashMap<>();
            responses.put("updated_user", updatedUser);
            return new ResponseEntity<>(Result.success(responses), HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>(Result.error("image cannot be uploaded to the remote server, please try again!"), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>(Result.error("update user profile failed, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("update password")
    @PostMapping("/updatePassword")
    public ResponseEntity<Result<?>> updatePassword(@RequestParam("emailAddress") String emailAddress, @RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword) {
        try {
            User currentUser = userService.queryByEmailAddress(emailAddress);
            if (currentUser == null) {
                return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
            }
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            if (!passwordEncoder.matches(oldPassword, currentUser.getPassword())) {
                return new ResponseEntity<>(Result.error("old password does not match"), HttpStatus.BAD_REQUEST);
            }
            String hashedPassword = passwordEncoder.encode(newPassword);
            userService.updatePassword(currentUser.getId(), hashedPassword);
            return new ResponseEntity<>(Result.success(), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(Result.error("update password failed, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Start Resetting password")
    @PostMapping("/resetPasswordEntry")
    public ResponseEntity<Result<?>> resetPasswordEntry(@RequestParam("emailAddress") String emailAddress) {
        try {
            User currentUser = userService.queryByEmailAddress(emailAddress);
            if (currentUser == null) {
                return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
            }
            // generate secure random verification code
            SecureRandom secureRandom = new SecureRandom();
            int verificationCode = secureRandom.nextInt(900000) + 100000;
            // save to redis
            redisUtil.saveVerificationCode(currentUser.getId(), String.valueOf(verificationCode));

            // write and send email
            GmailUtil.sendVerificationEmail(currentUser.getEmailAddress(), verificationCode);

            return new ResponseEntity<>(Result.success(), HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(Result.error("reset password failed, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Check if verification code is the same")
    @PostMapping("/checkVerificationCode")
    public ResponseEntity<Result<?>> checkVerificationCode(@RequestParam("emailAddress") String emailAddress, @RequestParam("verificationCode") String submittedVerificationCode) {
        User currentUser = userService.queryByEmailAddress(emailAddress);
        if (currentUser == null) {
            return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
        }

        // check if it is the same
        if (submittedVerificationCode.equals(redisUtil.getVerificationCode(currentUser.getId()))) {
            // generate temporary jwt token
            Map<String, Object> claims = new HashMap<>();
            claims.put(JwtClaimsKeyConstant.EMAIL_ADDRESS, emailAddress);
            claims.put(JwtClaimsKeyConstant.USER_ID, currentUser.getId());
            String jwtToken = JwtUtil.createJWT(resetPasswordSecretKey, resetPasswordTtl, claims);

            Map<String, Object> responses = new HashMap<>();
            responses.put("temp_authentication_token", jwtToken);
            return new ResponseEntity<>(Result.success(responses), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(Result.error("verification code does not match, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Reset the Password Function")
    @PostMapping("/resetPassword")
    public ResponseEntity<Result<?>> resetPassword(@RequestHeader("Authorization") String tempAuthorizationHeader, @RequestParam("emailAddress") String emailAddress , @RequestParam("newPassword") String newPassword) {
        User currentUser = userService.queryByEmailAddress(emailAddress);
        if (currentUser == null) {
            return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
        }
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(newPassword);
        userService.updatePassword(currentUser.getId(), hashedPassword);
        return new ResponseEntity<>(Result.success(), HttpStatus.OK);
    }
}
