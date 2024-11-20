package com.asiancuisine.asiancuisine.controller;

import com.asiancuisine.asiancuisine.Result.Result;
import com.asiancuisine.asiancuisine.entity.MyCollection;
import com.asiancuisine.asiancuisine.entity.User;
import com.asiancuisine.asiancuisine.entity.UserCollection;
import com.asiancuisine.asiancuisine.service.impl.CollectionService;
import com.asiancuisine.asiancuisine.service.impl.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.websocket.server.PathParam;
import java.util.List;

@Api(tags = "Collection Api")
@Slf4j
@RestController
@RequestMapping("/collection")
public class CollectionController {
    @Autowired
    private CollectionService collectionService;

    @Autowired
    private UserService userService;

    @ApiOperation("Get Collection Info for One User")
    @GetMapping("/getCollectionInfo")
    public ResponseEntity<Result<?>> getCollectionInfo(@RequestParam("emailAddress") String emailAddress) {
        try {
            // acquire user info
            User currentUser = userService.queryByEmailAddress(emailAddress);
            if (currentUser == null) {
                return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
            }

            // acquire all collection info
            List<MyCollection> collections = collectionService.queryCollectionsByUserId(currentUser.getId());

            return new ResponseEntity<>(Result.success(collections), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(Result.error("get collection info failed, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Get Recipes for User's One Collection")
    @GetMapping("/getCollectionRecipes")
    public ResponseEntity<Result<?>> getCollectionInfo(@RequestParam("emailAddress") String emailAddress, @RequestParam("collectionId") Long collectionId) {
        try {
            // acquire user info
            User currentUser = userService.queryByEmailAddress(emailAddress);
            if (currentUser == null) {
                return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
            }

            // acquire all collection info by user id
            List<UserCollection> recipes = collectionService.queryRecipesByUserIdAndCollectionId(currentUser.getId(), collectionId);

            return new ResponseEntity<>(Result.success(recipes), HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(Result.error("get recipe info for one collection failed, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Bind Recipe To a Collection")
    @PostMapping("/bindRecipeToCollection")
    public ResponseEntity<Result<?>> bindRecipeToCollection(@RequestParam("emailAddress") String emailAddress,
                                                            @RequestParam("collectionId") Long collectionId,
                                                            @RequestParam("recipeUrl") String recipeUrl,
                                                            @RequestParam("recipeImage") String recipeImage,
                                                            @RequestParam("recipeLabel") String recipeLabel,
                                                            @RequestParam("recipeTime") Long recipeTime) {
        try {
            User currentUser = userService.queryByEmailAddress(emailAddress);
            if (currentUser == null) {
                return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
            }

            int result = collectionService.addRecipeToCollection(currentUser.getId(), collectionId, recipeUrl, recipeImage, recipeLabel, recipeTime);
            if (result > 0) {
                return new ResponseEntity<>(Result.success(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(Result.error("add recipe to collection failed"), HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(Result.error("add recipe to collection failed, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Add a Collection")
    @PostMapping("/addCollection")
    public ResponseEntity<Result<?>> addCollection(@RequestParam("emailAddress") String emailAddress, @RequestParam("collectionName") String collectionName, @RequestParam("collectionImage") MultipartFile collectionImage) {
        try {
            User currentUser = userService.queryByEmailAddress(emailAddress);
            if (currentUser == null) {
                return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
            }

            // upload to aws
            String collectionImageUrl = userService.uploadIconToAWS(collectionImage);

            int result = collectionService.addCollection(collectionName, collectionImageUrl, currentUser.getId());

            if (result > 0) {
                // fetch all collections in the database
                List<MyCollection> collections = collectionService.queryCollectionsByUserId(currentUser.getId());
                return new ResponseEntity<>(Result.success(collections), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(Result.error("add collection failed"), HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(Result.error("add collection failed, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation("Check Whether A Recipe Is In Collections")
    @GetMapping("checkInCollection")
    public ResponseEntity<Result<?>> checkInCollection(@RequestParam("emailAddress") String emailAddress,
                                                       @RequestParam("recipeUrl") String recipeUrl,
                                                       @RequestParam("recipeImage") String recipeImage,
                                                       @RequestParam("recipeLabel") String recipeLabel,
                                                       @RequestParam("recipeTime") Long recipeTime) {
        try {
            User currentUser = userService.queryByEmailAddress(emailAddress);
            if (currentUser == null) {
                return new ResponseEntity<>(Result.error("email address does not exist in our database"), HttpStatus.BAD_REQUEST);
            }

            if (collectionService.isRecipeInCollection(currentUser.getId(), recipeUrl, recipeImage, recipeLabel, recipeTime)) {
                return new ResponseEntity<>(Result.success(), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(Result.error("recipe does not exist in any collection"), HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(Result.error("check recipe in collection failed, please retry!"), HttpStatus.BAD_REQUEST);
        }
    }
}
