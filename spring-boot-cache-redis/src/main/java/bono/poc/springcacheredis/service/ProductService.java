package bono.poc.springcacheredis.service;

import bono.poc.springcacheredis.entity.ProductEntity;
import bono.poc.springcacheredis.mapper.ProductMapper;
import bono.poc.springcacheredis.model.ProductModel;
import bono.poc.springcacheredis.repository.ProductRepository;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.MessageFormat;
import java.util.Optional;

@Slf4j
@Service
@CacheConfig(cacheNames = {ProductService.CACHE_NAME})
@RequiredArgsConstructor
public class ProductService {

    public static final int PRODUCT_CATALOG_SIZE = 5000;
    public static final String CACHE_NAME = "ProductsLocalCache";

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;

    @PostConstruct
    private void init() {
        log.debug("==== INITIALIZING ====");
        log.debug("Storing product catalog with {} products (this may take a while)", PRODUCT_CATALOG_SIZE);
        for (int i = 1; i < (PRODUCT_CATALOG_SIZE + 1); i++) {
            productRepository.save(
                ProductEntity.builder()
                    .id(String.valueOf(i))
                    .name(MessageFormat.format("Product {0} name", i))
                    .description(MessageFormat.format("Product {0} description", i))
                    .price((double) i)
                    .build()
            );
        }
        log.debug("Product catalog stored");
    }

    @Timed
    @Counted
    @Cacheable
    public Optional<ProductModel> getProductFromRedisWithCache(String id) {
        log.debug("[getProductWithCache] Getting product with id {} from Redis (the central cache)", id);
        return getProductModel(id);
    }

    @Timed
    @Counted
    public Optional<ProductModel> getProductFromRedisWithoutCache(String id) {
        log.debug("[getProductWithoutCache] Getting product with id {} from Redis (the central cache)", id);
        return getProductModel(id);
    }

    private Optional<ProductModel> getProductModel(String id) {
        Optional<ProductEntity> productEntity = productRepository.findById(id);
        return productEntity.map(productMapper::entityToModel);
    }

}
