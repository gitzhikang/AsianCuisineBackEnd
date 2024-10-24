package com.asiancuisine.asiancuisine.mapper;

import com.asiancuisine.asiancuisine.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IUserMapper {
    public User queryById(Long id);
}
