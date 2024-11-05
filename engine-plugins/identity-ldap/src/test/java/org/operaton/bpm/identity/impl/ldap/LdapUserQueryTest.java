/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.operaton.bpm.identity.impl.ldap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.operaton.bpm.engine.AuthorizationService;
import org.operaton.bpm.engine.BadUserRequestException;
import org.operaton.bpm.engine.IdentityService;
import org.operaton.bpm.engine.ProcessEngineConfiguration;
import org.operaton.bpm.engine.authorization.Authorization;
import org.operaton.bpm.engine.authorization.Permission;
import org.operaton.bpm.engine.authorization.Resource;
import org.operaton.bpm.engine.identity.User;
import org.operaton.bpm.engine.identity.UserQuery;
import org.operaton.bpm.engine.test.junit5.ProcessEngineExtension;
import org.operaton.bpm.identity.ldap.util.LdapTestEnvironment;
import org.operaton.bpm.identity.ldap.util.LdapTestEnvironmentExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.operaton.bpm.engine.authorization.Authorization.AUTH_TYPE_GRANT;
import static org.operaton.bpm.engine.authorization.Permissions.READ;
import static org.operaton.bpm.engine.authorization.Resources.USER;
import static org.operaton.bpm.identity.ldap.util.LdapTestUtilities.*;

@ExtendWith(ProcessEngineExtension.class)
@ExtendWith(LdapTestEnvironmentExtension.class)
public class LdapUserQueryTest {

  ProcessEngineConfiguration processEngineConfiguration;
  IdentityService identityService;
  AuthorizationService authorizationService;
  LdapTestEnvironment ldapTestEnvironment;

  @Test
  void countUsers() {
    // given

    // when
    UserQuery userQuery = identityService.createUserQuery();

    // then
    assertThat(userQuery.listPage(0, Integer.MAX_VALUE)).hasSize(12);
    assertThat(userQuery.count()).isEqualTo(12);
  }

  @Test
  void queryNoFilter() {
    // given

    // when
    List<User> result = identityService.createUserQuery().list();

    // then
    assertThat(result).hasSize(ldapTestEnvironment.getTotalNumberOfUsersCreated());
  }

  @Test
  void filterByUserId() {
    // given

    // when
    User user = identityService.createUserQuery().userId("oscar").singleResult();

    // then
    assertThat(user).isNotNull();

    // validate user
    assertThat(user.getId()).isEqualTo("oscar");
    assertThat(user.getFirstName()).isEqualTo("Oscar");
    assertThat(user.getLastName()).isEqualTo("The Crouch");
    assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void filterByNonexistentUserId() {
    // given

    // when
    User user = identityService.createUserQuery().userId("non-existing").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void filterByUserIdIn() {
    // given

    // when
    List<User> users = identityService.createUserQuery().userIdIn("oscar", "monster").list();

    // then
    assertThat(users).hasSize(2);
    assertThat(users).extracting("id").containsOnly("oscar", "monster");
  }

  @Test
  void filterByNonExistingUserIdIn() {
    // given

    // when
    List<User> users = identityService.createUserQuery().userIdIn("oscar", "monster", "daniel", "non-existing").list();

    // then
    assertThat(users)
      .isNotNull()
      .hasSize(3)
      .extracting("id").containsOnly("oscar", "monster", "daniel");
  }

  @Test
  void filterByUserIdWithCapitalization() {
    try {
      // given
      processEngineConfiguration.setAuthorizationEnabled(true);
      identityService.setAuthenticatedUserId("Oscar");

      // when
      User user = identityService.createUserQuery().userId("Oscar").singleResult();

      // then
      assertThat(user).isNotNull();

      // validate user
      assertThat(user.getId()).isEqualTo("oscar");
      assertThat(user.getFirstName()).isEqualTo("Oscar");
      assertThat(user.getLastName()).isEqualTo("The Crouch");
      assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
      identityService.clearAuthentication();
    }
  }

  @Test
  void filterByFirstname() {
    // given

    // when
    User user = identityService.createUserQuery().userFirstName("Oscar").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getFirstName()).isEqualTo("Oscar");
  }

  @Test
  void filterByNonexistingFirstname() {
    // given

    // when
    User user = identityService.createUserQuery().userFirstName("non-existing").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void filterByFirstnameLikeTrailingWildcard() {
    // given

    // when
    User user = identityService.createUserQuery().userFirstNameLike("Osc*").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getFirstName()).isEqualTo("Oscar");
  }

  @Test
  void filterByFirstnameLikeLeadingWildcard() {
    // given

    // when
    User user = identityService.createUserQuery().userFirstNameLike("*car").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getFirstName()).isEqualTo("Oscar");
  }

  @Test
  void filterByFirstnameLikeLeadingAndTrailingWildcard() {
    // given

    // when
    User user = identityService.createUserQuery().userFirstNameLike("*sca*").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getFirstName()).isEqualTo("Oscar");
  }

  @Test
  void filterByFirstnameLikeMiddleWildcard() {
    // given

    // when
    User user = identityService.createUserQuery().userFirstNameLike("O*ar").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getFirstName()).isEqualTo("Oscar");
  }

  @Test
  void filterByFirstnameLikeConvertFromDbWildcard() {
    // given

    // when using the SQL wildcard (%) instead of LDAP (*)
    User user = identityService.createUserQuery().userFirstNameLike("Osc%").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getFirstName()).isEqualTo("Oscar");
  }

  @Test
  void filterByNonexistingFirstnameLike() {
    // given

    // when
    User user = identityService.createUserQuery().userFirstNameLike("non-exist*").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void filterByLastname() {
    // given

    // when
    User user = identityService.createUserQuery().userLastName("The Crouch").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getLastName()).isEqualTo("The Crouch");
  }

  @Test
  void filterByNonexistingLastname() {
    // given

    // when
    User user = identityService.createUserQuery().userLastName("non-existing").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void filterByLastnameLikeTrailingWildcard() {
    // given

    // when
    User user = identityService.createUserQuery().userLastNameLike("The Cro*").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getLastName()).isEqualTo("The Crouch");
  }

  @Test
  void filterByLastnameLikeLeadingWildcard() {
    // given

    // when
    User user = identityService.createUserQuery().userLastNameLike("* Crouch").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getLastName()).isEqualTo("The Crouch");
  }

  @Test
  void filterByLastnameLikeLeadingAndTrailingWildcard() {
    // given

    // when
    User user = identityService.createUserQuery().userLastNameLike("* Cro*").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getLastName()).isEqualTo("The Crouch");
  }

  @Test
  void filterByLastnameLikeMiddleWildcard() {
    // given

    // when
    User user = identityService.createUserQuery().userLastNameLike("The *uch").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getLastName()).isEqualTo("The Crouch");
  }

  @Test
  void filterByNonexistingLastnameLike() {
    // given

    // when
    User user = identityService.createUserQuery().userLastNameLike("non-exist*").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void filterByLastnameLikeConvertFromDbWildcard() {
    // given

    // when using the SQL wildcard (%) instead of LDAP (*)
    User user = identityService.createUserQuery().userLastNameLike("The Cro%").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getLastName()).isEqualTo("The Crouch");
  }

  @Test
  void filterByEmail() {
    // given

    // when
    User user = identityService.createUserQuery().userEmail("oscar@operaton.org").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void filterByNonexistingEmail() {
    // given

    // when
    User user = identityService.createUserQuery().userEmail("non-exist").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void filterByEmailLikeTrailingWildCard() {
    // given

    // when
    User user = identityService.createUserQuery().userEmailLike("oscar@*").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void filterByEmailLikeLeadingWildCard() {
    // given

    // when
    User user = identityService.createUserQuery().userEmailLike("*car@operaton.org").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void filterByEmailLikeLeadingAndTrailingWildCard() {
    // given

    // when
    User user = identityService.createUserQuery().userEmailLike("*car@*").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void filterByEmailLikeMiddleWildCard() {
    // given

    // when
    User user = identityService.createUserQuery().userEmailLike("oscar@*.org").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void filterByNonexistingEmailLike() {
    // given

    // when
    User user = identityService.createUserQuery().userEmailLike("non-exist*").singleResult();

    // then
    assertThat(user).isNull();
  }

  @Test
  void filterByEmailLikeConvertFromDbWildcard() {
    // given

    // when using the SQL wildcard (%) instead of LDAP (*)
    User user = identityService.createUserQuery().userEmailLike("oscar@%").singleResult();

    // then
    assertThat(user).isNotNull();
    assertThat(user.getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void filterByGroupId() {
    // given

    // when
    List<User> result = identityService.createUserQuery().memberOfGroup("development").list();

    // then
    assertThat(result).hasSize(3);
    assertThat(result).extracting("id").containsOnly("roman", "daniel", "oscar");
  }

  @Test
  void filterByGroupIdAndFirstname() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("development")
            .userFirstName("Oscar")
            .list();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getFirstName()).isEqualTo("Oscar");
  }

  @Test
  void filterByGroupIdAndId() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("development")
            .userId("oscar")
            .list();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo("oscar");
  }

  @Test
  void filterByGroupIdAndLastname() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("development")
            .userLastName("The Crouch")
            .list();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getLastName()).isEqualTo("The Crouch");
  }

  @Test
  void filterByGroupIdAndEmail() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("development")
            .userEmail("oscar@operaton.org")
            .list();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getEmail()).isEqualTo("oscar@operaton.org");
  }

  @Test
  void filterByGroupIdAndEmailLike() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("development")
            .userEmailLike("*@operaton.org")
            .list();

    // then
    assertThat(result).hasSize(3);
    assertThat(result).extracting("email").containsOnly("daniel@operaton.org", "roman@operaton.org", "oscar@operaton.org");
  }

  @Test
  void filterByGroupIdAndIdForDnUsingCn() {
    // given

    // when
    List<User> result = identityService.createUserQuery()
            .memberOfGroup("external")
            .userId("fozzie")
            .list();

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getId()).isEqualTo("fozzie");
  }

  @Test
  void authenticatedUserSeesThemselve() {
    try {
      // given
      processEngineConfiguration.setAuthorizationEnabled(true);
      identityService.setAuthenticatedUserId("oscar");

      // when
      User user = identityService.createUserQuery().singleResult();

      // then
      assertThat(user).isNotNull();
      assertThat(user.getId()).isEqualTo("oscar");
    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
      identityService.clearAuthentication();
    }
  }

  @Test
  void nonexistingAuthenticatedUserDoesNotSeeThemselve() {
    try {
      // given
      processEngineConfiguration.setAuthorizationEnabled(true);
      identityService.setAuthenticatedUserId("non-existing");

      // when
      User user = identityService.createUserQuery().singleResult();

      // then
      assertThat(user).isNull();
    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
      identityService.clearAuthentication();
    }
  }

  @Test
  void pagination() {
    testUserPaging(identityService, ldapTestEnvironment);
  }

  @Test
  void paginationWithMemberOfGroup() {
    testUserPagingWithMemberOfGroup(identityService);
  }

  @Test
  void paginationWithAuthenticatedUser() {
    createGrantAuthorization(USER, "roman", "oscar", READ);
    createGrantAuthorization(USER, "daniel", "oscar", READ);
    createGrantAuthorization(USER, "monster", "oscar", READ);
    createGrantAuthorization(USER, "ruecker", "oscar", READ);

    try {
      processEngineConfiguration.setAuthorizationEnabled(true);

      identityService.setAuthenticatedUserId("oscar");

      Set<String> userNames = new HashSet<>();
      List<User> users = identityService.createUserQuery().listPage(0, 2);
      assertThat(users).hasSize(2);
      checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

      users = identityService.createUserQuery().listPage(2, 2);
      assertThat(users).hasSize(2);
      checkPagingResults(userNames, users.get(0).getId(), users.get(1).getId());

      users = identityService.createUserQuery().listPage(4, 2);
      assertThat(users).hasSize(1);
      assertThat(userNames).doesNotContain(users.get(0).getId());
      userNames.add(users.get(0).getId());

      identityService.setAuthenticatedUserId("daniel");

      users = identityService.createUserQuery().listPage(0, 2);
      assertThat(users).hasSize(1);

      assertThat(users.get(0).getId()).isEqualTo("daniel");

      users = identityService.createUserQuery().listPage(2, 2);
      assertThat(users).isEmpty();

    } finally {
      processEngineConfiguration.setAuthorizationEnabled(false);
      identityService.clearAuthentication();

      for (Authorization authorization : authorizationService.createAuthorizationQuery().list()) {
        authorizationService.deleteAuthorization(authorization.getId());
      }

    }
  }

  @Test
  void nativeQueryFail() {
    assertThatThrownBy(() -> identityService.createNativeUserQuery())
      .isInstanceOf(BadUserRequestException.class)
      .hasMessageContaining("Native user queries are not supported for LDAP");
  }

  protected void createGrantAuthorization(Resource resource, String resourceId, String userId, Permission... permissions) {
    Authorization authorization = createAuthorization(AUTH_TYPE_GRANT, resource, resourceId);
    authorization.setUserId(userId);
    for (Permission permission : permissions) {
      authorization.addPermission(permission);
    }
    authorizationService.saveAuthorization(authorization);
  }

  protected Authorization createAuthorization(int type, Resource resource, String resourceId) {
    Authorization authorization = authorizationService.createNewAuthorization(type);

    authorization.setResource(resource);
    if (resourceId != null) {
      authorization.setResourceId(resourceId);
    }

    return authorization;
  }

}
