package com.orangebees.camel;

import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that the Spring context and the SFTP Camel route load correctly.
 *
 * @author <a href="mailto:john.yeary@orangebees.com">John Yeary</a>
 * @version 2.0.0
 * @since 2.0.0
 */
@SpringBootTest
class ApplicationTests {

	private static final Path WORK_DIR = createWorkDir();

	@Autowired
	private Environment env;

	@Autowired
	private CamelContext camelContext;

	@DynamicPropertySource
	static void overrideEnvironmentSpecificPaths(DynamicPropertyRegistry registry) {
		registry.add("idempotent.file", () -> WORK_DIR.resolve("idempotent.txt").toString());
		registry.add("file.output.uri", () -> WORK_DIR.resolve("output").toString());
	}

	private static Path createWorkDir() {
		try {
			Path dir = Files.createTempDirectory("sftp-route-test");
			dir.toFile().deleteOnExit();
			return dir;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	void contextLoads() {
	}

	@Test
	void idempotentFilePropertyResolves() {
		assertEquals(WORK_DIR.resolve("idempotent.txt").toString(), env.getProperty("idempotent.file"));
	}

	@Test
	void misspelledIdempotentFilePropertyIsNotDefined() {
		assertNull(env.getProperty("idempotentent.file"));
	}

	@Test
	void sftpRouteIsRegistered() {
		assertNotNull(camelContext.getRouteController().getRouteStatus("sftpRoute"));
		assertNotNull(camelContext.getRoute("sftpRoute"));
	}

}
