package com.asiancuisine.asiancuisine.service;

import com.asiancuisine.asiancuisine.entity.MyCollection;
import com.asiancuisine.asiancuisine.entity.UserCollection;

import java.util.List;

public interface ICollectionService {
    List<UserCollection> queryRecipesByUserIdAndCollectionId(Long userId, Long collectionId);
    int addRecipeToCollection(Long userId, Long collectionId, String recipeUrl, String recipeImage, String recipeLabel, Long recipeTime);
    int deleteRecipeFromCollection(Long userId, Long collectionId, String recipeUrl, String recipeImage, String recipeLabel, Long recipeTime);
    boolean isRecipeInCollection(Long userId, String recipeUrl, String recipeImage, String recipeLabel, Long recipeTime);
    List<MyCollection> queryCollectionsByUserId(Long userId);
    int addCollection(String collectionName, String collectionImg, Long userId);
}
