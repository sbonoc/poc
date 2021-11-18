package bono.poc.springcacheredis.repository;

import bono.poc.springcacheredis.entity.ProductEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ProductRepository extends PagingAndSortingRepository<ProductEntity, String> {
}
