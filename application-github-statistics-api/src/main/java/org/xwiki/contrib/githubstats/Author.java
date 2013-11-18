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
public class Author
{
    private String id;

    private String email;

    public Author(String id, String email)
    {
        this.id = id;
        this.email = email;
    }

    public String getId()
    {
        return id;
    }

    public String getEmail()
    {
        return email;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("id", getId())
            .append("email", getEmail())
            .toString();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(11, 11)
            .append(getId())
            .append(getEmail())
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
        Author rhs = (Author) object;
        return new EqualsBuilder()
            .append(getId(), rhs.getId())
            .append(getEmail(), rhs.getEmail())
            .isEquals();
    }
}
