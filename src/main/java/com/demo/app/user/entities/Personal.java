package com.demo.app.user.entities;

import com.demo.app.user.models.CreditAccount;
import com.demo.app.user.models.CurrentAccount;
import com.demo.app.user.models.FixedTermAccount;
import com.demo.app.user.models.SavingAccount;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import javax.persistence.OneToOne;
import javax.validation.constraints.*;

@JsonPropertyOrder({"id","name","lastName","email","number","dni","createdAt","updateAt"})
@Document(collection = "personal")
@Data
public class Personal extends Audit {
    @Id
    private String id;

    @NotEmpty
    private String name;

    @Field(name = "last_name")
    @NotEmpty
    private String lastName;

    @Email
    @Indexed(unique=true, sparse=true)
    private String email;

    @NotEmpty
    @Size(min = 8,max = 8)
    private String dni;

    @NotEmpty
    @Size(min = 9,max = 9)
    private String number;

    @OneToOne
    private CurrentAccount currentAccount;

    @OneToOne
    private FixedTermAccount fixedTermAccount;

    @OneToOne
    private SavingAccount savingAccount;

    @OneToOne
    private CreditAccount creditAccount;
}
