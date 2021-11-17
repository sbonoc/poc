package bono.poc.springcacheredis.dto;

import lombok.Data;

@Data
public class ProductDto {

    private final String id;
    private final String name;
    private final String description;
    private final Double price;

}
