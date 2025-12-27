package org.anthills.examples.work;

public record SendSms(
    String phone,
    String message
) {}
