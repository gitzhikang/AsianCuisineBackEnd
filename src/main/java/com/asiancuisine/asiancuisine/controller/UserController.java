package com.asiancuisine.asiancuisine.controller;

import com.asiancuisine.asiancuisine.Result.Result;
import com.asiancuisine.asiancuisine.constant.JwtClaimsKeyConstant;
import com.asiancuisine.asiancuisine.entity.User;
import com.asiancuisine.asiancuisine.service.IUserService;
import com.asiancuisine.asiancuisine.util.JwtUtil;
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

@Api(tags = "User Api")
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    // 注入配置文件中的 JWT 秘钥和过期时间
    @Value("${jwt.user-secret-key}")
    private String userSecretKey;

    @Value("${jwt.user-ttl-remembered}")
    private long userTtlRemembered;

    @Value("${jwt.user-ttl-not-remembered}")
    private long userTtlNotRemembered;

    @ApiOperation("Get User Info by Email Address")
    @GetMapping("/{emailAddress}")
    public ResponseEntity<Result<User>> findByEmailAddress(
            @ApiParam(value = "user email address",required = true,example = "example@outlook.com") @PathVariable String emailAddress) {
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
}
