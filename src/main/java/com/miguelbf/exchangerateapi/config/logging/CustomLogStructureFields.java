package com.miguelbf.exchangerateapi.config.logging;

import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Implement this on custom exception types to have their fields
 * automatically unpacked into the ECS JSON log output.
 */
public interface CustomLogStructureFields {

    Map<String, @Nullable Object> getLogStructureFields();

}
