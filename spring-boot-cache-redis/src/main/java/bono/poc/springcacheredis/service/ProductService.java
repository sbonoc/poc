package bono.poc.springcacheredis.service;

import bono.poc.springcacheredis.entity.ProductEntity;
import bono.poc.springcacheredis.mapper.ProductMapper;
import bono.poc.springcacheredis.model.ProductModel;
import bono.poc.springcacheredis.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@CacheConfig(cacheNames = {"ProductsLocalCache"})
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductRepository productRepository;

    @PostConstruct
    private void init() {
        productRepository.saveAll(Arrays.asList(
                ProductEntity
                        .builder()
                        .id("1")
                        .name("Name 1")
                        .description("Description 1")
                        .price(1.0d)
                        .build(),
                ProductEntity
                        .builder()
                        .id("2")
                        .name("Name 2")
                        .description("Description 2")
                        .price(2.0d)
                        .build(),
                ProductEntity
                        .builder()
                        .id("3")
                        .name("Name 3")
                        .description("Description 3")
                        .price(3.0d)
                        .build()
        ));
    }

    public List<ProductModel> getProducts() {
        Iterable<ProductEntity> found = productRepository.findAll();
        List<ProductModel> result = new ArrayList<>();
        found.forEach(productEntity -> result.add(productMapper.entityToModel(productEntity)));
        return result;
    }

    @Cacheable
    public Optional<ProductModel> getProductUsingCaffeineCache(String id) {
        log.debug("[getProductUsingCaffeineCache] Getting product with id {} from Redis (the central cache)", id);
        return getProductModel(id);
    }

    public Optional<ProductModel> getProductWithoutCache(String id) {
        log.debug("[getProductWithoutCache] Getting product with id {} from Redis (the central cache)", id);
        return getProductModel(id);
    }

    private Optional<ProductModel> getProductModel(String id) {
        Optional<ProductEntity> productEntity = productRepository.findById(id);
        return productEntity.map(productMapper::entityToModel);
    }

}
