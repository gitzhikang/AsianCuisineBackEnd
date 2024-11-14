package com.asiancuisine.asiancuisine.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class MyCollection {
    /**
     * collection id
     */
    private Long collectionId;

    /**
     * collection name
     */
    private String collectionName;

    /**
     * collection image url
     */
    private String collectionImg;

    /**
     * collection belonged user id
     */
    private int userId;
}
