package com.nexxserve.nexxclinic.persistence;

import com.nexxserve.nexxclinic.model.NoteType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class NoteTypeConverter implements AttributeConverter<NoteType, Short> {

    @Override
    public Short convertToDatabaseColumn(NoteType attribute) {
        return attribute == null ? null : (short) attribute.ordinal();
    }

    @Override
    public NoteType convertToEntityAttribute(Short dbData) {
        if (dbData == null) {
            return null;
        }

        int ordinal = dbData.intValue();
        NoteType[] values = NoteType.values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Unknown NoteType ordinal: " + ordinal);
        }
        return values[ordinal];
    }
}
