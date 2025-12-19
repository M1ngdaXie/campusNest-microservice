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
public class CoordinatesDTO {
    private BigDecimal latitude;

    private BigDecimal longitude;

    private Boolean success;

    private String address;


}
