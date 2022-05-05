package com.demo.app.user.services.impl;

import com.demo.app.user.entities.Personal;
import com.demo.app.user.models.*;
import com.demo.app.user.repositories.PersonalRepository;
import com.demo.app.user.services.PersonalService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



@Service
public class PersonalServiceImpl implements PersonalService {

    private final PersonalRepository personalRepository;
    private final WebClient webClientPassiveCard;
    private final WebClient webClientActiveCard;


    public PersonalServiceImpl(PersonalRepository personalRepository, WebClient.Builder webClientPassiveCard, WebClient.Builder webClientActiveCard
            , @Value("${passive.card}") String passiveCardUrl, @Value("${active.card}") String activeCardUrl) {
        this.personalRepository = personalRepository;
        this.webClientPassiveCard = webClientPassiveCard.baseUrl(passiveCardUrl).build();
        this.webClientActiveCard = webClientActiveCard.baseUrl(activeCardUrl).build();
    }

    private static Mono<? extends Boolean> apply(Boolean x) {
        return Boolean.TRUE.equals(x)?Mono.just(true):Mono.just(false);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<Personal> findAll() {
        return personalRepository.findAll();
    }

    private Mono<CurrentAccount> createCurrentAccount(CurrentAccount card) {
        return webClientPassiveCard.post().uri("/currentAccount").
                body(Mono.just(card), CurrentAccount.class).
                retrieve().bodyToMono(CurrentAccount.class);
    }
    private Mono<SavingAccount> createSavingAccount(SavingAccount card) {
        return webClientPassiveCard.post().uri("/savingAccount").
                body(Mono.just(card), SavingAccount.class).
                retrieve().bodyToMono(SavingAccount.class);
    }
    private Mono<FixedTermAccount> createFixedTermAccount(FixedTermAccount card) {
        return webClientPassiveCard.post().uri("/fixedTermAccount").
                body(Mono.just(card), FixedTermAccount.class).
                retrieve().bodyToMono(FixedTermAccount.class);
    }

    private Mono<Boolean> findCurrentAccountByDni(String dni){
        return webClientPassiveCard.get().uri("/currentAccount/identifier/" + dni).
                retrieve().bodyToMono(Boolean.class);
    }
    private Mono<Boolean> findSavingAccountByDni(String dni){
        return webClientPassiveCard.get().uri("/savingAccount/identifier/" + dni).
                retrieve().bodyToMono(Boolean.class);
    }
    private Mono<Boolean> findFixedTermAccountByDni(String dni){
        return webClientPassiveCard.get().uri("/fixedTermAccount/identifier/" + dni).
                retrieve().bodyToMono(Boolean.class);
    }

    private Mono<Boolean> createCreditAccount(CreditAccount card) {
        return webClientActiveCard.post().uri("/creditAccount").
                body(Mono.just(card), CreditAccount.class).
                retrieve().bodyToMono(CreditAccount.class).then(Mono.just(true));
    }
    private Mono<Boolean> findAllCreditAccountByDni(String dni){
        return webClientActiveCard.get().uri("/creditAccount/all/identifier/" + dni).
                retrieve().bodyToFlux(CreditAccount.class).hasElements().flatMap(PersonalServiceImpl::apply);
    }

    private Mono<CurrentAccount> updateCurrentAccount(CurrentAccount currentAccount){
        return webClientPassiveCard.put().uri("/currentAccount/identifier" + currentAccount.getIdentifier()+
                "/account/"+currentAccount.getAccountNumber()).
                body(Mono.just(currentAccount), CurrentAccount.class)
                .retrieve()
                .bodyToMono(CurrentAccount.class);
    }

    private Mono<Boolean> findCardsDuplicated(String dni) {
        return Mono.zip(findSavingAccountByDni(dni), findCurrentAccountByDni(dni), findFixedTermAccountByDni(dni))
                .map(account -> {
                    if (account.getT1().equals(false) && account.getT2().equals(false) && account.getT3().equals(false)) {
                        return false;
                    }
                    return true;
                });
    }


    private Mono<Personal> findAndCreatePersonalAccount(Personal personal, SavingAccount savingAccount) {
        return findCardsDuplicated(personal.getDni()).flatMap(x->!x?Mono.zip(personalRepository.save(personal),createSavingAccount(savingAccount))
                .map(account -> {
                    account.getT1();
                    return account.getT2();
                }):Mono.empty()).thenReturn(personal);
    }

    @Override
    @Transactional
    public Mono<Personal> saveNormalSavingAccount(Personal personal) {
        SavingAccount savingAccount = personal.getSavingAccount();
        savingAccount.setIdentifier(personal.getDni());
        savingAccount.setType(SavingAccountType.NORMAL);
        return findAndCreatePersonalAccount(personal, savingAccount);
    }

    @Override
    public Mono<Personal> saveVipSavingAccount(Personal personal) {
        SavingAccount savingAccount = personal.getSavingAccount();
        savingAccount.setIdentifier(personal.getDni());
        savingAccount.setType(SavingAccountType.VIP);
        return findAllCreditAccountByDni(personal.getDni()).flatMap(x->x?findAndCreatePersonalAccount(personal,savingAccount):Mono.empty());
    }

    @Override
    @Transactional
    public Mono<Personal> saveFixedTermAccount(Personal personal) {
        FixedTermAccount fixedTermAccount = personal.getFixedTermAccount();
        fixedTermAccount.setIdentifier(personal.getDni());
        return findCardsDuplicated(personal.getDni()).flatMap(x->!x?Mono.zip(personalRepository.save(personal), createFixedTermAccount(fixedTermAccount))
                .map(account -> {
                    account.getT1();
                    return account.getT2();
                }):Mono.empty()).thenReturn(personal);
    }

    @Override
    public Mono<Personal> saveCreditAccount(Personal personal) {
        CreditAccount creditAccount = personal.getCreditAccount();
        creditAccount.setIdentifier(personal.getDni());
        return Mono.zip(findAllCreditAccountByDni(personal.getDni()),createCreditAccount(creditAccount),personalRepository.save(personal))
                .map(account->{
                    if(account.getT1().equals(true)) return Mono.empty();
                    account.getT2();
                    return account.getT3();
                }).thenReturn(personal);
    }

    @Override
    @Transactional
    public Mono<Personal> saveCurrentAccount(Personal personal) {
        CurrentAccount currentAccount = personal.getCurrentAccount();
        currentAccount.setIdentifier(personal.getDni());
        currentAccount.setType(CurrentAccountType.NORMAL);
        return findCardsDuplicated(personal.getDni()).flatMap(x->!x?Mono.zip(personalRepository.save(personal), createCurrentAccount(currentAccount))
                .map(account -> {
                    account.getT1();
                    return account.getT2();
                }):Mono.empty()).thenReturn(personal);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<Personal> findById(String id) {
        return personalRepository.findById(id);
    }

    @Override
    public Mono<Personal> update(Personal personal,String id) {
        return personalRepository.findById(id).flatMap(x->{
            x.setName(personal.getName());
            x.setLastName(personal.getLastName());
            x.setEmail(personal.getEmail());
            x.setNumber(personal.getNumber());
            if(x.getCurrentAccount()!=null) {
                x.setCurrentAccount(personal.getCurrentAccount());
                return updateCurrentAccount(personal.getCurrentAccount()).then(personalRepository.save(x));
            }else return personalRepository.save(x);
        });
    }

    @Override
    public Mono<Void> delete(String id) {
        return personalRepository.deleteById(id);
    }
}
