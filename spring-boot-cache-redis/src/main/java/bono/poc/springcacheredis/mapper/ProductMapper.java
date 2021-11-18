package bono.poc.springcacheredis.mapper;

import bono.poc.springcacheredis.dto.ProductDto;
import bono.poc.springcacheredis.entity.ProductEntity;
import bono.poc.springcacheredis.model.ProductModel;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDto modelToDto(ProductModel productModel);

    ProductModel dtoToModel(ProductDto productDto);

    default ProductModel entityToModel(ProductEntity productEntity) {
        return ProductModel
                .builder()
                .id(String.valueOf(productEntity.getId()))
                .name(String.valueOf(productEntity.getFields().get("name")))
                .description(String.valueOf(productEntity.getFields().get("description")))
                .price((Double) productEntity.getFields().get("price"))
                .build();
    }

}
