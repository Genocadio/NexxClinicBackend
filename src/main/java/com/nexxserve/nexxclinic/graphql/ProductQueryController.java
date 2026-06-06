package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.SearchProductsInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.ProductService;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ProductQueryController {

    private final ProductService productService;

    public ProductQueryController(ProductService productService) {
        this.productService = productService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINICIAN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE})
    @QueryMapping
    public ApiResponse product(
            @Argument UUID productId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return productService.product(productId);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINICIAN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE})
    @QueryMapping
    public ApiResponse products(
            @Argument(name = "input") SearchProductsInput input,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return productService.products(input);
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.RECEPTION, RoleName.FINANCE})
    @QueryMapping
    public ApiResponse productInsuranceCoverage(
            @Argument UUID productInsuranceCoverageId,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return productService.productInsuranceCoverage(productInsuranceCoverageId);
    }
}
