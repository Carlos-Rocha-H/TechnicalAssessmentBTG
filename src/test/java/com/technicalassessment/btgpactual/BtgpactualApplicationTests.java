package com.technicalassessment.btgpactual;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SPRING_BOOT_INTEGRATION_TESTS", matches = "true")
class BtgpactualApplicationTests {

	@Test
	void contextLoads() {
	}

}
