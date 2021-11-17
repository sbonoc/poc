package bono.poc.springcacheredis.repository;

import org.springframework.data.repository.PagingAndSortingRepository;
import bono.poc.springcacheredis.entity.ProductEntity;

public interface ProductRepository extends PagingAndSortingRepository<ProductEntity, String> {
}
