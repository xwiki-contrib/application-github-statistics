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
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.ViewPage;

public class ImportAuthorsPage extends ViewPage
{
    @FindBy(xpath = "//*[@id='importAuthors']//input[@type='submit' and @value='Import Authors from Git']")
    private WebElement importAllAuthorsFromGitSubmit;

    @FindBy(xpath = "//*[@id='importAuthors']//input[@type='submit' and @value='Update authors with GitHub data']")
    private WebElement importAllAuthorsFromGitHubSubmit;

    @FindBy(xpath = "//*[@id='importAuthors']//input[@type='submit' and @value='Import Committers from GitHub']")
    private WebElement importAllCommittersFromGitHubSubmit;

    @FindBy(xpath = "//*[@id='importAuthors']//input[@type='submit' and @value='Link Authors']")
    private WebElement linkAuthorsSubmit;

    @FindBy(xpath = "//*[@id='importAuthors']//input[@type='submit' and @value='Delete all Authors']")
    private WebElement deleteAllAuthorsSubmit;

    @FindBy(id = "confirmDelete")
    private WebElement confirmDeleteTextInput;

    @FindBy(xpath = "//*[@id='deleteAuthors']//input[@type='submit' and @value='Really delete all Authors']")
    private WebElement confirmDeleteAllAuthorsSubmit;

    public ImportAuthorsPage importAuthorsFromGit()
    {
        this.importAllAuthorsFromGitSubmit.click();
        return new ImportAuthorsPage();
    }

    public ImportAuthorsPage updateAuthorsWithGitHubData()
    {
        this.importAllAuthorsFromGitHubSubmit.click();
        return new ImportAuthorsPage();
    }

    public ImportAuthorsPage importCommittersFromGitHub()
    {
        this.importAllCommittersFromGitHubSubmit.click();
        return new ImportAuthorsPage();
    }

    public ImportAuthorsPage linkAuthors()
    {
        this.linkAuthorsSubmit.click();
        return new ImportAuthorsPage();
    }

    public ImportAuthorsPage deleteAuthors()
    {
        this.deleteAllAuthorsSubmit.click();
        ImportAuthorsPage currentPage = new ImportAuthorsPage();
        currentPage.confirmDeleteTextInput.clear();
        currentPage.confirmDeleteTextInput.sendKeys("ok");
        currentPage.confirmDeleteAllAuthorsSubmit.click();
        return new ImportAuthorsPage();
    }

    public LiveTableElement getAuthorsLiveTable()
    {
        LiveTableElement lt = new LiveTableElement("authors");
        lt.waitUntilReady();
        return lt;
    }
}
