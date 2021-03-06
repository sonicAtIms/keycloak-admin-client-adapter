/*
 * Copyright (C) open knowledge GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package de.openknowledge.authentication.domain;

import java.security.KeyPair;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.token.TokenService;
import org.keycloak.representations.idm.RealmRepresentation;

import de.openknowledge.authentication.domain.registration.Token;
import de.openknowledge.authentication.domain.registration.TokenSecret;
import de.openknowledge.authentication.domain.registration.VerificationLink;

@ApplicationScoped
public class KeycloakAdapter {

  private Keycloak keycloak;

  private TokenService tokenService;

  private KeycloakKeyConfiguration keyConfiguration;

  private KeyPair keyPair;

  private TokenSecret tokenSecret;

  protected KeycloakAdapter() {
    // for framework
  }

  @Inject
  public KeycloakAdapter(KeycloakAdapterConfiguration adapterConfig,
      KeycloakKeyConfiguration keyConfig) {
    ResteasyClient restClient = new ResteasyClientBuilder().connectionPoolSize(adapterConfig.getConnectionPoolSize()).build();
    keycloak = KeycloakBuilder.builder()
        .serverUrl(adapterConfig.getServerUrl())
        .realm(adapterConfig.getMasterRealm())
        .grantType(adapterConfig.getGrantType())
        .username(adapterConfig.getUsername())
        .password(adapterConfig.getPassword())
        .clientId(adapterConfig.getClientId())
        .resteasyClient(restClient)
        .build();
    tokenService = restClient.target(adapterConfig.getServerUrl()).proxy(TokenService.class);
    keyConfiguration = keyConfig;
  }

  @PostConstruct
  public void init() {
    keyPair = KeycloakKeyService.readKeyPair(keyConfiguration);
    tokenSecret = TokenSecret.fromValue(keyConfiguration.getTokenSecret());
  }

  public List<RealmRepresentation> findAll() {
    RealmsResource realmsResource = keycloak.realms();
    return realmsResource.findAll();
  }

  public UsersResource findUserResource(RealmName realmName) {
    RealmResource realmResource = keycloak.realm(realmName.getValue());
    return realmResource.users();
  }

  public GroupsResource findGroupResource(RealmName realmName) {
    RealmResource realmResource = keycloak.realm(realmName.getValue());
    return realmResource.groups();
  }

  public RolesResource findRoleResource(RealmName realmName) {
    RealmResource realmResource = keycloak.realm(realmName.getValue());
    return realmResource.roles();
  }

  public VerificationLink encode(Token token) {
    return KeycloakTokenService.encode(token, tokenSecret, keyPair.getPublic());
  }

  public Token decode(VerificationLink link) {
    return KeycloakTokenService.decode(link, tokenSecret, keyPair.getPrivate());
  }

  public TokenService getTokenService() {
    return tokenService;
  }
}
