package com.fishfarm.management.entity;

import com.fishfarm.management.security.FarmContext;
import jakarta.persistence.*;
import org.hibernate.annotations.TenantId;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class FarmOwnedEntity {
    @TenantId @Column(name = "farm_id", nullable = false, updatable = false)
    private Long farmId;
    @Column(updatable = false) private Long recordedBy;
    @Column(updatable = false) private LocalDateTime recordedAt;
    private Long lastChangedBy;
    private LocalDateTime lastChangedAt;
    public Long getFarmId() { return farmId; }
    public Long getRecordedBy() { return recordedBy; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public Long getLastChangedBy() { return lastChangedBy; }
    public LocalDateTime getLastChangedAt() { return lastChangedAt; }
    @PrePersist void createAudit() {
        recordedBy = FarmContext.userId(); recordedAt = LocalDateTime.now(java.time.ZoneOffset.UTC); updateAudit();
        legacyField("createdBy", recordedBy);
    }
    @PreUpdate void updateAudit() {
        lastChangedBy = FarmContext.userId(); lastChangedAt = LocalDateTime.now(java.time.ZoneOffset.UTC);
        legacyField("updatedBy", lastChangedBy);
    }
    private void legacyField(String name, Long value) {
        try { var field = getClass().getDeclaredField(name); field.setAccessible(true); field.set(this, value); }
        catch (NoSuchFieldException ignored) { /* Not every legacy entity has these fields. */ }
        catch (IllegalAccessException exception) { throw new IllegalStateException(exception); }
    }
}
