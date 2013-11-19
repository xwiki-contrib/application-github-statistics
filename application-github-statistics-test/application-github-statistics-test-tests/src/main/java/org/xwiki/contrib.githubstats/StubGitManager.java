/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.githubstats;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.git.GitHelper;
import org.xwiki.git.internal.DefaultGitManager;

@Component
@Singleton
public class StubGitManager extends DefaultGitManager
{
    @Inject
    private Environment environment;

    @Override
    public Repository getRepository(String repositoryURI, String localDirectoryName)
    {
        Repository gitRepository;

        // Create a fake local Git repository
        try {
            // Initialize a Git Repository with some test data in it.
            GitHelper gitHelper = new GitHelper(this.environment);

            boolean exists = gitHelper.exists(localDirectoryName);

            gitRepository = gitHelper.createGitTestRepository(localDirectoryName);

            // Add test data but only if the repo doesn't already exist
            if (!exists) {
                // Scenario 1:
                // - 3 authors representing the same user
                // - 2 different ids
                // - 2 different emails
                // - 6 repos
                if (localDirectoryName.equals("organization1/repository1")) {
                    addCommit("author1", "author1@doe.com", "test1.txt", gitRepository, gitHelper);
                    addCommit("author2", "author1@doe.com", "test2.txt", gitRepository, gitHelper);
                }
                if (localDirectoryName.equals("organization1/repository2")) {
                    addCommit("author1", "author1@doe.com", "test1.txt", gitRepository, gitHelper);
                    addCommit("author2", "author1@doe.com", "test2.txt", gitRepository, gitHelper);
                }
                if (localDirectoryName.equals("organization1/repository3")) {
                    addCommit("author1", "author1@doe.com", "test1.txt", gitRepository, gitHelper);
                }
                if (localDirectoryName.equals("organization1/repository4")) {
                    addCommit("author1", "author1@doe.com", "test1.txt", gitRepository, gitHelper);
                    addCommit("author2", "author1@doe.com", "test2.txt", gitRepository, gitHelper);
                }
                if (localDirectoryName.equals("organization1/repository5")) {
                    addCommit("author1", "author1@doe.com", "test1.txt", gitRepository, gitHelper);
                    addCommit("author2", "author1@doe.com", "test2.txt", gitRepository, gitHelper);
                }
                if (localDirectoryName.equals("organization1/repository6")) {
                    addCommit("author1", "author1@doe.com", "test1.txt", gitRepository, gitHelper);
                    addCommit("author2", "author1@doe.com", "test2.txt", gitRepository, gitHelper);
                    addCommit("author2", "author2@doe.com", "test3.txt", gitRepository, gitHelper);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to setup Git test repository for [%s] in [%s]",
                repositoryURI, localDirectoryName), e);
        }

        return gitRepository;
    }

    private void addCommit(String authorId, String emailId, String file, Repository gitRepository, GitHelper gitHelper)
        throws Exception
    {
        gitHelper.add(gitRepository.getDirectory(), file, "test content",
            new PersonIdent(authorId, emailId), new PersonIdent(authorId, emailId), "commit");
    }
}
