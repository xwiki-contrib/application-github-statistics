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
package org.xwiki.contrib;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents actions that can be done on the GitHubStats.WebHome page.
 *
 * @version $Id$
 * @since 2.0
 */
public class GitHubStatsHomePage extends ViewPage
{
    @FindBy(xpath = "//a[text() = 'Import Repositories']")
    private WebElement importRepositoriesLink;

    @FindBy(xpath = "//a[text() = 'Import Authors']")
    private WebElement importAuthorsLink;

    /**
     * Opens the GitHubStats home page.
     */
    public static GitHubStatsHomePage gotoPage()
    {
        getUtil().gotoPage(getSpace(), getPage());
        return new GitHubStatsHomePage();
    }

    public static String getSpace()
    {
        return "GitHubStats";
    }

    public static String getPage()
    {
        return "WebHome";
    }

    public ImportRepositoriesPage clickImportRepositories()
    {
        this.importRepositoriesLink.click();
        return new ImportRepositoriesPage();
    }

    public ImportAuthorsPage clickImportAuthors()
    {
        this.importAuthorsLink.click();
        return new ImportAuthorsPage();
    }
}
