package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.SearchIndexType;
import com.nexxserve.nexxclinic.security.HasRole;
import com.nexxserve.nexxclinic.service.MeilisearchIndexService;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class SearchIndexMutationController {

    private final MeilisearchIndexService indexService;

    public SearchIndexMutationController(MeilisearchIndexService indexService) {
        this.indexService = indexService;
    }

    @HasRole({RoleName.ADMIN, RoleName.CLINIC_ADMIN})
    @MutationMapping
    public ApiResponse reindexSearchIndexes(
            @Argument List<SearchIndexType> indexes,
            @ContextValue(name = "authUser", required = false) AuthenticatedUser authUser
    ) {
        if (indexes == null || indexes.isEmpty()) {
            return ApiResponse.error("indexes must list at least one of PRODUCTS, PATIENTS, WORKERS.");
        }
        for (SearchIndexType type : indexes) {
            indexService.reindex(uidFor(type));
        }
        return ApiResponse.success("Search indexes rebuilt.", true);
    }

    private String uidFor(SearchIndexType type) {
        return switch (type) {
            case PRODUCTS -> MeilisearchIndexService.PRODUCTS_INDEX;
            case PATIENTS -> MeilisearchIndexService.PATIENTS_INDEX;
            case WORKERS -> MeilisearchIndexService.WORKERS_INDEX;
        };
    }
}
