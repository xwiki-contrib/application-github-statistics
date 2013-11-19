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
package org.xwiki.githubstats.test.ui;

import org.junit.*;
import org.xwiki.contrib.GitHubStatsHomePage;
import org.xwiki.contrib.ImportAuthorsPage;
import org.xwiki.contrib.ImportRepositoriesPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.LiveTableElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * UI tests for the GitHubStats application.
 *
 * @version $Id$
 * @since 2.0
 */
public class GitHubStatsTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil(), getDriver());

    @Test
    public void testImports()
    {
        GitHubStatsHomePage home = GitHubStatsHomePage.gotoPage();

        // Import Repositories
        ImportRepositoriesPage importRepositoriesPage = home.clickImportRepositories();
        importRepositoriesPage = importRepositoriesPage.importOrganization("organization1");
        LiveTableElement livetable = importRepositoriesPage.getRepositoriesLiveTable();
        assertEquals(6, livetable.getRowCount());
        importRepositoriesPage = importRepositoriesPage.deleteAllRepositories();
        livetable = importRepositoriesPage.getRepositoriesLiveTable();
        assertEquals(0, livetable.getRowCount());
        importRepositoriesPage = importRepositoriesPage.importOrganization("organization1");

        // Navigate back to the home page using the breadcrumb
        importRepositoriesPage.clickBreadcrumbLink("GitHub Repositories & Git Authors");
        home = new GitHubStatsHomePage();

        // Import Authors
        ImportAuthorsPage importAuthorsPage = home.clickImportAuthors();
        // Import from Git
        importAuthorsPage = importAuthorsPage.importAllAuthorsFromGit();
        livetable = importAuthorsPage.getAuthorsLiveTable();
        assertEquals(3, livetable.getRowCount());
        assertTrue(livetable.hasRow("Git Id", "author1"));
        assertTrue(livetable.hasRow("Git Id", "author2"));
        assertTrue(livetable.hasRow("Git Email", "author1@doe.com"));
        assertTrue(livetable.hasRow("Git Email", "author2@doe.com"));
        assertTrue(livetable.hasRow("Name", "Not defined"));
        assertTrue(livetable.hasRow("Company", "Not defined"));
        importAuthorsPage = importAuthorsPage.deleteAllAuthors();
        livetable = importAuthorsPage.getAuthorsLiveTable();
        assertEquals(0, livetable.getRowCount());
        importAuthorsPage = importAuthorsPage.importAllAuthorsFromGit();
        // Import from GitHub
        importAuthorsPage = importAuthorsPage.importAllAuthorsFromGitHub();
        livetable = importAuthorsPage.getAuthorsLiveTable();
        assertEquals(3, livetable.getRowCount());
        assertTrue(livetable.hasRow("Name", "author1"));
        assertTrue(livetable.hasRow("Company", "Company"));
        assertTrue(livetable.hasRow("Name", "Not defined"));
        assertTrue(livetable.hasRow("Company", "Not defined"));
        // Import Committers from GitHub
        importAuthorsPage = importAuthorsPage.importAllCommittersFromGitHub();
        // Link authors
        importAuthorsPage = importAuthorsPage.linkAuthors();
        // Now navigate to each author page to verify its data
        // TODO...
    }
}
