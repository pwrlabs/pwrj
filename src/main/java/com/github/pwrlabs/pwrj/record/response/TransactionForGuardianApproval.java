package com.github.pwrlabs.pwrj.record.response;

import com.github.pwrlabs.pwrj.entities.FalconTransaction;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
/**
 * TransactionForGuardianApproval class.
 */
public class TransactionForGuardianApproval {
    private boolean valid;
    private String guardianAddress;
    private String errorMessage;
    private FalconTransaction transaction;
}
