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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.lib.Repository;
import org.gitective.core.stat.UserCommitActivity;
import org.kohsuke.github.GitHub;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

@Role
@Unstable
public interface GitHubStatsManager
{
    List<String> importRepositoriesFromGitHub(GitHub gitHub, String organizationId, boolean overwrite)
        throws GitHubStatsException;

    List<String> deleteRepositories() throws GitHubStatsException;

    /**
     * @return all authors from all defined organizations and repositories found in the underlying Git repository
     */
    Map<Author, Set<GitHubRepository>> findAllAuthors() throws GitHubStatsException;

    List<String> importAuthor(String authorId, String authorEmail, Collection<GitHubRepository> repositories,
        boolean overwrite) throws GitHubStatsException;

    List<String> importAuthorFromGitHub(GitHub gitHub, String authorId, String emailAddress, boolean overwrite)
        throws GitHubStatsException;

    List<String> createAuthorFromGitHub(GitHub gitHub, String authorId, String fallbackEmail, boolean overwrite)
        throws GitHubStatsException;

    List<String> importAllAuthorsFromGitHub(GitHub gitHub, boolean overwrite) throws GitHubStatsException;

    List<String> importCommittersFromGitHub(GitHub gitHub, GitHubRepository repository) throws GitHubStatsException;

    List<String> importAllCommittersFromGitHub(GitHub gitHub) throws GitHubStatsException;

    List<String> linkAuthors() throws GitHubStatsException;

    List<String> importAllAuthors(boolean overwrite) throws GitHubStatsException;

    List<String> deleteAuthors() throws GitHubStatsException;

    Map<Author, Map<String, ?>> getAuthorsForRepositories(Collection<GitHubRepository> repositories)
        throws GitHubStatsException;

    /**
     * The format of the repositories is: {@code (organization name)/(repository name)} where {@code organization name}
     * can be {@code *} to signify all repositories for this organization. For example:
     * <ul>
     *   <li>xwiki/*</li>
     *   <li>xwiki-contrib/application-github-statistics</li>
     * </ul>
     */
    Map<GitHubRepository, String> getRepositoryURLs(String... repositoryAsStrings) throws GitHubStatsException;

    Map<GitHubRepository, String> getRepositoryURLs(List<GitHubRepository> repositories) throws GitHubStatsException;

    List<Repository> getRepositories(Map<GitHubRepository, String> repositories);

    Map<String, Map<String, Object>> aggregateCommitsPerAuthor(UserCommitActivity[] userCommitActivity, Map<Author,
        Map<String, Object>> authors);
}
