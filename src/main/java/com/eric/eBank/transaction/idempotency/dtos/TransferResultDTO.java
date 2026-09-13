package com.eric.eBank.transaction.idempotency.dtos;

import com.eric.eBank.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResultDTO {

    private Long transferId;
    
    private TransactionStatus status;
}
