package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.Icd11SearchResult;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.Icd11SearchService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class Icd11QueryController {

    private final Icd11SearchService icd11SearchService;

    public Icd11QueryController(Icd11SearchService icd11SearchService) {
        this.icd11SearchService = icd11SearchService;
    }

    @HasRole({RoleName.CLINICIAN, RoleName.NURSE, RoleName.CLINIC_ADMIN})
    @QueryMapping
    public ApiResponse<Icd11SearchResult> searchIcd11Diseases(
            @Argument String query,
            @Argument String lang,
            @Argument Integer limit,
            @Argument String chapter,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        return icd11SearchService.search(query, lang, limit, chapter);
    }
}
