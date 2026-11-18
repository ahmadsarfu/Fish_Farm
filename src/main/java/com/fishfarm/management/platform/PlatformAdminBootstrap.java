package com.fishfarm.management.platform;
import com.fishfarm.management.entity.AppAccount;
import com.fishfarm.management.repository.AppAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
@Component @ConditionalOnProperty(name="app.platform-bootstrap-enabled",havingValue="true") @RequiredArgsConstructor
public class PlatformAdminBootstrap implements ApplicationRunner {
    private final AppAccountRepository accounts;
    private final PasswordEncoder encoder;
    private final ContactIdentity contacts;
    private final PlatformMfa mfa;
    @Value("${app.platform-admin-username:}") private String username;
    @Value("${app.platform-admin-password:}") private String password;
    @Override @Transactional public void run(ApplicationArguments arguments) {
        if(accounts.existsByPlatformAdminTrue())return;
        if(!username.matches("[A-Za-z0-9._@+\\-]{3,120}") || password.length()<14 || password.getBytes(StandardCharsets.UTF_8).length>72 || !mfa.configured())
            throw new IllegalStateException("Platform bootstrap requires an admin username, password of 14–72 UTF-8 bytes and a Base32 authenticator secret of at least 32 characters. No default admin is created.");
        String name=username.toLowerCase(Locale.ROOT);contacts.requireUsernameAvailable(name);
        AppAccount account=new AppAccount();account.setUsername(name);account.setDisplayName("Platform Owner");account.setPasswordHash(encoder.encode(password));account.setPlatformAdmin(true);accounts.saveAndFlush(account);
    }
}
