package com.demo.app.user.services.impl;

import com.demo.app.user.entities.Enterprise;
import com.demo.app.user.models.CreditAccount;
import com.demo.app.user.models.CurrentAccount;
import com.demo.app.user.models.CurrentAccountType;
import com.demo.app.user.repositories.EnterpriseRepository;
import com.demo.app.user.services.EnterpriseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class EnterpriseServiceImpl implements EnterpriseService {

    private final EnterpriseRepository enterpriseRepository;
    private final WebClient webClientPasiveCard;
    private final WebClient webClientActiveCard;

    public EnterpriseServiceImpl(EnterpriseRepository enterpriseRepository, WebClient.Builder webClientPasiveCard, WebClient.Builder webClientActiveCard
            ,@Value("${pasive.card}") String pasiveCardUrl,@Value("${active.card}") String activeCardUrl) {
        this.enterpriseRepository = enterpriseRepository;
        this.webClientPasiveCard = webClientPasiveCard.baseUrl(pasiveCardUrl).build();
        this.webClientActiveCard = webClientActiveCard.baseUrl(activeCardUrl).build();
    }

    private static Mono<? extends Boolean> apply(Boolean x) {
        return Boolean.TRUE.equals(x)?Mono.just(true):Mono.just(false);
    }

    private Mono<Boolean> createCurrentAccount(List<CurrentAccount> cards) {
        return webClientPasiveCard.post().uri("/currentAccount/all").
                body(Flux.fromIterable(cards), CurrentAccount.class).
                retrieve().bodyToFlux(CurrentAccount.class).then(Mono.just(true));
    }
    private Mono<Boolean> findAllCurrentAccountByDni(String dni){
        return webClientPasiveCard.get().uri("/currentAccount/all/dni/" + dni).
                retrieve().bodyToFlux(CurrentAccount.class).hasElements().flatMap(EnterpriseServiceImpl::apply);
    }
    private Mono<Boolean> createCreditAccount(List<CreditAccount> cards) {
        return webClientActiveCard.post().uri("/creditAccount/all").
                body(Flux.fromIterable(cards), CreditAccount.class).
                retrieve().bodyToFlux(CreditAccount.class).then(Mono.just(true));
    }
    private Mono<Boolean> findAllCreditAccountByDni(String dni){
        return webClientActiveCard.get().uri("/creditAccount/all/dni/" + dni).
                retrieve().bodyToFlux(CreditAccount.class).hasElements().flatMap(EnterpriseServiceImpl::apply);
    }
    @Override
    public Flux<Enterprise> findAll() {
        return enterpriseRepository.findAll();
    }

    private Mono<Enterprise> findAndSaveCurrentAccount(Enterprise enterprise, CurrentAccountType type) {
        return findAllCurrentAccountByDni(enterprise.getDni()).flatMap(x->{
            if(!x){
                List<CurrentAccount> cards = enterprise.getCards();
                cards.forEach(card -> {
                    card.setDni(enterprise.getDni());
                    card.setType(type);
                });
                Mono<Boolean> accounts = createCurrentAccount(cards);
                return Mono.zip(enterpriseRepository.save(enterprise),accounts)
                        .map(result->{
                            result.getT1();
                            return result.getT2();
                        });
            }
            return Mono.empty();
        }).thenReturn(enterprise);
    }
    @Override
    public Mono<Enterprise> saveNormalCurrentAccount(Enterprise enterprise) {
        return findAndSaveCurrentAccount(enterprise,CurrentAccountType.NORMAL);
    }

    @Override
    public Mono<Enterprise> savePymeCurrentAccount(Enterprise enterprise) {
        return Mono.zip(findAllCreditAccountByDni(enterprise.getDni()),findAndSaveCurrentAccount(enterprise,CurrentAccountType.PYME))
                .map(result->{
                    if(result.getT1().equals(false)) return Mono.empty();
                    return result.getT2();
                }).thenReturn(enterprise);
    }

    @Override
    public Mono<Enterprise> saveCreditAccount(Enterprise enterprise) {
        return findAllCreditAccountByDni(enterprise.getDni()).flatMap(x->{
            if(!x){
                List<CreditAccount> cards = enterprise.getCreditAccounts();
                cards.forEach(card -> card.setDni(enterprise.getDni()));
                Mono<Boolean> accounts = createCreditAccount(cards);
                return Mono.zip(enterpriseRepository.save(enterprise),accounts)
                        .map(result->{
                            result.getT1();
                            return result.getT2();
                        });
            }
            return Mono.empty();
        }).thenReturn(enterprise);
    }

    @Override
    public Mono<Enterprise> findById(String id) {
        return enterpriseRepository.findById(id);
    }

    @Override
    public Mono<Enterprise> update(Enterprise enterprise, String id) {
        return enterpriseRepository.findById(id).flatMap(x->{
            x.setName(enterprise.getName());
            x.setLastName(enterprise.getLastName());
            x.setEmail(enterprise.getEmail());
            x.setDni(enterprise.getDni());
            x.setNumber(enterprise.getNumber());
            x.setRuc(enterprise.getRuc());
            return enterpriseRepository.save(x);
        });
    }

    @Override
    public Mono<Void> delete(String id) {
        return enterpriseRepository.deleteById(id);
    }
}
