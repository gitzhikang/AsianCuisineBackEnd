package com.asiancuisine.asiancuisine.controller;

import com.asiancuisine.asiancuisine.entity.User;
import com.asiancuisine.asiancuisine.service.IUserService;
import com.asiancuisine.asiancuisine.service.impl.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Api("User API")
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @ApiOperation("Get User Info by Email Address")
    @GetMapping("/{emailAddress}")
    public ResponseEntity<User> findByEmailAddress(
            @ApiParam(value = "user email address",required = true,example = "example@outlook.com") @PathVariable String emailAddress) {
        User user = userService.queryByEmailAddress(emailAddress);
        if (user != null) {
            return new ResponseEntity<>(user, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @ApiOperation("Add User Info")
    @PostMapping("/adduser")
    public ResponseEntity<String> addUser(@RequestBody User user) {
        int result = userService.addUser(user);
        if (result > 0) {
            return new ResponseEntity<>("add user success", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("add user failed", HttpStatus.BAD_REQUEST);
        }
    }
}
