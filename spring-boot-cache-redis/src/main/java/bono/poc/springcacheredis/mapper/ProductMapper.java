package bono.poc.springcacheredis.mapper;

import bono.poc.springcacheredis.dto.ProductDto;
import bono.poc.springcacheredis.entity.ProductEntity;
import bono.poc.springcacheredis.model.ProductModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDto modelToDto(ProductModel productModel);
    ProductModel dtoToModel(ProductDto productDto);

    ProductModel entityToModel(ProductEntity productEntity);
    ProductEntity modelToEntity(ProductModel productModel);

}
