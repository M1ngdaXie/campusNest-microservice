package com.campusnest.housingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MapMarkerDTO {
    private Long id;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal price;
    private String title;
    private String thumbnailUrl;
}
