/*-
 * #%L
 * TailoringExpert
 * %%
 * Copyright (C) 2022 Michael Bädorf and others
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package eu.tailoringexpert.migration;

import eu.tailoringexpert.catalog.JPACatalogServiceRepositoryMapper;
import eu.tailoringexpert.catalog.JPACatalogServiceRepositoryMapperGenerated;
import eu.tailoringexpert.domain.BaseCatalogChapterEntity;
import eu.tailoringexpert.domain.BaseCatalogEntity;
import eu.tailoringexpert.repository.BaseCatalogRepository;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static java.util.stream.Stream.of;


@SpringBootApplication(
    exclude = {
        ThymeleafAutoConfiguration.class,
        LiquibaseAutoConfiguration.class
    }
)
@EnableJpaRepositories(
    basePackages = {
        "eu.tailoringexpert.repository"
    }
)
@EntityScan("eu.tailoringexpert.domain")
@Log4j2
/**
 * @see {https://github.com/tailoringexpert/platform/issues/245}
 */
public class BaseRequirementNumberMigrationClient {


    public static void main(String[] args) {
        SpringApplication.run(BaseRequirementNumberMigrationClient.class, args);
    }

    @Bean
    JPACatalogServiceRepositoryMapper mapper() {
        return new JPACatalogServiceRepositoryMapperGenerated();
    }

    @Bean
    CommandLineRunner runner(@NonNull BaseCatalogRepository repository, @NonNull JPACatalogServiceRepositoryMapper mapper) {
        return args -> {
            log.info("start adding numbers to baserequirements");
            repository.findAll()
                .forEach(catalog -> {
                    log.info("start " + catalog.getVersion());
                    catalog.getToc().getChapters()
                        .stream()
                        .flatMap(this::allChapters)
                        .forEach(chapter -> chapter.getRequirements()
                            .forEach(requirement -> requirement.setNumber(chapter.getNumber() + "." + requirement.getPosition()))
                        );
                    repository.save(catalog);
                    log.info("finish " + catalog.getVersion());
                });
            log.info("finish adding numbers to baserequirements");
        };
    }

    public Stream<BaseCatalogChapterEntity> allChapters(BaseCatalogChapterEntity chapter) {
        return Stream.concat(
            of(chapter),
            nonNull(chapter.getChapters()) ? chapter.getChapters().stream().flatMap(this::allChapters) : Stream.empty());

    }

}
