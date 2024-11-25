package com.asiancuisine.asiancuisine.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class UploadWithUrlDTO {
    String[] imageUrls;
    String text;
    String title;
    String tags;
}
