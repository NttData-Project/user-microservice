package com.demo.app.user.models;


import lombok.Data;
import java.math.BigDecimal;

@Data
public class CurrentAccount{
    private BigDecimal balance;
    private TypeCurrency currency;
    private String accountNumber;
    private CurrentAccountType type;
    private Integer cvc;
    private String identifier;
}
