package org.example.util;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator;

/**
 * Utility class for generating request IDs using UUIDv7 (time-ordered UUIDs). UUIDv7 is ideal for
 * request IDs because: - Time-ordered: easier to sort and debug chronologically - Unique:
 * collision-resistant - Standard UUID format: 36 characters
 */
public final class RequestIdGenerator {

  private static final TimeBasedEpochGenerator GENERATOR = Generators.timeBasedEpochGenerator();

  private RequestIdGenerator() {
    // Utility class - prevent instantiation
  }

  /**
   * Generates a new UUIDv7 (time-based epoch) request ID.
   *
   * @return A time-ordered UUID string
   */
  public static String generate() {
    return GENERATOR.generate().toString();
  }
}
