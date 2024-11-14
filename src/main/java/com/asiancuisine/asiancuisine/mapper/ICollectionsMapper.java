package com.asiancuisine.asiancuisine.mapper;

import com.asiancuisine.asiancuisine.entity.MyCollection;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ICollectionsMapper {
    List<MyCollection> queryCollectionsByUserId(Long userId);
    int addCollection(String collectionName, String collectionImg, Long userId);
}
