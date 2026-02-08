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
package io.github.microcks.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for Git repository import feature.
 * Enables scheduled tasks and configures Git import properties.
 *
 * @author serge
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "microcks.import.git.enabled", havingValue = "true", matchIfMissing = false)
public class GitRepositoryImportConfiguration {

   private static final Logger log = LoggerFactory.getLogger(GitRepositoryImportConfiguration.class);

   public GitRepositoryImportConfiguration() {
      log.info("Git repository import feature is enabled");
   }

   /**
    * Properties for Git repository import.
    */
   @Bean
   @ConfigurationProperties(prefix = "microcks.import.git")
   public GitImportProperties gitImportProperties() {
      return new GitImportProperties();
   }

   /**
    * Properties class for Git import configuration.
    */
   public static class GitImportProperties {
      private boolean enabled = false;
      private String url;
      private String branch = "main";
      private String specPaths;
      private String cron = "0 0/3 * * * *";  // Every 3 minutes
      private String username;
      private String password;
      private String token;
      private String privateKeyPath;

      public boolean isEnabled() {
         return enabled;
      }

      public void setEnabled(boolean enabled) {
         this.enabled = enabled;
      }

      public String getUrl() {
         return url;
      }

      public void setUrl(String url) {
         this.url = url;
      }

      public String getBranch() {
         return branch;
      }

      public void setBranch(String branch) {
         this.branch = branch;
      }

      public String getSpecPaths() {
         return specPaths;
      }

      public void setSpecPaths(String specPaths) {
         this.specPaths = specPaths;
      }

      public String getCron() {
         return cron;
      }

      public void setCron(String cron) {
         this.cron = cron;
      }

      public String getUsername() {
         return username;
      }

      public void setUsername(String username) {
         this.username = username;
      }

      public String getPassword() {
         return password;
      }

      public void setPassword(String password) {
         this.password = password;
      }

      public String getToken() {
         return token;
      }

      public void setToken(String token) {
         this.token = token;
      }

      public String getPrivateKeyPath() {
         return privateKeyPath;
      }

      public void setPrivateKeyPath(String privateKeyPath) {
         this.privateKeyPath = privateKeyPath;
      }
   }
}
