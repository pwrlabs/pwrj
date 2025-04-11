package com.github.pwrlabs.pwrj.record.response;

import com.github.pwrlabs.entities.FalconTransaction;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class TransactionForGuardianApproval {
    private boolean valid;
    private String guardianAddress;
    private String errorMessage;
    private FalconTransaction transaction;
}
