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
package org.xwiki.contrib.githubstats.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.gitective.core.stat.UserCommitActivity;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.contrib.githubstats.Author;
import org.xwiki.contrib.githubstats.GitHubStatsManager;
import org.xwiki.contrib.githubstats.GitHubRepository;
import org.xwiki.contrib.githubstats.GitHubStatsException;
import org.xwiki.git.GitManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
@Singleton
public class DefaultGitHubStatsManager implements GitHubStatsManager
{
    /**
     * Space where the GitHubStats application pages are located.
     */
    String SPACE = "GitHubStats";

    /**
     * GitHubStats.WebHome.
     */
    EntityReference PARENT = new EntityReference("WebHome", EntityType.DOCUMENT,
        new EntityReference(SPACE, EntityType.SPACE));

    /**
     * GitHubStats.AuthorClass xclass.
     */
    EntityReference AUTHOR_CLASS = new EntityReference("AuthorClass", EntityType.DOCUMENT,
        new EntityReference(SPACE, EntityType.SPACE));

    /**
     * GitHubStats.AuthorRepositoryClass xclass.
     */
    EntityReference AUTHOR_REPOSITORY_CLASS = new EntityReference("AuthorRepositoryClass", EntityType.DOCUMENT,
        new EntityReference(SPACE, EntityType.SPACE));

    /**
     * GitHubStats.RepositoryClass xclass.
     */
    EntityReference REPOSITORY_CLASS = new EntityReference("RepositoryClass", EntityType.DOCUMENT,
        new EntityReference(SPACE, EntityType.SPACE));

    @Inject
    private Logger logger;

    @Inject
    private GitManager gitManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    private EntityReferenceSerializer<String> defaultSerializer;

    @Inject
    private Execution execution;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @Override
    public Map<Author, Set<GitHubRepository>> findAllAuthors() throws GitHubStatsException
    {
        // For each repository found, find all authors that have ever contributed code.
        Map<Author, Set<GitHubRepository>> authors = new HashMap<Author, Set<GitHubRepository>>();
        for (Map.Entry<GitHubRepository, String> entry : getAllRepositoryURLs().entrySet()) {
            try {
                Repository repository = getRepository(entry.getValue(), entry.getKey());
                for (PersonIdent personIdent : this.gitManager.findAuthors(Arrays.asList(repository))) {
                    Author author = new Author(personIdent.getName(), personIdent.getEmailAddress());
                    Set<GitHubRepository> reposForAuthor = authors.get(author);
                    if (reposForAuthor == null) {
                        reposForAuthor = new HashSet<GitHubRepository>();
                        authors.put(author, reposForAuthor);
                    }
                    reposForAuthor.add(
                        new GitHubRepository(entry.getKey().getOrganizationId(), entry.getKey().getRepositoryId()));
                }
            } catch (Exception e) {
                // A repository can fail to be cloned/updated (for example if it's empty), in which case we simply
                // ignore it.
            }
        }

        return authors;
    }

    @Override
    public List<String> importAuthor(String authorId, String authorEmail, Collection<GitHubRepository> repositories,
        boolean overwrite) throws GitHubStatsException
    {
        List<String> importedUsers = new ArrayList<String>();
        BaseObject authorObject = importAuthorInternal(authorId, authorEmail, repositories, overwrite);
        if (authorObject != null) {
            String authorAsString = String.format("%s <%s>", authorId, authorEmail);
            importedUsers.add(authorAsString);
        }
        return importedUsers;
    }

    private BaseObject importAuthorInternal(String authorId, String authorEmail, Collection<GitHubRepository> repositories,
        boolean overwrite) throws GitHubStatsException
    {
        BaseObject authorObject = null;

        String authorAsString = String.format("%s <%s>", authorId, authorEmail);

        // Create a page based on the name + email (for unicity) and fill it with basic data
        try {
            // Get author document or create a new one if it doesn't exist
            XWikiDocument authorDocument = getAuthorDocument(authorId, authorEmail);
            if (authorDocument.isNew() || overwrite) {
                authorDocument.setTitle(String.format("Author [%s]", authorAsString));
                authorDocument.setParentReference(PARENT);
                authorDocument.setHidden(true);
                // If there's an existing AuthorClass xobject then reuse it, otherwise create it
                authorObject = createAuthorClass(authorDocument);
                authorObject.setStringValue("id", authorId);
                authorObject.setStringValue("email", authorEmail);
                // Merge AuthorRepositoryClass xobjects by removing all existing objects and adding new ones
                authorDocument.removeXObjects(AUTHOR_REPOSITORY_CLASS);
                XWikiContext xcontext = getXWikiContext();
                addAuthorRepositoryObjects(authorDocument, repositories, xcontext);
                // Save modifications
                xcontext.getWiki().saveDocument(authorDocument, "Imported author from Git", true, xcontext);
            }
        } catch (XWikiException e) {
            throw new GitHubStatsException(String.format("Failed to create or update author document for [%s]",
                authorAsString), e);
        }

        return authorObject;
    }

    @Override
    public List<String> importAllAuthors(boolean overwrite) throws GitHubStatsException
    {
        List<String> importedUsers = new ArrayList<String>();
        Map<Author, Set<GitHubRepository>> authors = findAllAuthors();
        for (Map.Entry<Author, Set<GitHubRepository>> entry : authors.entrySet()) {
            Author author = entry.getKey();
            Set<GitHubRepository> repositories = entry.getValue();
            importedUsers.addAll(
                importAuthor(author.getId(), author.getEmail(), repositories, overwrite));
        }
        return importedUsers;
    }

    @Override
    public List<String> importAllAuthorsFromGitHub(GitHub gitHub, boolean overwrite) throws GitHubStatsException
    {
        List<String> updatedUsers = new ArrayList<String>();

        // Find all authors already imported so that for each of them we look for more data on GitHub.
        try {
            List<BaseObject> authorObjects = getAuthorObjectsForQuery(String.format(
                ", doc.object(%s) as author", this.defaultSerializer.serialize(AUTHOR_CLASS)));
            for (BaseObject authorObject : authorObjects) {
                try {
                    // Only import if there are fields not set or if overwrite is true. This is to improve performances
                    // since we need to call GitHub for each author existing in XWiki.
                    String avatar = authorObject.getStringValue("avatar");
                    String name = authorObject.getStringValue("name");
                    if (overwrite || StringUtils.isEmpty(avatar) || StringUtils.isEmpty(name)) {
                        String userId = authorObject.getStringValue("id");
                        String emailAddress = authorObject.getStringValue("email");
                        updatedUsers.addAll(importAuthorFromGitHub(gitHub, userId, emailAddress, overwrite));
                    }
                } catch (GitHubStatsException e) {
                    // Failed to import the user. This is usually because the user doesn't exist but the GitHub API
                    // we're using doesn't let us make the difference between a non existent users and a failure to
                    // retrieve the user's data...
                    // Thus we simply skip this user and continue...
                }
            }
        } catch (Exception e) {
            throw new GitHubStatsException("Failed to import all authors data from GitHub", e);
        }

        return updatedUsers;
    }

    private List<String> importAuthorFromGitHub(GHUser user, List<BaseObject> authorToUpdateObjects,
        boolean overwrite) throws GitHubStatsException
    {
        List<String> importedUsers = new ArrayList<String>();
        try {
            XWikiContext xcontext = getXWikiContext();
            for (BaseObject authorToUpdateObject : authorToUpdateObjects) {
                boolean modified = false;
                String currentName = authorToUpdateObject.getStringValue("name");
                if (StringUtils.isEmpty(currentName) || overwrite) {
                    authorToUpdateObject.setStringValue("name", user.getName());
                    modified = true;
                }
                String currentAvatar = authorToUpdateObject.getStringValue("avatar");
                if (StringUtils.isEmpty(currentAvatar) || overwrite) {
                    authorToUpdateObject.setStringValue("avatar", user.getAvatarUrl());
                    modified = true;
                }
                String currentProfileURL = authorToUpdateObject.getStringValue("profileurl");
                if (StringUtils.isEmpty(currentProfileURL) || overwrite) {
                    // TODO: There's currently no way to get the User HTML URL,
                    // See https://github.com/kohsuke/github-api/issues/52
                    // matchingAuthorObject.setStringValue("profileurl", user.get...);
                    // modified = true;
                }
                String currentCompany = authorToUpdateObject.getStringValue("company");
                if (StringUtils.isEmpty(currentCompany) || overwrite) {
                    authorToUpdateObject.setStringValue("company", user.getCompany());
                    modified = true;
                }
                // Save modifications if any
                if (modified) {
                    xcontext.getWiki().saveDocument(authorToUpdateObject.getOwnerDocument(),
                        "Imported user data from GitHub", true, xcontext);
                    importedUsers.add(authorToUpdateObject.getOwnerDocument().getDocumentReference().getName());
                }
            }
        } catch (Exception e) {
            throw new GitHubStatsException("Failed to import author data from GitHub", e);
        }

        return importedUsers;
    }

    private List<String> importAuthorFromGitHub(GitHub gitHub, String authorId, String emailAddress,
        List<BaseObject> authorToUpdateObjects, boolean overwrite) throws GitHubStatsException
    {
        List<String> result = Collections.emptyList();

        // Load the XWiki page corresponding to that user and fill the data.
        try {
            GHUser matchinguser = locateUserInGitHub(gitHub, authorId, emailAddress);
            if (matchinguser != null) {
                result = importAuthorFromGitHub(matchinguser, authorToUpdateObjects, overwrite);
            }
        } catch (Exception e) {
            throw new GitHubStatsException("Failed to import author data from GitHub", e);
        }

        return result;
    }

    private GHUser locateUserInGitHub(GitHub gitHub, String authorId, String emailAddress)
    {
        try {
            // Search for a user with the passed login
            // Note: We don't use "gitHub.getUser(authorId)" because if the authorId is a simple one (like "Gabriela")
            // then it's very likely that it'll return the wrong user. Doing a search is likely to return more than one
            // user and thus we'll search with the email address and full name.
            PagedSearchIterable<GHUser> matchingUsers =
                gitHub.searchUsers().q(escapeQueryTerm(authorId)).type("user").in("login").list();
            if (matchingUsers.getTotalCount() == 1) {
                return matchingUsers.iterator().next();
            }

            // Search for a user with a matching email address
            matchingUsers = gitHub.searchUsers().q(escapeQueryTerm(emailAddress)).type("user").in("email").list();
            if (matchingUsers.getTotalCount() == 1) {
                return matchingUsers.iterator().next();
            }

            // Search for a user with a full name matching the passed login (since on git, sometimes users set their
            // name as their git id).
            matchingUsers = gitHub.searchUsers().q(escapeQueryTerm(authorId)).type("user").in("fullname").list();
            if (matchingUsers.getTotalCount() == 1) {
                return matchingUsers.iterator().next();
            }
        } catch (Throwable e) {
            // It failed to locate the user. The most likely reason is that the API rate limit has been reached.
            // Continue so that the users for which it has worked can be saved and so that the user can reimport the
            // rest later on.
            this.logger.warn("Failed to locate user [{}] (email [{}]). Reason: [{}]", authorId, emailAddress,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return null;
    }

    private String escapeQueryTerm(String term)
    {
        return StringUtils.prependIfMissing(StringUtils.appendIfMissing(term, "\""), "\"");
    }

    @Override
    public List<String> importAuthorFromGitHub(GitHub gitHub, String authorId, String emailAddress, boolean overwrite)
        throws GitHubStatsException
    {
        List<BaseObject> matchingAuthorObjects;
        try {
            matchingAuthorObjects = getAuthorObjectsForQuery(String.format(
                "where doc.object(%s).id = '%s'", this.defaultSerializer.serialize(AUTHOR_CLASS), authorId));
        } catch (Exception e) {
            throw new GitHubStatsException(String.format("Failed to find matching author for [%s]", authorId), e);
        }

        return importAuthorFromGitHub(gitHub, authorId, emailAddress, matchingAuthorObjects, overwrite);
    }

    @Override
    public List<String> createAuthorFromGitHub(GitHub gitHub, String authorId, String fallbackEmail, boolean overwrite)
        throws GitHubStatsException
    {
        List<String> importedUsers = new ArrayList<String>();

        // Create the page if it doesn't already exist.
        try {
            GHUser user = gitHub.getUser(authorId);
            String email = user.getEmail();
            if (StringUtils.isEmpty(email)) {
                email = fallbackEmail;
            }
            BaseObject authorObject = importAuthorInternal(user.getLogin(), email, Collections.EMPTY_LIST, overwrite);
            if (authorObject != null) {
                importedUsers.addAll(importAuthorFromGitHub(user, Arrays.asList(authorObject), overwrite));
            }
        } catch (Exception e) {
            throw new GitHubStatsException("Failed to import or create author from GitHub", e);
        }

        return importedUsers;
    }

    @Override
    public List<String> importAllCommittersFromGitHub(GitHub gitHub) throws GitHubStatsException
    {
        Set<String> importedUsers = new LinkedHashSet<String>();

        // Find all Git repositories defined in the current wiki.
        Map<GitHubRepository, String> repositories = getAllRepositoryURLs();
        for (Map.Entry<GitHubRepository, String> entry : repositories.entrySet()) {
            importedUsers.addAll(importCommittersFromGitHub(gitHub, entry.getKey()));
        }

        return new ArrayList<String>(importedUsers);
    }

    @Override
    public List<String> importCommittersFromGitHub(GitHub gitHub, GitHubRepository repository)
        throws GitHubStatsException
    {
        List<String> importedUsers = new ArrayList<String>();

        // Find all collaborators for the specified repository
        XWikiContext xcontext = getXWikiContext();
        try {
            GHRepository ghRepository = gitHub.getRepository(
                String.format("%s/%s", repository.getOrganizationId(), repository.getRepositoryId()));
            for (GHUser user : ghRepository.getCollaborators()) {
                // Ideally we would get the user email from GitHub and update that record. However a lot of users don't
                // specify their email address on GitHub. Thus we use a different strategy:
                // - Look for all users who have an id matching the GitHub user id and update them, hoping that no two
                //   users have the same id...
                // - If no matching author is found, create a new entry
                List<BaseObject> matchingAuthorObjects = getAuthorObjectsForQuery(String.format(
                    "where doc.object(%s).id = '%s'", this.defaultSerializer.serialize(AUTHOR_CLASS), user.getLogin()));
                if (matchingAuthorObjects.isEmpty()) {
                    // Create new author entry
                    BaseObject authorObject = importAuthorInternal(
                        user.getLogin(), user.getEmail(), Collections.singleton(repository), false);
                    // Fill it with author data from GitHub
                    importedUsers.addAll(importAuthorFromGitHub(user, Collections.singletonList(authorObject), false));
                } else {
                    for (BaseObject matchingAuthorObject : matchingAuthorObjects) {
                        XWikiDocument authorDocument = matchingAuthorObject.getOwnerDocument();
                        // Find the xobject representing that repository and if it doesn't exist, create it!
                        BaseObject foundRepositoryObject = null;
                        List<BaseObject> baseObjects = authorDocument.getXObjects(AUTHOR_REPOSITORY_CLASS);
                        if (baseObjects != null) {
                            for (BaseObject baseObject : baseObjects) {
                                if (repository.getOrganizationId().equals(baseObject.getStringValue("organizationId"))
                                    && repository.getRepositoryId().equals(baseObject.getStringValue("repositoryId")))
                                {
                                    foundRepositoryObject = baseObject;
                                    break;
                                }
                            }
                        }
                        if (foundRepositoryObject == null) {
                            // Create xobject
                            foundRepositoryObject = authorDocument.newXObject(AUTHOR_REPOSITORY_CLASS, xcontext);
                            foundRepositoryObject.setStringValue("organizationId", repository.getOrganizationId());
                            foundRepositoryObject.setStringValue("repositoryId", repository.getRepositoryId());
                        }
                        int currentCommitterValue = foundRepositoryObject.getIntValue("committer");
                        if (currentCommitterValue != 1) {
                            foundRepositoryObject.setIntValue("committer", 1);
                            // Save modifications
                            xcontext.getWiki().saveDocument(authorDocument, "Imported committer status from GitHub",
                                true, xcontext);
                            importedUsers.add(authorDocument.getDocumentReference().getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new GitHubStatsException("Failed to import Committer data from GitHub", e);
        }

        return importedUsers;
    }

    @Override
    public List<String> linkAuthors() throws GitHubStatsException
    {
        Set<String> modifiedUsers = new LinkedHashSet<String>();

        // For each author that has its user avatar field set look for similar authors and fill mutually missing fields
        // using the following strategies:
        // - A - find other authors with the same id (but different emails)
        // - B - find other authors having an id matching the author name
        // - C - find other authors having the same name
        // - D - find other authors having the same email
        // - E - find other authors having a full name matching the id
        try {
            List<String> linkedUsers = new ArrayList<String>();
            do {
                linkedUsers.clear();
                String authorClassReference = this.defaultSerializer.serialize(AUTHOR_CLASS);
                // Note that the query is made to work with Oracle which treats empty strings as null.
                List<BaseObject> fullAuthorObjects = getAuthorObjectsForQuery(String.format(
                    ", doc.object(%s) author where (author.avatar <> '' or (author.avatar is not null and '' is null))",
                    authorClassReference));
                for (BaseObject fullAuthorObject : fullAuthorObjects) {
                    String gitId = fullAuthorObject.getStringValue("id");
                    String gitName = fullAuthorObject.getStringValue("name");
                    String gitEmail = fullAuthorObject.getStringValue("email");

                    // Strategy A
                    if (!StringUtils.isEmpty(gitId)) {
                        List<BaseObject> authorObjects = getAuthorObjectsForQuery(String.format(
                            ", doc.object(%s) author where author.id = '%s' and author.email <> '%s'",
                            authorClassReference, gitId, gitEmail));
                        List<String> results = linkUser(fullAuthorObject, authorObjects);
                        linkedUsers.addAll(results);
                        modifiedUsers.addAll(results);
                    }

                    // Strategy B
                    if (!StringUtils.isEmpty(gitName)) {
                        List<BaseObject> authorObjects = getAuthorObjectsForQuery(String.format(
                            "where doc.object(%s).id = '%s'", authorClassReference, gitName));
                        List<String> results = linkUser(fullAuthorObject, authorObjects);
                        linkedUsers.addAll(results);
                        modifiedUsers.addAll(results);
                    }

                    // Strategy C
                    if (!StringUtils.isEmpty(gitName)) {
                        List<BaseObject> authorObjects  = getAuthorObjectsForQuery(String.format(
                            ", doc.object(%s) author where author.name = '%s' and author.id <> '%s' and "
                            + "author.email <> '%s'",
                            authorClassReference, gitName, gitId, gitEmail));
                        List<String> results = linkUser(fullAuthorObject, authorObjects);
                        linkedUsers.addAll(results);
                        modifiedUsers.addAll(results);
                    }

                    // Strategy D
                    if (!StringUtils.isEmpty(gitEmail)) {
                        List<BaseObject> authorObjects = getAuthorObjectsForQuery(String.format(
                            ", doc.object(%s) author where author.email = '%s' and author.id <> '%s'",
                            authorClassReference, gitEmail, gitId));
                        List<String> results = linkUser(fullAuthorObject, authorObjects);
                        linkedUsers.addAll(results);
                        modifiedUsers.addAll(results);
                    }

                    // Strategy E
                    if (!StringUtils.isEmpty(gitEmail)) {
                        List<BaseObject> authorObjects = getAuthorObjectsForQuery(String.format(
                            ", doc.object(%s) author where author.name = '%s' and author.id <> '%s'",
                            authorClassReference, gitId, gitId));
                        List<String> results = linkUser(fullAuthorObject, authorObjects);
                        linkedUsers.addAll(results);
                        modifiedUsers.addAll(results);
                    }
                }
            } while (!linkedUsers.isEmpty());
        } catch (Exception e) {
            throw new GitHubStatsException("Failed to link authors", e);
        }

        return new ArrayList<String>(modifiedUsers);
    }

    @Override
    public Map<Author, Map<String, ?>> getAuthorsForRepositories(Collection<GitHubRepository> repositories)
        throws GitHubStatsException
    {
        Map<Author, Map<String, ?>> authors = new HashMap<Author, Map<String, ?>>();

        try {
            // Find all authors for the passed repositories
            List<String> whereConditions = new ArrayList<String>();
            String authorRepositoryClassReference = this.defaultSerializer.serialize(AUTHOR_REPOSITORY_CLASS);
            for (GitHubRepository repository : repositories) {
                whereConditions.add(String.format(
                    "(authorRepo.organizationId = '%s' AND authorRepo.repositoryId = '%s')",
                    repository.getOrganizationId(), repository.getRepositoryId()));
            }
            List<BaseObject> authorObjects = getAuthorObjectsForQuery(String.format(
                ", doc.object(%s) authorRepo where %s", authorRepositoryClassReference,
                StringUtils.join(whereConditions, " OR ")));

            // For each author return some author data
            for (BaseObject authorObject : authorObjects) {
                String id = authorObject.getStringValue("id");
                Author author = new Author(id, authorObject.getStringValue("email"));
                Map<String, Object> authorData = new HashMap<String, Object>();
                String name = authorObject.getStringValue("name");
                if (StringUtils.isEmpty(name)) {
                    name = id;
                }
                authorData.put("name", name);
                authorData.put("avatar", authorObject.getStringValue("avatar"));
                authorData.put("company", authorObject.getStringValue("company"));
                authorData.put("committer", isCommitter(authorObject, repositories));
                authors.put(author, authorData);
            }

        } catch (Exception e) {
            throw new GitHubStatsException(String.format("Failed to get authors for repositories [%s]", repositories),
                e);
        }

        return authors;
    }

    private boolean isCommitter(BaseObject authorObject, Collection<GitHubRepository> repositories)
    {
        boolean isCommitter = false;
        List<BaseObject> authorRepoObjects =
            authorObject.getOwnerDocument().getXObjects(AUTHOR_REPOSITORY_CLASS);
        if (authorRepoObjects != null) {
            for (BaseObject authorRepoObject : authorRepoObjects) {
                boolean committer = authorRepoObject.getIntValue("committer") == 1;
                if (committer) {
                    String organizationId = authorRepoObject.getStringValue("organizationId");
                    String repositoryId = authorRepoObject.getStringValue("repositoryId");
                    if (repositories.contains(new GitHubRepository(organizationId, repositoryId))) {
                        isCommitter = true;
                        break;
                    }
                }
            }
        }
        return isCommitter;
    }

    @Override
    public List<String> importRepositoriesFromGitHub(GitHub gitHub, String organizationId, boolean overwrite)
        throws GitHubStatsException
    {
        List<String> importedRepositories = new ArrayList<String>();

        try {
            for (GHRepository repository : gitHub.getOrganization(organizationId).getRepositories().values()) {
                XWikiContext xcontext = getXWikiContext();
                String repositoryAsString = String.format("%s:%s", organizationId, repository.getName());
                EntityReference repositoryReference = new EntityReference(repositoryAsString, EntityType.DOCUMENT,
                    new EntityReference(SPACE, EntityType.SPACE));
                XWikiDocument repositoryDocument = xcontext.getWiki().getDocument(repositoryReference, xcontext);
                if (repositoryDocument.isNew() || overwrite) {
                    repositoryDocument.setTitle(String.format("Repository [%s] for Organization [%s]",
                        repository.getName(), organizationId));
                    repositoryDocument.setParentReference(PARENT);
                    repositoryDocument.setHidden(true);
                    BaseObject repositoryObject = repositoryDocument.getXObject(REPOSITORY_CLASS, true, xcontext);
                    repositoryObject.setStringValue("organization", organizationId);
                    repositoryObject.setStringValue("id", repository.getName());
                    repositoryObject.setStringValue("giturl", repository.getGitTransportUrl());
                    repositoryObject.setStringValue("htmlurl", repository.getUrl().toExternalForm());
                    // Save modifications
                    xcontext.getWiki().saveDocument(repositoryDocument, "Imported repository from GitHub", true,
                        xcontext);
                    importedRepositories.add(repositoryAsString);
                }
            }
        } catch (Exception e) {
            throw new GitHubStatsException(
                String.format("Failed to locate Git repositories from GitHub for organization [%s]", organizationId));
        }

        return importedRepositories;
    }

    @Override
    public List<String> deleteRepositories() throws GitHubStatsException
    {
        return deleteItems(REPOSITORY_CLASS, "Failed to delete some GitHub repository pages");
    }

    @Override
    public List<String> deleteAuthors() throws GitHubStatsException
    {
        return deleteItems(AUTHOR_CLASS, "Failed to delete some GitHub author pages");
    }

    @Override
    public Map<GitHubRepository, String> getRepositoryURLs(String... repositoriesAsStrings)
        throws GitHubStatsException
    {
        Map<GitHubRepository, String> result = new HashMap<GitHubRepository, String>();
        Map<GitHubRepository, String> allRepos = getAllRepositoryURLs();
        // For each defined repo, verify it matches the passed string. If it doesn't remove it from the list!
        for (Map.Entry<GitHubRepository, String> repoEntry : allRepos.entrySet()) {
            GitHubRepository repository = repoEntry.getKey();
            for (String repositoryAsString : repositoriesAsStrings) {
                String[] tokens = StringUtils.split(repositoryAsString, '/');
                if (("*".equals(tokens[1]) && repository.getOrganizationId().equals(tokens[0]))
                    || (repository.getOrganizationId().equals(tokens[0])
                        && repository.getRepositoryId().equals(tokens[1])))
                {
                    result.put(repository, repoEntry.getValue());
                }
            }
        }
        return result;
    }

    private Map<GitHubRepository, String> getAllRepositoryURLs() throws GitHubStatsException
    {
        // Find all Git repositories defined in the current wiki.
        List<Object[]> results;
        try {
            Query query = this.queryManager.createQuery(
                String.format("select distinct repo.organization, repo.id, repo.giturl from Document doc, "
                    + "doc.object(%s) as repo", this.defaultSerializer.serialize(REPOSITORY_CLASS)), Query.XWQL);
            results = query.execute();
        } catch (QueryException e) {
            throw new GitHubStatsException("Failed to locate GitHub repositories objects in the wiki", e);
        }

        // For each repository found, find all authors that have ever contributed code.
        Map<GitHubRepository, String> repositories = new HashMap<GitHubRepository, String>();
        for (Object[] repoData : results) {
            String organizationId = (String) repoData[0];
            String repositoryId = (String) repoData[1];
            String gitURL = (String) repoData[2];
            repositories.put(new GitHubRepository(organizationId, repositoryId), gitURL);
        }

        return repositories;
    }

    @Override
    public Map<GitHubRepository, String> getRepositoryURLs(List<GitHubRepository> repositories)
        throws GitHubStatsException
    {
        Map<GitHubRepository, String> result = new HashMap<GitHubRepository, String>();
        Map<GitHubRepository, String> repos = getAllRepositoryURLs();
        for (Map.Entry<GitHubRepository, String> repoEntry : repos.entrySet()) {
            GitHubRepository gitHubRepository = repoEntry.getKey();
            if (repositories.contains(gitHubRepository)) {
                result.put(gitHubRepository, repoEntry.getValue());
            }
        }
        return result;
    }

    @Override
    public List<Repository> getRepositories(Map<GitHubRepository, String> repositories)
    {
        List<Repository> result = new ArrayList<Repository>();
        for (Map.Entry<GitHubRepository, String> repoEntry : repositories.entrySet()) {
            result.add(getRepository(repoEntry.getValue(), repoEntry.getKey()));
        }
        return result;
    }

    @Override
    public Map<String, Map<String, Object>> aggregateCommitsPerAuthor(UserCommitActivity[] userCommitActivity,
        Map<Author, Map<String, Object>> authors)
    {
        Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();

        Map<String, Set<Author>> authorsByName = extractAuthorsByName(authors);
        for (UserCommitActivity userCommit : userCommitActivity) {
            Author author = new Author(userCommit.getName(), userCommit.getEmail());
            Map<String, Object> authorData = authors.get(author);
            if (authorData == null) {
                // If we don't know this author, we skip it
                continue;
            }
            String authorName = (String) authorData.get("name");
            if (StringUtils.isEmpty(authorName)) {
                authorName = userCommit.getName();
            }
            // Create a result entry if none exist for the name already
            Map<String, Object> authorResult = result.get(authorName);
            if (authorResult == null) {
                authorResult = new HashMap<String, Object>();
                result.put(authorName, authorResult);
                // Add all the authors with the same name as duplicates
                Set<Author> contributingAuthors = authorsByName.get(authorName);
                authorResult.put("authors", contributingAuthors);
                // Save the email too
                authorResult.put("email", author.getEmail());
            } else {
                // Add this author as an aggregated author
                Set<Author> contributingAuthors = (Set<Author>) authorResult.get("authors");
                contributingAuthors.add(author);
            }
            // If the avatar or company fields are not set already, set them!
            String avatar = (String) authorResult.get("avatar");
            if (StringUtils.isEmpty(avatar)) {
                authorResult.put("avatar", authorData.get("avatar"));
            }
            String company = (String) authorResult.get("company");
            if (StringUtils.isEmpty(company)) {
                authorResult.put("company", authorData.get("company"));
            }
            // Overwrite committer info if true
            if ((Boolean) authorData.get("committer")) {
                authorResult.put("committer", true);
            } else {
                authorResult.put("committer", false);
            }
            // Increase counter and store aggregated result
            Integer counter = (Integer) authorResult.get("count");
            if (counter == null) {
                counter = 0;
            }
            counter += userCommit.getCount();
            authorResult.put("count", counter);
        }

        // Also add all authors with same email addresses as aggregated authors.
        Map<String, Set<Author>> authorsByEmail = extractAuthorsByEmail(authors);
        for (Map.Entry<String, Map<String, Object>> entry : result.entrySet()) {
            Map<String, Object> resultData = entry.getValue();
            String email = (String) resultData.get("email");
            Set<Author> authorByEmail = authorsByEmail.get(email);
            Set<Author> contributingAuthors = (Set<Author>) resultData.get("authors");
            contributingAuthors.addAll(authorByEmail);
        }

        // Sort Map
        List<Map.Entry<String, Map<String, Object>>> list =
            new ArrayList<Map.Entry<String, Map<String, Object>>>(result.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<String, Map<String, Object>>>() {
            public int compare(Map.Entry<String, Map<String, Object>> e1, Map.Entry<String, Map<String, Object>> e2) {
                // Highest count first!
                Integer count1 = (Integer) e1.getValue().get("count");
                Integer count2 = (Integer) e2.getValue().get("count");
                return count2.compareTo(count1);
            }
        });
        Map<String, Map<String, Object>> sortedResult = new LinkedHashMap<String, Map<String, Object>>();
        for (Map.Entry<String, Map<String, Object>> entry : list) {
            sortedResult.put(entry.getKey(), entry.getValue());
        }

        return sortedResult;
    }

    private Map<String, Set<Author>> extractAuthorsByName(Map<Author, Map<String, Object>> authors)
    {
        Map<String, Set<Author>> authorsByName = new HashMap<String, Set<Author>>();
        for (Map.Entry<Author, Map<String, Object>> entry : authors.entrySet()) {
            Author author = entry.getKey();
            Map<String, Object> authorData = entry.getValue();
            String name = (String) authorData.get("name");
            Set<Author> authorByName = authorsByName.get(name);
            if (authorByName == null) {
                authorByName = new HashSet<Author>();
                authorsByName.put(name, authorByName);
            }
            authorByName.add(author);
        }

        return authorsByName;
    }

    private Map<String, Set<Author>> extractAuthorsByEmail(Map<Author, Map<String, Object>> authors)
    {
        Map<String, Set<Author>> authorsByEmail = new HashMap<String, Set<Author>>();
        for (Map.Entry<Author, Map<String, Object>> entry : authors.entrySet()) {
            Author author = entry.getKey();
            Map<String, Object> authorData = entry.getValue();
            Set<Author> authorByEmail = authorsByEmail.get(author.getEmail());
            if (authorByEmail == null) {
                authorByEmail = new HashSet<Author>();
                authorsByEmail.put(author.getEmail(), authorByEmail);
            }
            authorByEmail.add(author);
        }

        return authorsByEmail;
    }

    private List<String> deleteItems(EntityReference xclassReference, String exceptionMessage)
        throws GitHubStatsException
    {
        List<String> deletedItems = new ArrayList<String>();
        try {
            Query query = this.queryManager.createQuery(
                String.format("select distinct doc.space, doc.name from Document doc, doc.object(%s) as author",
                    this.defaultSerializer.serialize(xclassReference)), Query.XWQL);
            List<Object[]> results = query.execute();
            XWikiContext xcontext = getXWikiContext();
            for (Object[] documentData : results) {
                EntityReference relativeReference = new EntityReference((String) documentData[1], EntityType.DOCUMENT,
                    new EntityReference((String) documentData[0], EntityType.SPACE));
                XWikiDocument itemDocument = xcontext.getWiki().getDocument(relativeReference, xcontext);
                deletedItems.add(itemDocument.getDocumentReference().toString());
                xcontext.getWiki().deleteDocument(itemDocument, true, xcontext);
            }
        } catch (Exception e) {
            throw new GitHubStatsException(exceptionMessage, e);
        }

        return deletedItems;
    }

    private List<String> linkUser(BaseObject fullAuthorObject, List<BaseObject> authorObjects)
        throws XWikiException
    {
        List<String> linkedUsers = new ArrayList<String>();

        XWikiContext xcontext = getXWikiContext();
        for (BaseObject authorObject : authorObjects) {
            boolean modified = false;
            modified = modified || setField("avatar", fullAuthorObject, authorObject);
            modified = modified || setField("name", fullAuthorObject, authorObject);
            modified = modified || setField("profileurl", fullAuthorObject, authorObject);
            modified = modified || setField("company", fullAuthorObject, authorObject);
            // Also set the committer flag on repos
            Map<GitHubRepository, BaseObject> fullRepos = getAuthorRepositories(fullAuthorObject);
            Map<GitHubRepository, BaseObject> repos = getAuthorRepositories(authorObject);
            for (Map.Entry<GitHubRepository, BaseObject> repoEntry : repos.entrySet()) {
                // If this repo is in fullRepos and the fullAuthor is a committer then set it!
                BaseObject fullRepoObject = fullRepos.get(repoEntry.getKey());
                if (fullRepoObject != null) {
                    boolean isCommitter = fullRepoObject.getIntValue("committer") == 1;
                    if (isCommitter && repoEntry.getValue().getIntValue("committer") != 1) {
                        repoEntry.getValue().setIntValue("committer", 1);
                        modified = true;
                    }
                }
            }
            if (modified) {
                String fullId = fullAuthorObject.getStringValue("id");
                // Save modifications
                xcontext.getWiki().saveDocument(authorObject.getOwnerDocument(),
                    String.format("Linked author with [%s]", fullId), true, xcontext);
                linkedUsers.add(authorObject.getDocumentReference().getName());
            }
        }

        return linkedUsers;
    }

    private Map<GitHubRepository, BaseObject> getAuthorRepositories(BaseObject authorObject)
    {
        Map<GitHubRepository, BaseObject> repos = new HashMap<GitHubRepository, BaseObject>();
        List<BaseObject> repoObjects = authorObject.getOwnerDocument().getXObjects(AUTHOR_REPOSITORY_CLASS);
        if (repoObjects != null) {
            for (BaseObject repoObject : repoObjects) {
                String organizationId = repoObject.getStringValue("organizationId");
                String repositoryId = repoObject.getStringValue("repositoryId");
                repos.put(new GitHubRepository(organizationId, repositoryId), repoObject);
            }
        }
        return repos;
    }

    private boolean setField(String fieldName, BaseObject source, BaseObject target)
    {
        boolean modified = false;
        String fullValue = source.getStringValue(fieldName);
        if (!StringUtils.isEmpty(fullValue)) {
            String value = target.getStringValue(fieldName);
            if (StringUtils.isEmpty(value)) {
                target.setStringValue(fieldName, fullValue);
                modified = true;
            }
        }
        return modified;
    }

    private List<BaseObject> getAuthorObjectsForQuery(String xwqlWhere) throws QueryException, XWikiException
    {
        List<BaseObject> objects = new ArrayList<BaseObject>();
        Query query = this.queryManager.createQuery(String.format(
            "select distinct doc.space, doc.name from Document doc %s", xwqlWhere), Query.XWQL);
        List<Object[]> results = query.execute();
        XWikiContext xcontext = getXWikiContext();
        for (Object[] documentData : results) {
            EntityReference relativeReference = new EntityReference((String) documentData[1], EntityType.DOCUMENT,
                new EntityReference((String) documentData[0], EntityType.SPACE));
            XWikiDocument authorDocument = xcontext.getWiki().getDocument(relativeReference, xcontext);
            BaseObject authorObject = authorDocument.getXObject(AUTHOR_CLASS, false, xcontext);
            objects.add(authorObject);
        }
        return objects;
    }

    private XWikiDocument getAuthorDocument(String name, String email) throws XWikiException
    {
        String authorAsString = String.format("%s <%s>", name, email);
        XWikiContext xcontext = getXWikiContext();
        EntityReference authorReference = new EntityReference(authorAsString, EntityType.DOCUMENT,
            new EntityReference(SPACE, EntityType.SPACE));
        // Get author document or create a new one if it doesn't exist
        return xcontext.getWiki().getDocument(authorReference, xcontext);
    }

    private BaseObject createAuthorClass(XWikiDocument authorDocument)
    {
        return authorDocument.getXObject(AUTHOR_CLASS, true, getXWikiContext());
    }

    private void addAuthorRepositoryObjects(XWikiDocument authorDocument, Collection<GitHubRepository> repositories,
        XWikiContext xcontext) throws XWikiException
    {
        for (GitHubRepository repository : repositories) {
            BaseObject authorObject = authorDocument.newXObject(AUTHOR_REPOSITORY_CLASS, xcontext);
            authorObject.setStringValue("organizationId", repository.getOrganizationId());
            authorObject.setStringValue("repositoryId", repository.getRepositoryId());
        }
    }

    private Repository getRepository(String uri, GitHubRepository repository)
    {
        return this.gitManager.getRepository(uri,
            repository.getOrganizationId() + File.separator + repository.getRepositoryId());
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
