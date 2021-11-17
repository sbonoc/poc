package bono.poc.springcacheredis.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@Builder
@RedisHash("Product")
public class ProductEntity implements Serializable {

    @Id
    private final String id;
    private final String name;
    private final String description;
    private final Double price;

}
