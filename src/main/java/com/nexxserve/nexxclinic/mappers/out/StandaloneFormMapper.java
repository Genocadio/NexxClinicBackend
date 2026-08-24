package com.nexxserve.nexxclinic.mappers.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.dto.out.StandaloneFormAnswerDto;
import com.nexxserve.nexxclinic.dto.out.StandaloneFormDto;
import com.nexxserve.nexxclinic.dto.out.StandaloneFormVersionDto;
import com.nexxserve.nexxclinic.entity.StandaloneForm;
import com.nexxserve.nexxclinic.entity.StandaloneFormAnswer;
import com.nexxserve.nexxclinic.entity.StandaloneFormVersion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class StandaloneFormMapper {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Mapping(target = "activeVersion", ignore = true)
    @Mapping(source = "isTemplate", target = "isTemplate")
    public abstract StandaloneFormDto toDto(StandaloneForm entity);

    @Mapping(source = "form.id", target = "formId")
    @Mapping(target = "blocks", qualifiedByName = "jsonToObject")
    @Mapping(target = "theme", qualifiedByName = "jsonToObject")
    public abstract StandaloneFormVersionDto toDto(StandaloneFormVersion entity);

    @Mapping(target = "answers", qualifiedByName = "jsonToObject")
    @Mapping(target = "form", expression = "java(mapFormFromVersion(entity.getFormVersion()))")
    public abstract StandaloneFormAnswerDto toDto(StandaloneFormAnswer entity);

    protected StandaloneFormDto mapFormFromVersion(StandaloneFormVersion version) {
        if (version == null || version.getForm() == null) return null;
        StandaloneForm form = version.getForm();
        StandaloneFormDto formDto = toDto(form);
        StandaloneFormVersionDto versionDto = toDto(version);
        return new StandaloneFormDto(
                formDto.id(),
                formDto.name(),
                formDto.description(),
                formDto.type(),
                formDto.category(),
                formDto.isTemplate(),
                formDto.createdBy(),
                versionDto,
                formDto.createdAt(),
                formDto.updatedAt()
        );
    }

    public abstract List<StandaloneFormDto> toDtoList(List<StandaloneForm> entities);

    public abstract List<StandaloneFormAnswerDto> toAnswerDtoList(List<StandaloneFormAnswer> entities);

    @Named("jsonToObject")
    protected Object jsonToObject(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Named("objectToJson")
    protected String objectToJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
