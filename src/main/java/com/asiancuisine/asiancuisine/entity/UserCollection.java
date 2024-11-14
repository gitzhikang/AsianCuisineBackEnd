package com.asiancuisine.asiancuisine.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class UserCollection {
    /**
     * user id
     */
    private Long userId;

    /**
     * collection id
     */
    private Long collectionId;

    /**
     * recipe url
     */
    private String recipeUrl;

    /**
     * recipe image
     */
    private String recipeImage;

    /**
     * recipe label
     */
    private String recipeLabel;

    /**
     * recipe cooking time
     */
    private Long recipeTime;
}
