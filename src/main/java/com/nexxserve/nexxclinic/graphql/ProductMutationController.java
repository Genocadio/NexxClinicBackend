package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.CreateProductInput;
import com.nexxserve.nexxclinic.graphql.input.CreateProductInsuranceCoverageInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateProductInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateProductInsuranceCoverageInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.ProductService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ProductMutationController {

    private final ProductService productService;

    public ProductMutationController(ProductService productService) {
        this.productService = productService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse createProduct(
            @Argument @Valid CreateProductInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return productService.createProduct(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse updateProduct(
            @Argument UUID productId,
            @Argument @Valid UpdateProductInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return productService.updateProduct(productId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse createProductInsuranceCoverage(
            @Argument UUID productId,
            @Argument @Valid CreateProductInsuranceCoverageInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return productService.createProductInsuranceCoverage(productId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse updateProductInsuranceCoverage(
            @Argument UUID productInsuranceCoverageId,
            @Argument @Valid UpdateProductInsuranceCoverageInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return productService.updateProductInsuranceCoverage(productInsuranceCoverageId, input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION})
    @MutationMapping
    public ApiResponse deleteProductInsuranceCoverage(
            @Argument UUID productInsuranceCoverageId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return productService.deleteProductInsuranceCoverage(productInsuranceCoverageId);
    }
}
