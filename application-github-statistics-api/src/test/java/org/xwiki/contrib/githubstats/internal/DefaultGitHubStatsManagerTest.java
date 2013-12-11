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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.gitective.core.stat.UserCommitActivity;
import org.junit.*;
import org.xwiki.contrib.githubstats.Author;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.xwiki.contrib.githubstats.internal.DefaultGitHubStatsManager}.
 *
 * @version $Id$
 * @since 2.0
 */
public class DefaultGitHubStatsManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultGitHubStatsManager> mocker =
        new MockitoComponentMockingRule<DefaultGitHubStatsManager>(DefaultGitHubStatsManager.class);

    @Test
    public void aggregateCommitsPerAuthorWhenSameNameButDifferentEmails() throws Exception
    {
        UserCommitActivity[] activities = new UserCommitActivity[1];
        activities[0] = new UserCommitActivity("id1", "email1");

        Map<Author, Map<String, Object>> authors = new HashMap<Author, Map<String, Object>>();
        // 2 users who represent the same person: same name but different emails.
        addAuthor("id1", "name1", "email1", "avatar1", "company1", true, authors);
        addAuthor("id1", "name1", "email2", "avatar1", "company1", true, authors);

        Map<String, Map<String, Object>> results =
            this.mocker.getComponentUnderTest().aggregateCommitsPerAuthor(activities, authors);

        assertEquals(1, results.size());
        Map<String, Object> resultAuthorData = results.get("name1");
        assertNotNull(resultAuthorData);
        assertEquals(0, resultAuthorData.get("count"));
        assertEquals("company1", resultAuthorData.get("company"));
        assertEquals("avatar1", resultAuthorData.get("avatar"));
        assertEquals(true, resultAuthorData.get("committer"));
        Set<Author> relatedAuthors = (Set<Author>) resultAuthorData.get("authors");
        assertEquals(2, relatedAuthors.size());
        assertTrue(relatedAuthors.contains(new Author("id1", "email1")));
        assertTrue(relatedAuthors.contains(new Author("id1", "email2")));
    }

    @Test
    public void aggregateCommitsPerAuthorWhenSameEmailButDifferentNames() throws Exception
    {
        UserCommitActivity[] activities = new UserCommitActivity[1];
        activities[0] = new UserCommitActivity("id1", "email1");

        Map<Author, Map<String, Object>> authors = new HashMap<Author, Map<String, Object>>();
        // 2 users who represent the same person from a UserCommitActivity (its key is the email address): different
        // name but same email
        addAuthor("id1", "name1", "email1", "avatar1", "company1", true, authors);
        addAuthor("id2", "name2", "email1", "avatar2", "company2", true, authors);

        Map<String, Map<String, Object>> results =
            this.mocker.getComponentUnderTest().aggregateCommitsPerAuthor(activities, authors);

        assertEquals(1, results.size());
        Map<String, Object> resultAuthorData = results.get("name1");
        assertNotNull(resultAuthorData);
        assertEquals(0, resultAuthorData.get("count"));
        assertEquals("company1", resultAuthorData.get("company"));
        assertEquals("avatar1", resultAuthorData.get("avatar"));
        assertEquals(true, resultAuthorData.get("committer"));
        Set<Author> relatedAuthors = (Set<Author>) resultAuthorData.get("authors");
        assertEquals(2, relatedAuthors.size());
        assertTrue(relatedAuthors.contains(new Author("id1", "email1")));
        assertTrue(relatedAuthors.contains(new Author("id2", "email1")));
    }

    private void addAuthor(String id, String name, String email, String avatar, String company, boolean isCommitter,
        Map<Author, Map<String, Object>> authors)
    {
        Map<String, Object> authorData = new HashMap<String, Object>();
        authorData.put("name", name);
        authorData.put("avatar", avatar);
        authorData.put("company", company);
        authorData.put("committer", isCommitter);
        authors.put(new Author(id, email), authorData);
    }
}
