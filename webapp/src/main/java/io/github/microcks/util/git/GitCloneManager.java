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
package io.github.microcks.util.git;

import io.github.microcks.domain.GitRepositoryConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manager for Git repository operations using JGit.
 * Handles cloning, pulling, and change detection for Git repositories.
 *
 * @author serge
 */
@Component
public class GitCloneManager {

   private static final Logger log = LoggerFactory.getLogger(GitCloneManager.class);
   private static final String TEMP_DIR_PREFIX = "microcks-git-";

   /**
    * Result of a Git clone operation.
    */
   public static class GitCloneResult {
      private final String localPath;
      private final String commitHash;

      public GitCloneResult(String localPath, String commitHash) {
         this.localPath = localPath;
         this.commitHash = commitHash;
      }

      public String getLocalPath() {
         return localPath;
      }

      public String getCommitHash() {
         return commitHash;
      }
   }

   /**
    * Result of a Git pull operation.
    */
   public static class GitPullResult {
      private final String commitHash;
      private final List<String> changedFiles;

      public GitPullResult(String commitHash, List<String> changedFiles) {
         this.commitHash = commitHash;
         this.changedFiles = changedFiles;
      }

      public String getCommitHash() {
         return commitHash;
      }

      public List<String> getChangedFiles() {
         return changedFiles;
      }
   }

   /**
    * Clone a Git repository to a temporary directory.
    *
    * @param config The Git repository configuration
    * @return GitCloneResult containing local path and commit hash
    * @throws GitAPIException if Git operation fails
    * @throws IOException if file operation fails
    */
   public GitCloneResult cloneRepository(GitRepositoryConfig config) throws GitAPIException, IOException {
      Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
      log.info("Cloning repository {} to {}", config.getRepositoryUrl(), tempDir);

      try (Git git = Git.cloneRepository()
              .setURI(config.getRepositoryUrl())
              .setDirectory(tempDir.toFile())
              .setBranch(config.getBranch() != null ? config.getBranch() : "main")
              .setDepth(1)  // Shallow clone for speed
              .setCredentialsProvider(buildCredentials(config))
              .call()) {

         String commitHash = git.getRepository()
                 .parseCommit(git.getRepository().resolve("HEAD"))
                 .getName();

         log.info("Repository cloned successfully. Commit hash: {}", commitHash);
         return new GitCloneResult(tempDir.toString(), commitHash);
      }
   }

   /**
    * Pull latest changes from a Git repository.
    *
    * @param repoPath Local path to the repository
    * @param config The Git repository configuration
    * @return GitPullResult containing new commit hash and changed files
    * @throws GitAPIException if Git operation fails
    * @throws IOException if file operation fails
    */
   public GitPullResult pullChanges(String repoPath, GitRepositoryConfig config) throws GitAPIException, IOException {
      File repoDir = new File(repoPath);
      if (!repoDir.exists()) {
         throw new IOException("Repository directory does not exist: " + repoPath);
      }

      log.info("Pulling changes from {}", config.getRepositoryUrl());

      try (Git git = Git.open(repoDir)) {
         // Get current commit before pull
         String oldCommitHash = git.getRepository()
                 .parseCommit(git.getRepository().resolve("HEAD"))
                 .getName();

         // Pull latest changes
         PullResult pullResult = git.pull()
                 .setCredentialsProvider(buildCredentials(config))
                 .call();

         if (!pullResult.isSuccessful()) {
            log.warn("Pull was not successful: {}", pullResult.toString());
         }

         // Get new commit hash
         String newCommitHash = git.getRepository()
                 .parseCommit(git.getRepository().resolve("HEAD"))
                 .getName();

         // Get changed files if commit hash changed
         List<String> changedFiles = List.of();
         if (!oldCommitHash.equals(newCommitHash)) {
            changedFiles = getChangedFiles(git, oldCommitHash, newCommitHash);
            log.info("Changes detected. {} files changed", changedFiles.size());
         } else {
            log.info("No changes detected");
         }

         return new GitPullResult(newCommitHash, changedFiles);
      }
   }

   /**
    * Get the current commit hash of a repository.
    *
    * @param repoPath Local path to the repository
    * @return Current commit hash
    * @throws IOException if file operation fails
    * @throws GitAPIException if Git operation fails
    */
   public String getCurrentCommitHash(String repoPath) throws IOException, GitAPIException {
      try (Git git = Git.open(new File(repoPath))) {
         return git.getRepository()
                 .parseCommit(git.getRepository().resolve("HEAD"))
                 .getName();
      }
   }

   /**
    * Delete a local repository directory.
    *
    * @param repoPath Path to the repository directory
    * @throws IOException if deletion fails
    */
   public void cleanupRepository(String repoPath) throws IOException {
      log.info("Cleaning up repository directory: {}", repoPath);
      Path path = Path.of(repoPath);
      if (Files.exists(path)) {
         Files.walk(path)
                 .sorted((a, b) -> -a.compareTo(b))  // Reverse order for deletion
                 .forEach(p -> {
                    try {
                       Files.delete(p);
                    } catch (IOException e) {
                       log.warn("Failed to delete: {}", p, e);
                    }
                 });
      }
   }

   /**
    * Build credentials provider for Git operations.
    *
    * @param config The Git repository configuration
    * @return CredentialsProvider or null if no authentication
    */
   private CredentialsProvider buildCredentials(GitRepositoryConfig config) {
      if (config.getSecretRef() != null && config.getSecretRef().getSecretId() != null) {
         String secret = config.getSecretRef().getSecretId();

         return switch (config.getAuthType()) {
            case TOKEN -> new UsernamePasswordCredentialsProvider(secret, "");  // Token as username
            case SSH_KEY -> {
               // For SSH key, we'd need SSHCredentialsProvider
               // This is a simplified version
               yield new UsernamePasswordCredentialsProvider("git", secret);
            }
            default -> null;
         };
      }
      return null;
   }

   /**
    * Get list of changed files between two commits.
    *
    * @param git Git instance
    * @param oldCommit Old commit hash
    * @param newCommit New commit hash
    * @return List of changed file paths
    * @throws IOException if file operation fails
    * @throws GitAPIException if Git operation fails
    */
   private List<String> getChangedFiles(Git git, String oldCommit, String newCommit)
           throws IOException, GitAPIException {
      Repository repository = git.getRepository();

      try (RevWalk revWalk = new RevWalk(repository)) {
         RevCommit oldCommitObj = revWalk.parseCommit(ObjectId.fromString(oldCommit));
         RevCommit newCommitObj = revWalk.parseCommit(ObjectId.fromString(newCommit));

         try (ObjectReader reader = repository.newObjectReader()) {
            AbstractTreeIterator oldTreeIter = prepareTreeParser(reader, oldCommitObj);
            AbstractTreeIterator newTreeIter = prepareTreeParser(reader, newCommitObj);

            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTreeIter)
                    .setNewTree(newTreeIter)
                    .call();

            return diffs.stream()
                    .map(d -> d.getNewPath())
                    .collect(Collectors.toList());
         }
      }
   }

   /**
    * Prepare a tree parser for diff operations.
    *
    * @param reader ObjectReader for the repository
    * @param commit Commit to parse
    * @return CanonicalTreeParser for the commit
    * @throws IOException if file operation fails
    */
   private AbstractTreeIterator prepareTreeParser(ObjectReader reader, RevCommit commit)
           throws IOException {
      CanonicalTreeParser parser = new CanonicalTreeParser();
      parser.reset(reader, commit.getTree());
      return parser;
   }
}
