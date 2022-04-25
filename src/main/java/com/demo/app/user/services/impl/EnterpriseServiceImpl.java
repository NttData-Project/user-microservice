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
    private final WebClient webClientPassiveCard;
    private final WebClient webClientActiveCard;

    public EnterpriseServiceImpl(EnterpriseRepository enterpriseRepository, WebClient.Builder webClientPassiveCard, WebClient.Builder webClientActiveCard
            ,@Value("${passive.card}") String passiveCardUrl,@Value("${active.card}") String activeCardUrl) {
        this.enterpriseRepository = enterpriseRepository;
        this.webClientPassiveCard = webClientPassiveCard.baseUrl(passiveCardUrl).build();
        this.webClientActiveCard = webClientActiveCard.baseUrl(activeCardUrl).build();
    }

    private static Mono<? extends Boolean> apply(Boolean x) {
        System.out.println(x);
        return Boolean.TRUE.equals(x)?Mono.just(true):Mono.just(false);
    }

    private Mono<Boolean> createCurrentAccount(List<CurrentAccount> cards) {
        return webClientPassiveCard.post().uri("/currentAccount/all").
                body(Flux.fromIterable(cards), CurrentAccount.class).
                retrieve().bodyToFlux(CurrentAccount.class).then(Mono.just(true));
    }
    private Mono<Boolean> findAllCurrentAccountByRuc (String ruc){
        return webClientPassiveCard.get().uri("/currentAccount/all/identifier/" + ruc).
                retrieve().bodyToFlux(CurrentAccount.class).hasElements().flatMap(EnterpriseServiceImpl::apply);
    }
    private Mono<Boolean> createCreditAccount (List<CreditAccount> cards) {
        return webClientActiveCard.post().uri("/creditAccount/all").
                body(Flux.fromIterable(cards), CreditAccount.class).
                retrieve().bodyToFlux(CreditAccount.class).then(Mono.just(true));
    }
    private Mono<Boolean> findAllCreditAccountByRuc (String ruc){
        return webClientActiveCard.get().uri("/creditAccount/all/identifier/" + ruc).
                retrieve().bodyToFlux(CreditAccount.class).hasElements().flatMap(EnterpriseServiceImpl::apply);
    }
    @Override
    public Flux<Enterprise> findAll() {
        return enterpriseRepository.findAll();
    }

    private Mono<Enterprise> findAndSaveCurrentAccount(Enterprise enterprise, CurrentAccountType type) {
        return findAllCurrentAccountByRuc(enterprise.getRuc()).flatMap(x->{
            if(!x){
                List<CurrentAccount> cards = enterprise.getCards();
                cards.forEach(card -> {
                    card.setIdentifier(enterprise.getRuc());
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
        return findAllCreditAccountByRuc(enterprise.getRuc()).flatMap(x->x?findAndSaveCurrentAccount(enterprise,CurrentAccountType.PYME):Mono.empty());
    }

    @Override
    public Mono<Enterprise> saveCreditAccount(Enterprise enterprise) {
        return findAllCreditAccountByRuc(enterprise.getRuc()).flatMap(x->{
            if(!x){
                List<CreditAccount> cards = enterprise.getCreditAccounts();
                cards.forEach(card -> card.setIdentifier(enterprise.getRuc()));
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
            x.setEmail(enterprise.getEmail());
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
