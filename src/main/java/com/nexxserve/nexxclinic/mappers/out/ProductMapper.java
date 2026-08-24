package com.nexxserve.nexxclinic.mappers.out;

import com.nexxserve.nexxclinic.dto.out.ProductDto;
import com.nexxserve.nexxclinic.dto.out.ProductInsuranceCoverageDto;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {InsuranceProviderMapper.class})
public interface ProductMapper {

    ProductDto toDto(Product product);

    @Mapping(source = "insuranceProvider", target = "insuranceProvider")
    ProductInsuranceCoverageDto toDto(ProductInsuranceCoverage coverage);

    List<ProductDto> toDtoList(List<Product> products);
}
