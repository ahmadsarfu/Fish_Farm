package com.fishfarm.management.security;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class FarmTenantResolver implements CurrentTenantIdentifierResolver<Long>, HibernatePropertiesCustomizer {
    public Long resolveCurrentTenantIdentifier() { return FarmContext.farmId(); }
    public boolean validateExistingCurrentSessions() { return true; }
    public void customize(Map<String, Object> properties) { properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this); }
}
