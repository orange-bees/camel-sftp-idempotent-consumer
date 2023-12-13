/*
 * Copyright 2023 Orange Bees.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orangebees.camel.route;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.idempotent.IdempotentConsumer;
import org.apache.camel.support.processor.idempotent.FileIdempotentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * An example SFTP Route that uses a file based {@link IdempotentConsumer} to
 * keep track of processed files.
 *
 * @author <a href="mailto:john.yeary@orangebees.com">John Yeary</a>
 * @version 1.0.0
 */
@Component
@Slf4j
public class SFTPRoute extends RouteBuilder {

    @Autowired
    private Environment env;

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure() throws Exception {
        from("sftp:localhost:2222/"
                + "?username={{sftp.username}}"
                + "&password={{sftp.password}}"
                + "&noop=true"
                + "&jschLoggingLevel={{sftp.jschLoggingLevel}}"
                + "&timeUnit=SECONDS"
                + "&filterFile=$simple{file:ext} == {{file.filter}}"
                + "&delay=10"
                + "&useUserKnownHostsFile=false")
                .routeId("sftpRoute")
                // Use env to fetch the idempotent.file name since {{}} does not work with File in this case.
                .idempotentConsumer(header("CamelFileName"),
                        FileIdempotentRepository.fileIdempotentRepository(new File(env.getProperty("idempotentent.file"))))
                .id("fileIdempotentRepository")
                .process((Exchange exchange) -> {
                    log.info("This file is being processed the first time -- {}", exchange.getIn().getHeader("CamelFileName"));
                })
                .id("fileProcessor")
                .log("${body}")
                .id("bodyLogger")
                .to("file://{{file.output.uri}}")
                .id("toFile")
                .end();

    }

}
