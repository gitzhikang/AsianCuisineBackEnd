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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @Value("${jwt.user-ttl}")
    private long userTtl;

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
        int result = userService.addUser(user);
        if (result > 0) {
            return new ResponseEntity<>(Result.success(), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(Result.error("register user failed, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Login")
    @PostMapping("/login")
    public ResponseEntity<Result<String>> login(@RequestBody Map<String, String> loginRequest) {
        String emailAddress = loginRequest.get("emailAddress");
        String password = loginRequest.get("password");
        String isRemember = loginRequest.get("isRemembered");

        String dbPassword = userService.queryPasswordByEmailAddress(emailAddress);
        if (dbPassword != null && dbPassword.equals(password)) {
            if (isRemember.equals("true")) {
                Map<String, Object> claims = new HashMap<>();
                claims.put(JwtClaimsKeyConstant.EMAIL_ADDRESS, emailAddress);
                String jwtToken = JwtUtil.createJWT(userSecretKey, userTtl, claims);
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + jwtToken);

                return new ResponseEntity<>(Result.success(), headers, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(Result.success(), HttpStatus.OK);
            }
        } else {
            return new ResponseEntity<>(Result.error("login failed, password is wrong"), HttpStatus.BAD_REQUEST);
        }
    }
}
