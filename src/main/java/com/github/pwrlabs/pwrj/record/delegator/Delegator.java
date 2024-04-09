package com.github.pwrlabs.pwrj.record.delegator;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class Delegator {
    private final String address;
    private final String validatorAddress;
    private final long shares;
    private final long delegatedPWR;
}
