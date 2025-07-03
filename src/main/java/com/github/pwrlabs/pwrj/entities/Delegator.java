package com.github.pwrlabs.pwrj.entities;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@Getter
@SuperBuilder
/**
 * Delegator class.
 */
public class Delegator {
    private final String address;
    private final String validatorAddress;
    private final BigInteger shares;
    private final long delegatedPWR;
}
