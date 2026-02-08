/*
 * Copyright The Microcks Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.microcks.task;

import io.github.microcks.domain.GitRepositoryConfig;
import io.github.microcks.repository.GitRepositoryConfigRepository;
import io.github.microcks.service.GitRepositoryImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Scheduled task for syncing Git repositories.
 * Runs periodically to check for updates in configured Git repositories
 * and import changed specifications.
 *
 * @author serge
 */
@Component
public class GitRepositoryImportTask {

   private static final Logger log = LoggerFactory.getLogger(GitRepositoryImportTask.class);

   @Autowired
   private GitRepositoryImportService gitImportService;

   @Autowired(required = false)
   private GitRepositoryConfigRepository configRepository;

   /**
    * Scheduled task to sync all active Git repositories.
    * Runs every 3 minutes by default (configurable via application properties).
    */
   @Scheduled(cron = "${microcks.import.git.cron:0 0/3 * * * *}")
   public void syncGitRepositories() {
      if (configRepository == null) {
         log.debug("GitRepositoryConfigRepository not available - skipping Git sync");
         return;
      }

      log.info("Starting Git repositories sync at {}", new Date());

      try {
         List<GitRepositoryConfig> activeConfigs = configRepository.findByActiveTrue();

         if (activeConfigs.isEmpty()) {
            log.debug("No active Git repository configurations found");
            return;
         }

         log.info("Found {} active Git repository configurations", activeConfigs.size());

         int successCount = 0;
         int failureCount = 0;

         for (GitRepositoryConfig config : activeConfigs) {
            try {
               syncRepository(config);
               successCount++;
            } catch (Exception e) {
               log.error("Failed to sync repository: {}", config.getName(), e);
               config.setLastImportError(e.getMessage());
               configRepository.save(config);
               failureCount++;
            }
         }

         log.info("Git sync completed. Success: {}, Failures: {}", successCount, failureCount);

      } catch (Exception e) {
         log.error("Error during Git repositories sync", e);
      }
   }

   /**
    * Sync a single Git repository.
    *
    * @param config The Git repository configuration
    */
   private void syncRepository(GitRepositoryConfig config) {
      log.debug("Syncing repository: {}", config.getName());

      try {
         // Get current commit hash
         String currentHash = gitImportService.getCurrentCommitHash(config);

         // Check if repository has changed
         if (!currentHash.equals(config.getLastCommitHash())) {
            log.info("Changes detected in repository '{}'. Importing specs...", config.getName());

            // Import changed specs
            var importedSpecs = gitImportService.importChangedSpecs(config);

            log.info("Imported {} specs from repository '{}'", importedSpecs.size(), config.getName());

            // Save updated config
            configRepository.save(config);
         } else {
            log.debug("No changes detected in repository: {}", config.getName());
         }

      } catch (Exception e) {
         log.error("Error syncing repository: {}", config.getName(), e);
         config.setLastImportError(e.getMessage());
         configRepository.save(config);
         throw e;
      }
   }

   /**
    * Initial import for newly activated repositories.
    * This can be triggered manually when a repository is first activated.
    *
    * @param configId The ID of the repository configuration
    */
   public void initialImport(String configId) {
      if (configRepository == null) {
         log.warn("GitRepositoryConfigRepository not available");
         return;
      }

      log.info("Performing initial import for repository config: {}", configId);

      GitRepositoryConfig config = configRepository.findById(configId).orElse(null);
      if (config == null) {
         log.warn("Repository config not found: {}", configId);
         return;
      }

      try {
         // Force sync on first activation
         var importedSpecs = gitImportService.forceSync(config);
         log.info("Initial import completed. Imported {} specs", importedSpecs.size());

         configRepository.save(config);

      } catch (Exception e) {
         log.error("Initial import failed for repository: {}", config.getName(), e);
         config.setLastImportError(e.getMessage());
         configRepository.save(config);
      }
   }
}
