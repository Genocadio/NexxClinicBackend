package com.nexxserve.nexxclinic.graphql.input;

public record UpdateUserSettingsInput(
    String fontSize,
    String theme,
    Object settingsJson
) {}
