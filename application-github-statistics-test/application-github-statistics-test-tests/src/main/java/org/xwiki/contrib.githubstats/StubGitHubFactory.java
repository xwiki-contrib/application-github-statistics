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
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPersonSet;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GitHub;

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

        GitHub gitHub = mock(GitHub.class);
        when(gitHub.getUser("author2")).thenReturn(user2);
        when(gitHub.getRepository("organization1/repository1")).thenReturn(repository1);
        when(gitHub.getRepository("organization1/repository2")).thenReturn(repository2);
        when(gitHub.getRepository("organization1/repository3")).thenReturn(repository3);
        when(gitHub.getRepository("organization1/repository4")).thenReturn(repository4);
        when(gitHub.getRepository("organization1/repository5")).thenReturn(repository5);
        when(gitHub.getRepository("organization1/repository6")).thenReturn(repository6);
        when(gitHub.getOrganization("organization1")).thenReturn(organization1);

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
        when(repository.getUrl()).thenReturn("repository" + id + " HTML URL");
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
