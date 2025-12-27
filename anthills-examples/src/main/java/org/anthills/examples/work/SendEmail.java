package org.anthills.examples.work;

public record SendEmail(
    String to,
    String subject,
    String body
) {}
