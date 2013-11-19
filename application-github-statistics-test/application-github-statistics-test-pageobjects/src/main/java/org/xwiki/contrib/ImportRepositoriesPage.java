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

public class ImportRepositoriesPage extends ViewPage
{
    @FindBy(id = "organization")
    private WebElement organizationTextInput;

    @FindBy(xpath = "//*[@id='importRepositories']//input[@type='submit' and @value='Import']")
    private WebElement importSubmit;

    @FindBy(xpath = "//*[@id='importRepositories']//input[@type='submit' and @value='Delete all Repositories']")
    private WebElement deleteAllRepositoriesSubmit;

    @FindBy(id = "confirmDelete")
    private WebElement confirmDeleteTextInput;

    @FindBy(xpath = "//*[@id='deleteRepositories']//input[@type='submit' and @value='Really delete all Repositories']")
    private WebElement confirmDeleteAllRepositoriesSubmit;

    public ImportRepositoriesPage importOrganization(String organizationId)
    {
        this.organizationTextInput.clear();
        this.organizationTextInput.sendKeys(organizationId);
        this.importSubmit.click();
        return new ImportRepositoriesPage();
    }

    public ImportRepositoriesPage deleteAllRepositories()
    {
        this.deleteAllRepositoriesSubmit.click();
        ImportRepositoriesPage currentPage = new ImportRepositoriesPage();
        currentPage.confirmDeleteTextInput.clear();
        currentPage.confirmDeleteTextInput.sendKeys("ok");
        currentPage.confirmDeleteAllRepositoriesSubmit.click();
        return new ImportRepositoriesPage();
    }

    public LiveTableElement getRepositoriesLiveTable()
    {
        LiveTableElement lt = new LiveTableElement("repositories");
        lt.waitUntilReady();
        return lt;
    }
}
