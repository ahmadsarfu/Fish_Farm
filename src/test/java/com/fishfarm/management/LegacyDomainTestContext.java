package com.fishfarm.management;
import com.fishfarm.management.security.FarmContext;
import org.junit.jupiter.api.*;
import org.springframework.test.context.transaction.*;
abstract class LegacyDomainTestContext {
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate testJdbc;
    private void ensureTestFarm() {
        testJdbc.update("INSERT INTO farms(id,name,legacy_claimable,active) SELECT 1,'Domain test farm',FALSE,TRUE WHERE NOT EXISTS(SELECT 1 FROM farms WHERE id=1)");
    }
    @BeforeTransaction void beforeTransaction(){ensureTestFarm();FarmContext.set(1L,1L,"OWNER");}
    @BeforeEach void beforeDomainTest(){ensureTestFarm();FarmContext.set(1L,1L,"OWNER");}
    @AfterEach void afterDomainTest(){FarmContext.clear();}
    @AfterTransaction void afterTransaction(){FarmContext.clear();}
}
