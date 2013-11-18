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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

@Unstable
public class GitHubRepository
{
    private String organizationId;
    private String repositoryId;

    public GitHubRepository(String organizationId, String repositoryId)
    {
        this.organizationId = organizationId;
        this.repositoryId = repositoryId;
    }

    public String getOrganizationId()
    {
        return this.organizationId;
    }

    public String getRepositoryId()
    {
        return this.repositoryId;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("organizationId", getOrganizationId())
            .append("repositoryId", getRepositoryId())
            .toString();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(9, 3)
            .append(getOrganizationId())
            .append(getRepositoryId())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        GitHubRepository rhs = (GitHubRepository) object;
        return new EqualsBuilder()
            .append(getOrganizationId(), rhs.getOrganizationId())
            .append(getRepositoryId(), rhs.getRepositoryId())
            .isEquals();
    }
}
