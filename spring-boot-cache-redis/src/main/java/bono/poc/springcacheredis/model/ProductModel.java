package bono.poc.springcacheredis.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class ProductModel implements Serializable {

    private final String id;
    private final String name;
    private final String description;
    private final Double price;

}
