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
package io.github.microcks.domain;

import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Domain object representing a Git repository configuration for automatic spec import.
 * Git repository configs are responsible for periodically checking Git repositories
 * and updating service definitions when specifications change.
 *
 * @author serge
 */
public class GitRepositoryConfig {

   @Id
   private String id;
   private String name;
   private String repositoryUrl;
   private String branch;
   private List<String> specPaths;
   private String cronExpression;
   private boolean active = false;

   // Git tracking
   private String localPath;
   private String lastCommitHash;
   private Date lastImportDate;
   private String lastImportError;

   // Authentication
   private AuthType authType = AuthType.NONE;
   private SecretRef secretRef;

   // Service references
   private Set<ServiceRef> serviceRefs = new HashSet<>();

   // Metadata
   private Metadata metadata;

   public enum AuthType {
      NONE, TOKEN, SSH_KEY
   }

   // Getters and Setters

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getRepositoryUrl() {
      return repositoryUrl;
   }

   public void setRepositoryUrl(String repositoryUrl) {
      this.repositoryUrl = repositoryUrl;
   }

   public String getBranch() {
      return branch;
   }

   public void setBranch(String branch) {
      this.branch = branch;
   }

   public List<String> getSpecPaths() {
      return specPaths;
   }

   public void setSpecPaths(List<String> specPaths) {
      this.specPaths = specPaths;
   }

   public String getCronExpression() {
      return cronExpression;
   }

   public void setCronExpression(String cronExpression) {
      this.cronExpression = cronExpression;
   }

   public boolean isActive() {
      return active;
   }

   public void setActive(boolean active) {
      this.active = active;
   }

   public String getLocalPath() {
      return localPath;
   }

   public void setLocalPath(String localPath) {
      this.localPath = localPath;
   }

   public String getLastCommitHash() {
      return lastCommitHash;
   }

   public void setLastCommitHash(String lastCommitHash) {
      this.lastCommitHash = lastCommitHash;
   }

   public Date getLastImportDate() {
      return lastImportDate;
   }

   public void setLastImportDate(Date lastImportDate) {
      this.lastImportDate = lastImportDate;
   }

   public String getLastImportError() {
      return lastImportError;
   }

   public void setLastImportError(String lastImportError) {
      this.lastImportError = lastImportError;
   }

   public AuthType getAuthType() {
      return authType;
   }

   public void setAuthType(AuthType authType) {
      this.authType = authType;
   }

   public SecretRef getSecretRef() {
      return secretRef;
   }

   public void setSecretRef(SecretRef secretRef) {
      this.secretRef = secretRef;
   }

   public Secret getSecret() {
      if (secretRef != null && secretRef.getSecretId() != null) {
         return new Secret(secretRef.getSecretId(), secretRef.getName());
      }
      return null;
   }

   public Set<ServiceRef> getServiceRefs() {
      return serviceRefs;
   }

   public void setServiceRefs(Set<ServiceRef> serviceRefs) {
      this.serviceRefs = serviceRefs;
   }

   public Metadata getMetadata() {
      return metadata;
   }

   public void setMetadata(Metadata metadata) {
      this.metadata = metadata;
   }

   @Override
   public String toString() {
      return "GitRepositoryConfig{" +
              "id='" + id + '\'' +
              ", name='" + name + '\'' +
              ", repositoryUrl='" + repositoryUrl + '\'' +
              ", branch='" + branch + '\'' +
              ", specPaths=" + specPaths +
              ", cronExpression='" + cronExpression + '\'' +
              ", active=" + active +
              ", lastCommitHash='" + lastCommitHash + '\'' +
              ", lastImportDate=" + lastImportDate +
              ", lastImportError='" + lastImportError + '\'' +
              ", authType=" + authType +
              '}';
   }
}
