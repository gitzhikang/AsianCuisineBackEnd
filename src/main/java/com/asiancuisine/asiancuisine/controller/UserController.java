package com.asiancuisine.asiancuisine.controller;

import com.asiancuisine.asiancuisine.entity.User;
import com.asiancuisine.asiancuisine.service.IUserService;
import com.asiancuisine.asiancuisine.service.impl.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api("User API")
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @ApiOperation("Get User Info by Id")
    @GetMapping("/{id}")
    public User findById(
            @ApiParam(value = "user ID",required = true,example = "1") @PathVariable Long id) {
        return userService.queryById(id);
    }

    @ApiOperation("Add User Info")
    @PostMapping("/adduser")
    public String addUser(@RequestBody User user) {
        int result = userService.addUser(user);
        if (result > 0) {
            return "add user success";
        } else {
            return "add user failed";
        }
    }
}
