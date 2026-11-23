package br.com.corpoario.users.service;

import br.com.corpoario.users.entity.Account;
import br.com.corpoario.users.entity.AccountRole;
import br.com.corpoario.users.exception.EmailAlreadyRegisteredException;
import br.com.corpoario.users.repository.AccountRepository;

import io.quarkus.elytron.security.common.BcryptUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.exception.ConstraintViolationException;

@ApplicationScoped
public class CustomerService {
    private static final int BCRYPT_COST = 12;
    private static final String EMAIL_UNIQUE_CONSTRAINT = "uk_user_accounts_email";

    @Inject
    AccountRepository repository;

    @Transactional
    public Account register(String email, String password) {
        // The role is fixed here so no request data can influence it.
        Account account = new Account(email, BcryptUtil.bcryptHash(password, BCRYPT_COST), AccountRole.CUSTOMER);

        try {
            // The unique constraint is the only race-free duplicate check.
            repository.persistAndFlush(account);
        } catch (ConstraintViolationException exception) {
            if (EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(exception.getConstraintName())) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }

        return account;
    }
}
