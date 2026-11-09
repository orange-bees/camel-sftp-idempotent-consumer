package com.orangebees.camel.route;

import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.FileIdempotentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@link FileIdempotentRepository} behavior {@link SFTPRoute} relies on:
 * a file name is accepted once and the state is persisted across restarts.
 *
 * @author <a href="mailto:john.yeary@orangebees.com">John Yeary</a>
 * @version 2.0.0
 * @since 2.0.0
 */
class IdempotentRepositoryTest {

	@TempDir
	Path tempDir;

	@Test
	void secondAddOfSameKeyIsRejected() throws Exception {
		File store = tempDir.resolve("repo.txt").toFile();
		IdempotentRepository repository = FileIdempotentRepository.fileIdempotentRepository(store);
		repository.start();
		try {
			assertTrue(repository.add("a.java"));
			assertFalse(repository.add("a.java"));
			assertTrue(repository.add("b.java"));
		} finally {
			repository.stop();
		}
	}

	@Test
	void stateSurvivesRestart() throws Exception {
		File store = tempDir.resolve("repo.txt").toFile();

		IdempotentRepository first = FileIdempotentRepository.fileIdempotentRepository(store);
		first.start();
		assertTrue(first.add("a.java"));
		first.stop();
		assertTrue(Files.readString(store.toPath()).contains("a.java"));

		IdempotentRepository second = FileIdempotentRepository.fileIdempotentRepository(store);
		second.start();
		try {
			assertFalse(second.add("a.java"));
			assertTrue(second.add("c.java"));
		} finally {
			second.stop();
		}
	}

}
