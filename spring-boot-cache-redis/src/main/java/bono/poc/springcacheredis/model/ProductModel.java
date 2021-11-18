package bono.poc.springcacheredis.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class ProductModel implements Serializable {

    private final String id;
    private final String name;
    private final String description;
    private final Double price;

}
