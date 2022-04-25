package com.demo.app.user.models;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class FixedTermAccount {
    private BigDecimal balance;
    private TypeCurrency currency;
    private String accountNumber;
    private Integer cvc;
    private String identifier;
}
