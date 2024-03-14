package com.github.pwrlabs.pwrj.ResponseModels;

import com.github.pwrlabs.pwrj.Transaction.Transaction;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class TxnForGuardianApproval {
    private boolean valid;
    private String guardianAddress;
    private String errorMessage;
    private Transaction transaction;
}
