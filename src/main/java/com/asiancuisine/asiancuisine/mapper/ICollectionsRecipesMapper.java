package com.asiancuisine.asiancuisine.mapper;

import com.asiancuisine.asiancuisine.entity.UserCollection;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ICollectionsRecipesMapper {
    List<UserCollection> queryRecipesByUserIdAndCollectionId(Long userId, Long collectionId);
    int checkRecipesInCollection(Long userId, String recipeUrl, String recipeImage, String recipeLabel, Long recipeTime);
    int addRecipeToCollection(Long userId, Long collectionId, String recipeUrl, String recipeImage, String recipeLabel, Long recipeTime);
}
