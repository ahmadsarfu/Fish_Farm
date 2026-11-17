package com.fishfarm.management;

import com.fishfarm.management.dto.FeedingSlotRequest;
import com.fishfarm.management.entity.*;
import com.fishfarm.management.enums.FeedingTime;
import com.fishfarm.management.repository.*;
import com.fishfarm.management.service.impl.FeedingDashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FeedingDashboardLegacyRateTests {
    @Test void zeroRateFromLegacySchemaReturnsActionableErrorWithoutWriting() {
        PondRepository ponds = mock(PondRepository.class);
        FeedTypeRepository types = mock(FeedTypeRepository.class);
        PondFeedConfigRepository configs = mock(PondFeedConfigRepository.class);
        DailyFeedRepository feeds = mock(DailyFeedRepository.class);
        Pond pond = new Pond(); pond.setId(1L);
        FeedType type = new FeedType(); type.setRatePerKg(BigDecimal.ZERO);
        PondFeedConfig config = new PondFeedConfig(); config.setPond(pond); config.setFeedType(type);
        config.setMorningQtyKg(new BigDecimal("2")); config.setEveningQtyKg(new BigDecimal("2"));
        LocalDate date = LocalDate.now();
        when(ponds.findForFeedingUpdate(1L)).thenReturn(Optional.of(pond));
        when(feeds.search(date, date, 1L)).thenReturn(List.of());
        when(configs.findByPondId(1L)).thenReturn(Optional.of(config));
        FeedingDashboardServiceImpl service = new FeedingDashboardServiceImpl(ponds, types, configs, feeds);
        assertThatThrownBy(() -> service.complete(1L, FeedingTime.MORNING, new FeedingSlotRequest(date, true)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Set a feed rate greater than zero");
        verify(feeds, never()).saveAndFlush(any());
    }
}
