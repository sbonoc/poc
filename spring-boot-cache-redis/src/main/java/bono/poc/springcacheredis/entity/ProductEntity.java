package bono.poc.springcacheredis.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@RedisHash("Product")
public class ProductEntity implements Serializable {

    @Id
    private final Object id;
    private final Map<?, ?> fields;

}
