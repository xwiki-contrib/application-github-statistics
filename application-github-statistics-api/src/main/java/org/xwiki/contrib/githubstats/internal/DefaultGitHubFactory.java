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

import java.io.IOException;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.github.GitHub;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.githubstats.GitHubFactory;

@Component
@Singleton
public class DefaultGitHubFactory implements GitHubFactory
{
    @Override
    public GitHub createGitHub() throws IOException
    {
        return GitHub.connectAnonymously();
    }

    @Override
    public GitHub createGitHub(String login, String authToken) throws IOException
    {
        GitHub gitHub;
        if (StringUtils.isEmpty(login) && StringUtils.isEmpty(authToken)) {
            gitHub = createGitHub();
        } else {
            gitHub = GitHub.connect(login, authToken);
        }

        return gitHub;
    }
}
