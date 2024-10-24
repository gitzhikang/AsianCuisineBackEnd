package com.asiancuisine.asiancuisine.entity;

import com.asiancuisine.asiancuisine.enums.Sex;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * primary key
     */
    private Long id;

    /**
     * email address
     */
    private String emailAddress;

    /**
     * password hashed
     */
    private String password;

    /**
     * first name
     */
    private String firstName;

    /**
     * last name
     */
    private String lastName;

    /**
     * sex
     */
    private Sex sex;

    /**
     * nickname
     */
    private String nickName;

    /**
     * user icon
     */
    private String icon = "";

    /**
     * create time
     */
    private LocalDateTime createTime;

    /**
     * update time
     */
    private LocalDateTime updateTime;
}
