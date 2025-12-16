package org.anthills.core;

@FunctionalInterface
public interface LeaseRenewer {
    boolean renew();
}
