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
package io.github.microcks.service;

import io.github.microcks.domain.GitRepositoryConfig;
import io.github.microcks.domain.Secret;
import io.github.microcks.domain.Service;
import io.github.microcks.util.git.GitCloneManager;
import io.github.microcks.util.git.GitCloneManager.GitCloneResult;
import io.github.microcks.util.git.GitCloneManager.GitPullResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Service for importing specifications from Git repositories.
 * Handles Git operations and delegates to ServiceService for actual imports.
 *
 * @author serge
 */
@Service
public class GitRepositoryImportService {

   private static final Logger log = LoggerFactory.getLogger(GitRepositoryImportService.class);

   @Autowired
   private GitCloneManager gitManager;

   @Autowired
   private ServiceService serviceService;

   @Autowired
   private ObjectMapper objectMapper;

   /**
    * Import changed specifications from a Git repository.
    *
    * @param config The Git repository configuration
    * @return List of imported specification paths
    */
   public List<String> importChangedSpecs(GitRepositoryConfig config) {
      List<String> importedSpecs = new ArrayList<>();

      try {
         // Get current commit hash
         String currentHash = getCurrentCommitHash(config);

         // If commit hash changed, import specs
         if (!currentHash.equals(config.getLastCommitHash())) {
            log.info("Changes detected in repository {}", config.getName());

            GitPullResult pullResult = gitManager.pullChanges(
                    config.getLocalPath(),
                    config
            );

            // Import changed spec files
            for (String specPath : config.getSpecPaths()) {
               if (pullResult.getChangedFiles().contains(specPath) ||
                       pullResult.getChangedFiles().isEmpty()) {
                  // Import spec if it changed or if it's first import
                  importSpec(config, specPath);
                  importedSpecs.add(specPath);
                  log.info("Imported spec: {}", specPath);
               }
            }

            // Update tracking info
            config.setLastCommitHash(pullResult.getCommitHash());
            config.setLastImportDate(new Date());
            config.setLastImportError(null);
         } else {
            log.info("No changes detected in repository {}", config.getName());
         }

      } catch (Exception e) {
         log.error("Failed to import specs from repository {}", config.getName(), e);
         config.setLastImportError(e.getMessage());
      }

      return importedSpecs;
   }

   /**
    * Get the current commit hash of a Git repository.
    * Clones repository if not already cloned locally.
    *
    * @param config The Git repository configuration
    * @return Current commit hash
    * @throws Exception if Git operation fails
    */
   public String getCurrentCommitHash(GitRepositoryConfig config) throws Exception {
      if (config.getLocalPath() == null || config.getLocalPath().isEmpty()) {
         // First time - clone the repository
         GitCloneResult cloneResult = gitManager.cloneRepository(config);
         config.setLocalPath(cloneResult.getLocalPath());
         config.setLastCommitHash(cloneResult.getCommitHash());
         log.info("Repository cloned to {}", cloneResult.getLocalPath());
         return cloneResult.getCommitHash();
      }

      // Repository already cloned - get current hash
      return gitManager.getCurrentCommitHash(config.getLocalPath());
   }

   /**
    * Import a single specification file.
    *
    * @param config The Git repository configuration
    * @param specPath Path to the specification file
    */
   private void importSpec(GitRepositoryConfig config, String specPath) {
      try {
         File specFile = new File(config.getLocalPath(), specPath);
         if (!specFile.exists()) {
            log.warn("Spec file does not exist: {}", specFile);
            return;
         }

         // Convert local file to file:// URL
         String fileUrl = specFile.toURI().toString();
         log.info("Importing spec from: {}", fileUrl);

         // Get secret for authentication if needed
         Secret secret = config.getSecret();

         // Import using ServiceService
         Service service = serviceService.importServiceDefinition(
                 fileUrl,           // repositoryUrl как file:// URL
                 secret,            // Secret для auth
                 false,             // disableSSLValidation
                 true               // mainArtifact
         );

         if (service != null) {
            log.info("Successfully imported service: {}", service.getName());
         } else {
            log.warn("Failed to import service from: {}", fileUrl);
         }

      } catch (Exception e) {
         log.error("Failed to import spec: {}", specPath, e);
         throw new RuntimeException("Failed to import spec: " + specPath, e);
      }
   }

   /**
    * Force sync all specifications from a Git repository.
    *
    * @param config The Git repository configuration
    * @return List of imported specification paths
    */
   public List<String> forceSync(GitRepositoryConfig config) {
      List<String> importedSpecs = new ArrayList<>();

      try {
         // Force pull latest changes
         GitPullResult pullResult = gitManager.pullChanges(
                 config.getLocalPath(),
                 config
         );

         // Import all configured specs regardless of changes
         for (String specPath : config.getSpecPaths()) {
            try {
               importSpec(config, specPath);
               importedSpecs.add(specPath);
            } catch (Exception e) {
               log.error("Failed to import spec during force sync: {}", specPath, e);
            }
         }

         // Update tracking info
         config.setLastCommitHash(pullResult.getCommitHash());
         config.setLastImportDate(new Date());
         config.setLastImportError(null);

      } catch (Exception e) {
         log.error("Failed to force sync repository {}", config.getName(), e);
         config.setLastImportError(e.getMessage());
      }

      return importedSpecs;
   }

   /**
    * Cleanup local repository files.
    *
    * @param config The Git repository configuration
    */
   public void cleanupRepository(GitRepositoryConfig config) {
      if (config.getLocalPath() != null) {
         try {
            gitManager.cleanupRepository(config.getLocalPath());
            config.setLocalPath(null);
            log.info("Cleaned up repository: {}", config.getName());
         } catch (Exception e) {
            log.error("Failed to cleanup repository {}", config.getName(), e);
         }
      }
   }
}
