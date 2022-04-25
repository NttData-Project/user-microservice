package com.demo.app.user.models;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class SavingAccount {
    private BigDecimal balance;
    private TypeCurrency currency;
    private SavingAccountType type;
    private String accountNumber;
    private Integer cvc;
    private String identifier;
}
