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
package io.github.microcks.repository;

import io.github.microcks.domain.GitRepositoryConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * MongoDB repository for GitRepositoryConfig entities.
 *
 * @author serge
 */
public interface GitRepositoryConfigRepository extends MongoRepository<GitRepositoryConfig, String> {

   /**
    * Find all active Git repository configurations.
    *
    * @return List of active configurations
    */
   List<GitRepositoryConfig> findByActiveTrue();

   /**
    * Find by name.
    *
    * @param name The repository name
    * @return The configuration or null
    */
   GitRepositoryConfig findByName(String name);

   /**
    * Find by repository URL.
    *
    * @param repositoryUrl The repository URL
    * @return The configuration or null
    */
   GitRepositoryConfig findByRepositoryUrl(String repositoryUrl);

   /**
    * Check if a repository URL already exists.
    *
    * @param repositoryUrl The repository URL
    * @return true if exists
    */
   boolean existsByRepositoryUrl(String repositoryUrl);
}
