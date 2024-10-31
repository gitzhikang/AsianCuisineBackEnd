package com.asiancuisine.asiancuisine.controller;
import com.asiancuisine.asiancuisine.Result.Result;
import com.asiancuisine.asiancuisine.util.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "Authenticate Api")
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthenticateController {

    @ApiOperation("Validation of User login status")
    @GetMapping("/validate")
    public ResponseEntity<Result<?>> validateUserLoginStatus(@RequestHeader("Authorization") String authorizationHeader) {
        return ResponseEntity.status(HttpStatus.OK).body(Result.success());
    }
}
