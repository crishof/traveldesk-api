package com.crishof.traveldeskapi.mapper;

import com.crishof.traveldeskapi.dto.SupplierCreateRequest;
import com.crishof.traveldeskapi.dto.SupplierRequest;
import com.crishof.traveldeskapi.dto.SupplierResponse;
import com.crishof.traveldeskapi.model.Supplier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    @Mapping(target = "serviceType", source = "type")
    SupplierResponse toResponse(Supplier supplier);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Supplier toEntity(SupplierRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Supplier toEntity(SupplierCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "agency", ignore = true)
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromRequest(SupplierRequest request, @MappingTarget Supplier supplier);

    default String map(com.crishof.traveldeskapi.model.SupplierType supplierType) {
        return supplierType == null ? null : supplierType.name();
    }
}
