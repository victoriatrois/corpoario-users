package br.com.corpoario.users.repository;

import br.com.corpoario.users.entity.Account;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AccountRepository implements PanacheRepositoryBase<Account, UUID> {

    public Optional<Account> findByEmail(String email) {
        return find("email", Account.normaliseEmail(email)).firstResultOptional();
    }
}
