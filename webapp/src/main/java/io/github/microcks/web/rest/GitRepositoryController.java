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
package io.github.microcks.web.rest;

import io.github.microcks.domain.GitRepositoryConfig;
import io.github.microcks.repository.GitRepositoryConfigRepository;
import io.github.microcks.service.GitRepositoryImportService;
import io.github.microcks.task.GitRepositoryImportTask;
import io.github.microacks.web.dto.GitRepositoryConfigDTO;
import io.github.microacks.web.dto.SyncResultDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for managing Git repository configurations.
 *
 * @author serge
 */
@RestController
@RequestMapping("/api")
public class GitRepositoryController {

   private static final Logger log = LoggerFactory.getLogger(GitRepositoryController.class);

   @Autowired
   private GitRepositoryImportService gitImportService;

   @Autowired(required = false)
   private GitRepositoryConfigRepository configRepository;

   @Autowired
   private GitRepositoryImportTask importTask;

   /**
    * Create a new Git repository configuration.
    * POST /api/git-repositories
    */
   @PostMapping(value = "/git-repositories")
   public ResponseEntity<GitRepositoryConfig> createConfig(@RequestBody GitRepositoryConfig config) {
      if (configRepository == null) {
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      log.info("Creating Git repository config: {}", config.getName());
      GitRepositoryConfig saved = configRepository.save(config);
      return ResponseEntity.status(HttpStatus.CREATED).body(saved);
   }

   /**
    * Get all Git repository configurations.
    * GET /api/git-repositories
    */
   @GetMapping(value = "/git-repositories")
   public ResponseEntity<List<GitRepositoryConfig>> getAllConfigs() {
      if (configRepository == null) {
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      List<GitRepositoryConfig> configs = configRepository.findAll();
      return ResponseEntity.ok(configs);
   }

   /**
    * Get a specific Git repository configuration.
    * GET /api/git-repositories/{id}
    */
   @GetMapping(value = "/git-repositories/{id}")
   public ResponseEntity<GitRepositoryConfig> getConfig(@PathVariable String id) {
      if (configRepository == null) {
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      return configRepository.findById(id)
              .map(ResponseEntity::ok)
              .orElse(ResponseEntity.notFound().build());
   }

   /**
    * Update a Git repository configuration.
    * PUT /api/git-repositories/{id}
    */
   @PutMapping(value = "/git-repositories/{id}")
   public ResponseEntity<GitRepositoryConfig> updateConfig(
           @PathVariable String id,
           @RequestBody GitRepositoryConfig config) {
      if (configRepository == null) {
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      if (!configRepository.existsById(id)) {
         return ResponseEntity.notFound().build();
      }

      config.setId(id);
      GitRepositoryConfig updated = configRepository.save(config);
      return ResponseEntity.ok(updated);
   }

   /**
    * Delete a Git repository configuration.
    * DELETE /api/git-repositories/{id}
    */
   @DeleteMapping(value = "/git-repositories/{id}")
   public ResponseEntity<Void> deleteConfig(@PathVariable String id) {
      if (configRepository == null) {
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      if (!configRepository.existsById(id)) {
         return ResponseEntity.notFound().build();
      }

      GitRepositoryConfig config = configRepository.findById(id).get();

      // Cleanup local files
      gitImportService.cleanupRepository(config);

      configRepository.deleteById(id);
      return ResponseEntity.noContent().build();
   }

   /**
    * Activate a Git repository configuration.
    * PUT /api/git-repositories/{id}/activate
    */
   @PutMapping(value = "/git-repositories/{id}/activate")
   public ResponseEntity<Void> activateConfig(@PathVariable String id) {
      if (configRepository == null) {
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      GitRepositoryConfig config = configRepository.findById(id).orElse(null);
      if (config == null) {
         return ResponseEntity.notFound().build();
      }

      config.setActive(true);
      configRepository.save(config);

      // Trigger initial import
      importTask.initialImport(id);

      return ResponseEntity.ok().build();
   }

   /**
    * Deactivate a Git repository configuration.
    * PUT /api/git-repositories/{id}/deactivate
    */
   @PutMapping(value = "/git-repositories/{id}/deactivate")
   public ResponseEntity<Void> deactivateConfig(@PathVariable String id) {
      if (configRepository == null) {
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      GitRepositoryConfig config = configRepository.findById(id).orElse(null);
      if (config == null) {
         return ResponseEntity.notFound().build();
      }

      config.setActive(false);
      configRepository.save(config);

      return ResponseEntity.ok().build();
   }

   /**
    * Force sync a Git repository now.
    * PUT /api/git-repositories/{id}/sync
    */
   @PutMapping(value = "/git-repositories/{id}/sync")
   public ResponseEntity<SyncResultDTO> syncNow(@PathVariable String id) {
      if (configRepository == null) {
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      GitRepositoryConfig config = configRepository.findById(id).orElse(null);
      if (config == null) {
         return ResponseEntity.notFound().build();
      }

      try {
         List<String> importedSpecs = gitImportService.forceSync(config);
         configRepository.save(config);

         SyncResultDTO result = new SyncResultDTO(
                 true,
                 importedSpecs.size() + " specs imported",
                 importedSpecs
         );
         return ResponseEntity.ok(result);

      } catch (Exception e) {
         log.error("Failed to sync repository: {}", config.getName(), e);
         SyncResultDTO result = new SyncResultDTO(
                 false,
                 e.getMessage(),
                 List.of()
         );
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
      }
   }

   /**
    * Get repository status.
    * GET /api/git-repositories/{id}/status
    */
   @GetMapping(value = "/git-repositories/{id}/status")
   public ResponseEntity<GitRepositoryConfig> getStatus(@PathVariable String id) {
      if (configRepository == null) {
         return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
      }

      GitRepositoryConfig config = configRepository.findById(id).orElse(null);
      if (config == null) {
         return ResponseEntity.notFound().build();
      }

      return ResponseEntity.ok(config);
   }
}
