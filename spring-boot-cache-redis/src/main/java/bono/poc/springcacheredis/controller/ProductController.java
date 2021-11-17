package bono.poc.springcacheredis.controller;

import bono.poc.springcacheredis.dto.ProductDto;
import bono.poc.springcacheredis.mapper.ProductMapper;
import bono.poc.springcacheredis.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @GetMapping("/products")
    public ResponseEntity<List<ProductDto>> getProducts() {
        log.debug("Getting all products");
        return ResponseEntity.ok(
                productService.getProducts().stream()
                        .map(productMapper::modelToDto)
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDto> getProductById(@PathVariable String id) {
        log.debug("Getting product with id {}", id);
        return ResponseEntity.of(productService.getProduct(id).map(productMapper::modelToDto));
    }

}
