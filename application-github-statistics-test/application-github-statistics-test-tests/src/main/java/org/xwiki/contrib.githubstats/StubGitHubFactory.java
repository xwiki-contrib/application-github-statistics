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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPersonSet;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHUserSearchBuilder;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterator;
import org.kohsuke.github.PagedSearchIterable;

import static org.mockito.Mockito.*;

public class StubGitHubFactory implements GitHubFactory
{
    @Override
    public GitHub createGitHub() throws IOException
    {
        // Scenario 1:
        // - 1 author in Github
        // - 6 repos
        // - 1 org
        GHUser user2 = createUser("author2", "author1");

        GHRepository repository1 = createRepository(1, user2);
        GHRepository repository2 = createRepository(2, user2);
        GHRepository repository3 = createRepository(3, user2);
        GHRepository repository4 = createRepository(4, user2);
        GHRepository repository5 = createRepository(5, user2);
        GHRepository repository6 = createRepository(6, user2);

        GHOrganization organization1 = mock(GHOrganization.class);
        Map<String, GHRepository> repositories = new HashMap<String, GHRepository>();
        repositories.put("repository1", repository1);
        repositories.put("repository2", repository2);
        repositories.put("repository3", repository3);
        repositories.put("repository4", repository4);
        repositories.put("repository5", repository5);
        repositories.put("repository6", repository6);
        when(organization1.getRepositories()).thenReturn(repositories);

        PagedIterator<GHUser> author2PagedIterator = mock(PagedIterator.class);
        when(author2PagedIterator.next()).thenReturn(user2);

        PagedSearchIterable<GHUser> author2PagedSearchIterable = mock(PagedSearchIterable.class);
        when(author2PagedSearchIterable.getTotalCount()).thenReturn(1);
        when(author2PagedSearchIterable.iterator()).thenReturn(author2PagedIterator);

        GHUserSearchBuilder author2SearchUserBuilder = mock(GHUserSearchBuilder.class);
        when(author2SearchUserBuilder.type("user")).thenReturn(author2SearchUserBuilder);
        when(author2SearchUserBuilder.in("login")).thenReturn(author2SearchUserBuilder);
        when(author2SearchUserBuilder.list()).thenReturn(author2PagedSearchIterable);

        PagedSearchIterable<GHUser> emptyPagedSearchIterable = mock(PagedSearchIterable.class);
        when(emptyPagedSearchIterable.getTotalCount()).thenReturn(0);

        GHUserSearchBuilder emptySearchUserBuilder = mock(GHUserSearchBuilder.class);
        when(emptySearchUserBuilder.type("user")).thenReturn(emptySearchUserBuilder);
        when(emptySearchUserBuilder.in("login")).thenReturn(emptySearchUserBuilder);
        when(emptySearchUserBuilder.in("email")).thenReturn(emptySearchUserBuilder);
        when(emptySearchUserBuilder.in("fullname")).thenReturn(emptySearchUserBuilder);
        when(emptySearchUserBuilder.list()).thenReturn(emptyPagedSearchIterable);

        GHUserSearchBuilder searchUserBuilder = mock(GHUserSearchBuilder.class);
        when(searchUserBuilder.q("\"author1\"")).thenReturn(emptySearchUserBuilder);
        when(searchUserBuilder.q("\"author1@doe.com\"")).thenReturn(emptySearchUserBuilder);
        when(searchUserBuilder.q("\"author2\"")).thenReturn(author2SearchUserBuilder);
        when(searchUserBuilder.q("\"author3\"")).thenReturn(emptySearchUserBuilder);
        when(searchUserBuilder.q("\"author3@doe.com\"")).thenReturn(emptySearchUserBuilder);

        GitHub gitHub = mock(GitHub.class);
        when(gitHub.getRepository("organization1/repository1")).thenReturn(repository1);
        when(gitHub.getRepository("organization1/repository2")).thenReturn(repository2);
        when(gitHub.getRepository("organization1/repository3")).thenReturn(repository3);
        when(gitHub.getRepository("organization1/repository4")).thenReturn(repository4);
        when(gitHub.getRepository("organization1/repository5")).thenReturn(repository5);
        when(gitHub.getRepository("organization1/repository6")).thenReturn(repository6);
        when(gitHub.getOrganization("organization1")).thenReturn(organization1);
        when(gitHub.searchUsers()).thenReturn(searchUserBuilder);

        return gitHub;
    }

    @Override
    public GitHub createGitHub(String login, String authToken) throws IOException
    {
        return createGitHub();
    }

    private GHRepository createRepository(int id, GHUser user) throws IOException
    {
        GHRepository repository = mock(GHRepository.class);
        GHPersonSet personSet = new GHPersonSet();
        personSet.add(user);
        when(repository.getCollaborators()).thenReturn(personSet);
        when(repository.getName()).thenReturn("repository" + id);
        when(repository.getGitTransportUrl()).thenReturn("repository" + id + " git URL");
        when(repository.getUrl()).thenReturn(new URL("http://github.com/" + id));
        // Return a dummy non-zero value to signify it's not an empty repository!
        when(repository.getSize()).thenReturn(1);
        return repository;
    }

    private GHUser createUser(String userId, String name) throws IOException
    {
        GHUser user = mock(GHUser.class);
        when(user.getLogin()).thenReturn(userId);
        when(user.getName()).thenReturn(name);
        when(user.getCompany()).thenReturn("Company");
        when(user.getEmail()).thenReturn("");
        when(user.getAvatarUrl()).thenReturn("avatar");
        return user;
    }
}
