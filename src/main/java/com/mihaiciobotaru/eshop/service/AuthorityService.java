package com.mihaiciobotaru.eshop.service;

import com.mihaiciobotaru.eshop.models.Authority;
import com.mihaiciobotaru.eshop.enums.AuthorityEnum;
import com.mihaiciobotaru.eshop.models.User;
import com.mihaiciobotaru.eshop.repository.AuthorityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AuthorityService {

    private final AuthorityRepository authorityRepository;

    @Autowired
    public AuthorityService(AuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
    }

    public List<Authority> findAll() {
        return authorityRepository.findAll();
    }

    public Optional<Authority> findById(Long id) {
        return authorityRepository.findById(id);
    }

    public Authority save(Authority authority) {
        return authorityRepository.save(authority);
    }

    public Authority createAuthority(User user) {
        Authority authority = new Authority();
        authority.setAuthority(AuthorityEnum.ROLE_USER);
        authority.setUsername(user.getUsername());
        authority.setUser(user);
        return authorityRepository.save(authority);
    }

    public void deleteById(Long id) {
        authorityRepository.deleteById(id);
    }
}