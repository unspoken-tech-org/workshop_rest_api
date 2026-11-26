package com.tproject.workshop.idempotency;

/** Response captured for a completed idempotent request. */
public record StoredResponse(int status, String contentType, String body) {}
