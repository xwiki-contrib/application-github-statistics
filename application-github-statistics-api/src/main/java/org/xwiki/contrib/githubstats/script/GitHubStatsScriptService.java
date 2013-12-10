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
package org.xwiki.contrib.githubstats.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.Repository;
import org.gitective.core.stat.UserCommitActivity;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.contrib.githubstats.Author;
import org.xwiki.contrib.githubstats.GitHubStatsManager;
import org.xwiki.contrib.githubstats.GitHubFactory;
import org.xwiki.contrib.githubstats.GitHubRepository;
import org.xwiki.contrib.githubstats.GitHubStatsException;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;

@Component
@Named("githubstats")
@Singleton
public class GitHubStatsScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String ERROR_KEY = "scriptservice.githubstats.error";

    @Inject
    private GitHubStatsManager manager;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    @Inject
    private GitHubFactory gitHubFactory;

    public Map<Author, Set<GitHubRepository>> findAllAuthors()
    {
        try {
            return this.manager.findAllAuthors();
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    public List<String> importAuthor(String authorId, String authorEmail, Collection<GitHubRepository> repositories,
        boolean overwrite)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importAuthor(authorId, authorEmail, repositories, overwrite);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to import a Git author"));
        }
        return result;
    }

    public List<String> importAllAuthorsFromGitHub(boolean overwrite) throws GitHubStatsException
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importAllAuthorsFromGitHub(this.gitHubFactory.createGitHub(), overwrite);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to import all author data from GitHub"));
        }
        return result;
    }

    public List<String> importAllAuthorsFromGitHub(String gitHubLogin, String gitHubAuthToken, boolean overwrite)
        throws GitHubStatsException
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importAllAuthorsFromGitHub(
                    this.gitHubFactory.createGitHub(gitHubLogin, gitHubAuthToken), overwrite);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to import all author data from GitHub"));
        }
        return result;
    }

    public List<String> importAuthorFromGitHub(String authorId, boolean overwrite)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importAuthorFromGitHub(this.gitHubFactory.createGitHub(), authorId, overwrite);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to import author data from GitHub"));
        }
        return result;
    }

    public List<String> importAuthorFromGitHub(String gitHubLogin, String gitHubAuthToken, String authorId,
        boolean overwrite)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importAuthorFromGitHub(
                    this.gitHubFactory.createGitHub(gitHubLogin, gitHubAuthToken), authorId, overwrite);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to import author data from GitHub"));
        }
        return result;
    }

    public List<String> createAuthorFromGitHub(String authorId, String fallbackEmail, boolean overwrite)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.createAuthorFromGitHub(this.gitHubFactory.createGitHub(), authorId,                    fallbackEmail, overwrite);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to create author from GitHub"));
        }
        return result;
    }

    public List<String> createAuthorFromGitHub(String gitHubLogin, String gitHubAuthToken, String authorId,
        String fallbackEmail, boolean overwrite)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.createAuthorFromGitHub(
                    this.gitHubFactory.createGitHub(gitHubLogin, gitHubAuthToken), authorId, fallbackEmail, overwrite);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to create author from GitHub"));
        }
        return result;
    }

    private List<BaseObject> convertToBaseObjects(List<com.xpn.xwiki.api.Object> authorToUpdateObjects)
    {
        List<BaseObject> baseObjects = new ArrayList<BaseObject>();
        for (com.xpn.xwiki.api.Object authorToUpdateObject : authorToUpdateObjects) {
            baseObjects.add(authorToUpdateObject.getXWikiObject());
        }
        return baseObjects;
    }

    public List<String> importAllCommittersFromGitHub()
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importAllCommittersFromGitHub(this.gitHubFactory.createGitHub());
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException(
                "User need to have Admin rights to import all committer data from GitHub"));
        }
        return result;
    }

    public List<String> importAllCommittersFromGitHub(String gitHubLogin, String gitHubAuthToken)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importAllCommittersFromGitHub(
                    this.gitHubFactory.createGitHub(gitHubLogin, gitHubAuthToken));
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException(
                "User need to have Admin rights to import all committer data from GitHub"));
        }
        return result;
    }

    public List<String> importCommittersFromGitHub(GitHubRepository repository)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importCommittersFromGitHub(this.gitHubFactory.createGitHub(), repository);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to import committer data from GitHub"));
        }
        return result;
    }

    public List<String> importCommittersFromGitHub(String gitHubLogin, String gitHubAuthToken,
        GitHubRepository repository)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importCommittersFromGitHub(
                    this.gitHubFactory.createGitHub(gitHubLogin, gitHubAuthToken), repository);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to import committer data from GitHub"));
        }
        return result;
    }

    public List<String> linkAuthors()
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.linkAuthors();
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to link authors together"));
        }
        return result;
    }

    public List<String> importAllAuthors(boolean overwrite)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importAllAuthors(overwrite);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to import all authors"));
        }
        return result;

    }

    public Map<Author, Map<String, ?>>  getAuthorsForRepositories(String repositoriesAsString)
    {
        try {
            Collection<GitHubRepository> repositories;
            if (StringUtils.isEmpty(repositoriesAsString)) {
                repositories = this.manager.getAllRepositoryURLs().keySet();
            } else {
                repositories = extractRepositoriesFromSingleString(repositoriesAsString);
            }
            return this.manager.getAuthorsForRepositories(repositories);
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    public List<String> importRepositoriesFromGitHub(String organizationId, boolean overwrite)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importRepositoriesFromGitHub(this.gitHubFactory.createGitHub(), organizationId,
                    overwrite);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to import repositories from GitHub"));
        }
        return result;
    }

    public List<String> importRepositoriesFromGitHub(String gitHubLogin, String gitHubAuthToken, String organizationId,
        boolean overwrite)
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.importRepositoriesFromGitHub(
                    this.gitHubFactory.createGitHub(gitHubLogin, gitHubAuthToken), organizationId, overwrite);
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to import repositories from GitHub"));
        }
        return result;
    }

    public List<String> deleteRepositories()
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.deleteRepositories();
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to delete imported repositories"));
        }
        return result;
    }

    public List<String> deleteAuthors()
    {
        List<String> result = null;
        if (hasPermission()) {
            try {
                result = this.manager.deleteAuthors();
            } catch (Exception e) {
                setError(e);
            }
        } else {
            setError(new GitHubStatsException("User need to have Admin rights to delete imported authors"));
        }
        return result;
    }

    public List<Repository> getAllRepositories()
    {
        try {
            return this.manager.getRepositories(this.manager.getAllRepositoryURLs());
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    public List<Repository> getRepositories(String repositoriesAsString)
    {
        if (StringUtils.isEmpty(repositoriesAsString)) {
            return getAllRepositories();
        }

        try {
            return this.manager.getRepositories(
                this.manager.getRepositoryURLs(
                    extractRepositoriesFromStrings(StringUtils.split(repositoriesAsString, ','))));
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    public Map<String, Map<String, Object>> aggregateCommitsPerAuthor(UserCommitActivity[] userCommitActivity, Map<Author,
        Map<String, Object>> authors)
    {
        return this.manager.aggregateCommitsPerAuthor(userCommitActivity, authors);
    }

    public Author buildAuthor(String authorId, String authorEmail)
    {
        return new Author(authorId, authorEmail);
    }

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return the last exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    private List<GitHubRepository> extractRepositoriesFromStrings(String... repositoriesAsStrings)
    {
        List<GitHubRepository> repositories = new ArrayList<GitHubRepository>();
        for (String repositoryAsString : repositoriesAsStrings) {
            String[] tokens = StringUtils.split(repositoryAsString, '/');
            GitHubRepository repository = new GitHubRepository(tokens[0], tokens[1]);
            repositories.add(repository);
        }
        return repositories;
    }

    private List<GitHubRepository> extractRepositoriesFromSingleString(String repositoriesAsStrings)
    {
        return extractRepositoriesFromStrings(StringUtils.split(repositoriesAsStrings, ','));
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param exception the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception exception)
    {
        this.execution.getContext().setProperty(ERROR_KEY, exception);
    }

    /**
     * @return true if the user is allowed to perform write operations or false otherwise. We allow Admins to perform
     *         these operations.
     */
    private boolean hasPermission()
    {
        XWikiContext xcontext = getXWikiContext();
        return xcontext.getWiki().getRightService().hasAdminRights(xcontext);
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
