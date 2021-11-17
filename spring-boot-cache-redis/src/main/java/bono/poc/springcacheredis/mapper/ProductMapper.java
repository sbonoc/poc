package bono.poc.springcacheredis.mapper;

import org.mapstruct.Mapper;
import bono.poc.springcacheredis.dto.ProductDto;
import bono.poc.springcacheredis.entity.ProductEntity;
import bono.poc.springcacheredis.model.ProductModel;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDto modelToDto(ProductModel productModel);
    ProductModel dtoToModel(ProductDto productDto);

    ProductModel entityToModel(ProductEntity productEntity);
    ProductEntity modelToEntity(ProductModel productModel);

}
