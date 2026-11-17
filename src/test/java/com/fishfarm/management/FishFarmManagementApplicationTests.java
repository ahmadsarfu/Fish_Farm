package com.fishfarm.management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties={"app.business-zone=UTC"})
class FishFarmManagementApplicationTests extends LegacyDomainTestContext {

	@Test
	void contextLoads() {
	}

}
