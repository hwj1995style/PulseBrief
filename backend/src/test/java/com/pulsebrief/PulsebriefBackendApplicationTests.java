package com.pulsebrief;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class PulsebriefBackendApplicationTests {

	@Test
	void exposesMainEntrypoint() throws NoSuchMethodException {
		Method main = PulsebriefBackendApplication.class.getMethod("main", String[].class);

		assertThat(main).isNotNull();
	}

}
