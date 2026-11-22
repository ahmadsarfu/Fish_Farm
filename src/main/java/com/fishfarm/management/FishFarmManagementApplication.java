package com.fishfarm.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FishFarmManagementApplication {

	public static void main(String[] args) {
		// Legacy LocalDateTime audit columns are stored as UTC; business dates use the IST Clock.
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
		SpringApplication.run(FishFarmManagementApplication.class, args);
	}

}
