/*
 * Copyright 2017 Kafdrop contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package kafdrop.model;

import java.util.Objects;

public final class AclVO implements Comparable<AclVO>{
    private final String name;
    private final String resourceType;
    private final String patternType;

    private final String principal;
    private final String host;
    private final String operation;
    private final String permissionType;

    public AclVO(String resourceType, String name, String patternType, String principal, String host, String operation, String permissionType) {
        this.resourceType = resourceType;
        this.name = name;
        this.patternType = patternType;
        this.principal = principal;
        this.host = host;
        this.operation = operation;
        this.permissionType = permissionType;
    }

    public String getName() {
        return name;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getPatternType() {
        return patternType;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getHost() {
        return host;
    }

    public String getOperation() {
        return operation;
    }

    public String getPermissionType() {
        return permissionType;
    }

    @Override
    public int compareTo(AclVO that) { return this.name.compareTo(that.name) ; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AclVO aclVO = (AclVO) o;
        return name.equals(aclVO.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
