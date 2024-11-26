package com.asiancuisine.asiancuisine.service.impl;

import com.asiancuisine.asiancuisine.entity.MyCollection;
import com.asiancuisine.asiancuisine.entity.UserCollection;
import com.asiancuisine.asiancuisine.mapper.ICollectionsMapper;
import com.asiancuisine.asiancuisine.mapper.ICollectionsRecipesMapper;
import com.asiancuisine.asiancuisine.service.ICollectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CollectionService implements ICollectionService {
    @Autowired
    private ICollectionsRecipesMapper collectionsRecipesMapper;

    @Autowired
    private ICollectionsMapper collectionsMapper;

    @Override
    public List<UserCollection> queryRecipesByUserIdAndCollectionId(Long userId, Long collectionId) {
        return collectionsRecipesMapper.queryRecipesByUserIdAndCollectionId(userId, collectionId);
    }

    @Override
    public int addRecipeToCollection(Long userId, Long collectionId, String recipeUrl, String recipeImage, String recipeLabel, Long recipeTime) {
        return collectionsRecipesMapper.addRecipeToCollection(userId, collectionId, recipeUrl, recipeImage, recipeLabel, recipeTime);
    }

    public int deleteRecipeFromCollection(Long userId, Long collectionId, String recipeUrl, String recipeImage, String recipeLabel, Long recipeTime) {
        return collectionsRecipesMapper.deleteRecipeFromCollection(userId, collectionId, recipeUrl, recipeImage, recipeLabel, recipeTime);
    }

    @Override
    public boolean isRecipeInCollection(Long userId, String recipeUrl, String recipeImage, String recipeLabel, Long recipeTime) {
        int result = collectionsRecipesMapper.checkRecipesInCollection(userId, recipeUrl, recipeImage, recipeLabel, recipeTime);
        return result > 0;
    }

    @Override
    public List<MyCollection> queryCollectionsByUserId(Long userId) {
        return collectionsMapper.queryCollectionsByUserId(userId);
    }

    @Override
    public int addCollection(String collectionName, String collectionImg, Long userId) {
        return collectionsMapper.addCollection(collectionName, collectionImg, userId);
    }
}
