package com.demo.app.user.entities;

import com.demo.app.user.models.CreditAccount;
import com.demo.app.user.models.CurrentAccount;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.persistence.OneToMany;
import javax.validation.constraints.*;
import java.util.List;

@JsonPropertyOrder({"id","name","lastName","dni","email","number","ruc","createdAt","updateAt"})
@Document(collection = "enterprise")
@Data
public class Enterprise extends Audit {
    @Id
    private String id;

    @NotEmpty
    private String name;

    @NotEmpty
    @Size(min = 9, max = 9)
    private String number;

    @NotEmpty
    @Size(min = 11, max = 11)
    private String ruc;

    @Email
    private String email;

    @OneToMany
    private List<CurrentAccount> cards;

    @OneToMany
    private List<CreditAccount> creditAccounts;

}
