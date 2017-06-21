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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.contrib.AuthorSheetPage;
import org.xwiki.contrib.GitHubStatsHomePage;
import org.xwiki.contrib.ImportAuthorsPage;
import org.xwiki.contrib.ImportRepositoriesPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

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
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

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
        importAuthorsPage = importAuthorsPage.importAuthorsFromGit();
        livetable = importAuthorsPage.getAuthorsLiveTable();
        assertEquals(4, livetable.getRowCount());
        importAuthorsPage = importAuthorsPage.deleteAuthors();
        livetable = importAuthorsPage.getAuthorsLiveTable();
        assertEquals(0, livetable.getRowCount());
        importAuthorsPage = importAuthorsPage.importAuthorsFromGit();
        livetable.filterColumn("xwiki-livetable-authors-filter-2", "author1");
        assertEquals(1, livetable.getRowCount());
        assertTrue(hasRow(livetable, "Git Id", "author1"));
        assertTrue(hasRow(livetable, "Git Email", "author1@doe.com"));
        assertTrue(hasRow(livetable, "Name", "Not defined"));
        assertTrue(hasRow(livetable, "Company", "Not defined"));
        livetable.filterColumn("xwiki-livetable-authors-filter-2", "author2");
        assertEquals(2, livetable.getRowCount());
        assertTrue(hasExactRows(livetable, "Git Id", Arrays.asList("author2", "author2")));
        assertTrue(hasExactRows(livetable, "Git Email", Arrays.asList("author2@doe.com", "author1@doe.com")));
        livetable.filterColumn("xwiki-livetable-authors-filter-2", "author3");
        assertTrue(hasRow(livetable, "Git Id", "author3"));
        assertTrue(hasRow(livetable, "Git Email", "author3@doe.com"));
        // Import from GitHub
        importAuthorsPage = importAuthorsPage.updateAuthorsWithGitHubData();
        livetable = importAuthorsPage.getAuthorsLiveTable();
        // Reset the filter since it's still active even after the button click
        livetable.filterColumn("xwiki-livetable-authors-filter-2", "");
        assertEquals(4, livetable.getRowCount());
        assertTrue(hasRow(livetable, "Name", "author1"));
        assertTrue(hasRow(livetable, "Company", "Company"));
        assertTrue(hasRow(livetable, "Name", "Not defined"));
        assertTrue(hasRow(livetable, "Company", "Not defined"));
        // Import Committers from GitHub
        importAuthorsPage = importAuthorsPage.importCommittersFromGitHub();
        // Link authors
        importAuthorsPage.linkAuthors();

        // Now navigate to each author page to verify its data
        livetable.clickCell(1, 2);
        AuthorSheetPage authorSheetPage = new AuthorSheetPage();
        assertEquals("Id\nauthor3\n"
            + "Email\nauthor3@doe.com\n"
            + "Name\n\n"
            + "Company\n\n"
            + "Repositories\n"
            + "organization1 / repository1", authorSheetPage.getContent());
        importRepositoriesPage.clickBreadcrumbLink("GitHub Repositories & Git Authors");
        home = new GitHubStatsHomePage();
        livetable = home.getAuthorsLiveTable();
        livetable.clickCell(2, 2);
        authorSheetPage = new AuthorSheetPage();
        assertEquals("Id\nauthor2\n"
            + "Email\nauthor1@doe.com\n"
            + "Name\nauthor1\n"
            + "Company\nCompany\n"
            + "Repositories\n"
            + "organization1 / repository2 (Committer)\n"
            + "organization1 / repository1 (Committer)\n"
            + "organization1 / repository6 (Committer)\n"
            + "organization1 / repository5 (Committer)\n"
            + "organization1 / repository4 (Committer)\n"
            + "organization1 / repository3 (Committer)", authorSheetPage.getContent());
        importRepositoriesPage.clickBreadcrumbLink("GitHub Repositories & Git Authors");
        home = new GitHubStatsHomePage();
        livetable = home.getAuthorsLiveTable();
        livetable.clickCell(3, 2);
        authorSheetPage = new AuthorSheetPage();
        assertEquals("Id\nauthor2\n"
            + "Email\nauthor2@doe.com\n"
            + "Name\nauthor1\n"
            + "Company\nCompany\n"
            + "Repositories\n"
            + "organization1 / repository6 (Committer)\n"
            + "organization1 / repository2 (Committer)\n"
            + "organization1 / repository1 (Committer)\n"
            + "organization1 / repository5 (Committer)\n"
            + "organization1 / repository4 (Committer)\n"
            + "organization1 / repository3 (Committer)", authorSheetPage.getContent());
        importRepositoriesPage.clickBreadcrumbLink("GitHub Repositories & Git Authors");
        home = new GitHubStatsHomePage();
        livetable = home.getAuthorsLiveTable();
        livetable.clickCell(4, 2);
        authorSheetPage = new AuthorSheetPage();
        assertEquals("Id\nauthor1\n"
            + "Email\nauthor1@doe.com\n"
            + "Name\nauthor1\n"
            + "Company\nCompany\n"
            + "Repositories\n"
            + "organization1 / repository2 (Committer)\n"
            + "organization1 / repository1 (Committer)\n"
            + "organization1 / repository6 (Committer)\n"
            + "organization1 / repository5 (Committer)\n"
            + "organization1 / repository4 (Committer)\n"
            + "organization1 / repository3 (Committer)", authorSheetPage.getContent());

        // Now create a new page using the committers and firstCommits macro to verify they work fine
        ViewPage vp = getUtil().createPage(getTestClassName(), getTestMethodName(),
            "{{committers repositories='organization1/repository1,organization1/repository2,"
            + "organization1/repository3,organization1/repository4,organization1/repository5,"
            + "organization1/repository6'/}}\n\n"
            + "{{committers since='1' repositories='organization1/repository1'/}}\n\n"
            + "{{firstCommits repositories='organization1/repository1,organization1/repository2,"
            + "organization1/repository3,organization1/repository4,organization1/repository5,"
            + "organization1/repository6'/}}", "Macro Tests", "xwiki/2.1");
        String expectedRegex = "author1\nCompany\n12\n"
            + "author3\n1\n"
            + "author1\nCompany\n2\n"
            + "author1\nCompany\n.*\n0 years, 0 months, 0 days\n"
            + "author1\nCompany\n.*\n0 years, 0 months, 0 days\n"
            + "author3\n.*\n0 years, 0 months, 3 days";
        assertTrue(Pattern.compile(expectedRegex).matcher(vp.getContent()).matches());
    }

    /**
     * @todo Remove this when this app depends on XWiki 7.2M2 since it's been added in there, see
     * http://jira.xwiki.org/browse/XWIKI-12350
     */
    private boolean hasRow(LiveTableElement liveTableElement, String columnTitle, String columnValue)
    {
        List<WebElement> elements = getRows(liveTableElement, columnTitle);

        boolean result = elements.size() > 0;
        boolean match = false;
        if (result) {
            for (WebElement element : elements) {
                match = element.getText().equals(columnValue);
                if (match) {
                    break;
                }
            }
        }

        return result && match;
    }

    /**
     * @todo Remove this when this app depends on XWiki 7.2M2 since it's been added in there, see
     * http://jira.xwiki.org/browse/XWIKI-12350
     */
    private boolean hasExactRows(LiveTableElement liveTableElement, String columnTitle, List<String> columnValues)
    {
        List<WebElement> elements = getRows(liveTableElement, columnTitle);

        boolean result = elements.size() == columnValues.size();
        if (result) {
            for (int i = 0; i < elements.size(); i++) {
                result = result && elements.get(i).getText().equals(columnValues.get(i));
                if (!result) {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * @todo Remove this when this app depends on XWiki 7.2M2 since it's been added in there, see
     * http://jira.xwiki.org/browse/XWIKI-12350
     */
    private List<WebElement> getRows(LiveTableElement liveTableElement, String columnTitle)
    {
        String cellXPath = String.format(".//tr/td[position() = %s]", liveTableElement.getColumnIndex(columnTitle) + 1);
        WebElement liveTableBody = getDriver().findElement(By.id("authors-display"));
        return liveTableBody.findElements(By.xpath(cellXPath));
    }
}
