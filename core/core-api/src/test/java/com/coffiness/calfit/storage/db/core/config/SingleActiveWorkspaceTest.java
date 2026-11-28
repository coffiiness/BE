package com.coffiness.calfit.storage.db.core.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.coffiness.calfit.core.api.controller.ApiControllerAdvice;
import com.coffiness.calfit.core.api.controller.v1.calendar.CalendarConnectController;
import com.coffiness.calfit.core.api.facade.calendar.CalendarConnectFacade;
import com.coffiness.calfit.core.api.facade.workspace.InvitationFacade;
import com.coffiness.calfit.core.api.facade.workspace.WorkspaceCreationFacade;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.*;
import com.coffiness.calfit.domain.billing.event.BillingEventHandler;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionStore;
import com.coffiness.calfit.domain.event.CalendarWatchStopRequestedEvent;
import com.coffiness.calfit.domain.user.User;
import com.coffiness.calfit.domain.user.UserService;
import com.coffiness.calfit.domain.workspace.Workspace;
import com.coffiness.calfit.domain.workspace.WorkspaceService;
import com.coffiness.calfit.domain.workspace.invitation.InvitationService;
import com.coffiness.calfit.domain.workspace.member.MemberService;
import com.coffiness.calfit.infra.*;
import com.coffiness.calfit.model.GoogleCalendarWatchResult;
import com.coffiness.calfit.model.OAuthExchangeResult;
import com.coffiness.calfit.port.GoogleCalendarPort;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.coffiness.calfit.storage.db.core.invitation.WorkspaceInvitationEntity;
import com.coffiness.calfit.storage.db.core.invitation.WorkspaceInvitationRepository;
import com.coffiness.calfit.storage.db.core.member.ActiveMembershipPolicy;
import com.coffiness.calfit.storage.db.core.member.MemberRepository;
import com.coffiness.calfit.storage.db.core.user.UserEntity;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import com.coffiness.calfit.storage.db.core.workspace.WorkspaceRepository;
import com.coffiness.calfit.support.email.EmailProperties;
import com.coffiness.calfit.support.email.EmailService;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.event.DomainEventPublisher;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import com.coffiness.calfit.v1.request.CalendarConnectRequest;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Uses only a new, explicitly named local QA database; Google and mail ports are mocked. */
@SpringBootTest(
    classes = SingleActiveWorkspaceTest.Config.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"local", "single-workspace-test"})
class SingleActiveWorkspaceTest {
  private static final String REDIRECT = "http://localhost:5173/auth/callback";
  @Autowired private WorkspaceCreationFacade creation;
  @Autowired private MemberService members;
  @Autowired private InvitationFacade invitations;
  @Autowired private InvitationService invitationService;
  @Autowired private UserRepository users;
  @Autowired private MemberRepository memberships;
  @Autowired private WorkspaceRepository workspaces;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private GoogleCalendarOAuthStateService states;
  @Autowired private CalendarConnectController calendarController;
  @Autowired private CalendarConnectFacade calendarFacade;
  @Autowired private CalendarConnectionLifecycleService lifecycle;
  @Autowired private ExternalCalendarStore calendars;
  @Autowired private ExternalCalendarReader calendarReader;
  @Autowired private GoogleCalendarTokenService tokens;
  @Autowired private GoogleChannelTokenService channels;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private WorkspaceInvitationRepository invitationRepository;
  @MockitoSpyBean private MemberStoreImpl memberStore;
  @MockitoBean private UserService userService;
  @MockitoBean private EmailService email;
  @MockitoBean private SubscriptionStore subscriptions;
  @MockitoBean private GoogleOAuthPort oauth;
  @MockitoBean private GoogleCalendarPort google;
  @MockitoBean private ScheduleService schedules;
  @MockitoBean private ScheduleReader scheduleReader;
  @MockitoBean private ScheduleStore scheduleStore;

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    String url = System.getenv("CALFIT_QA_MYSQL_URL");
    if (url != null
        && !url.matches(
            "jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/calfit_membership_qa(?:_migration)?(?:\\?.*)?"))
      throw new IllegalArgumentException(
          "Only a loopback calfit_membership_qa database is allowed");
    boolean mysql = url != null;
    properties.add(
        "spring.datasource.url",
        () -> mysql ? url : "jdbc:h2:mem:single_membership;MODE=MySQL;DB_CLOSE_DELAY=-1");
    properties.add(
        "spring.datasource.driver-class-name",
        () -> mysql ? "com.mysql.cj.jdbc.Driver" : "org.h2.Driver");
    properties.add("spring.datasource.username", () -> mysql ? "root" : "sa");
    properties.add("spring.datasource.password", () -> mysql ? "synthetic-membership-root" : "");
    properties.add(
        "spring.jpa.hibernate.ddl-auto",
        () ->
            mysql
                ? ("validate".equals(System.getenv("CALFIT_QA_SCHEMA_MODE"))
                    ? "validate"
                    : "update")
                : "create");
    properties.add("spring.jpa.open-in-view", () -> "false");
    properties.add(
        "spring.security.oauth2.client.registration.google.client-id", () -> "mock-google-client");
    properties.add("calendar.google-sync.allowed-redirect-uris", () -> REDIRECT);
    properties.add(
        "calendar.google-sync.watch-token-secret", () -> "synthetic-local-watch-secret-only");
    properties.add(
        "calendar.google-sync.watch-callback-url", () -> "https://mock.invalid/notifications");
  }

  @BeforeEach
  void mockUserAndGoogle() {
    doAnswer(
            call -> {
              UserEntity entity = users.findById(call.getArgument(0)).orElseThrow();
              return new User(
                  entity.getId(),
                  entity.getEmail(),
                  entity.getName(),
                  "MEMBER",
                  entity.getCreatedAt());
            })
        .when(userService)
        .getUser(anyLong());
    when(oauth.exchangeAuthorizationCode(anyString(), anyString()))
        .thenReturn(
            new OAuthExchangeResult(
                "mock-access",
                3600L,
                "mock-refresh",
                "header.eyJlbWFpbCI6Im1vY2tAZXhhbXBsZS5pbnZhbGlkIn0.signature"));
    when(google.resolveWorkspaceCalendarId(anyString(), anyString(), anyString()))
        .thenAnswer(call -> "calendar-" + call.getArgument(1));
    when(google.watchEvents(anyString(), anyString(), anyString(), anyString(), anyLong()))
        .thenReturn(
            new GoogleCalendarWatchResult(
                "mock-channel", "mock-resource", LocalDateTime.now().plusDays(1)));
    when(scheduleReader.readAllOwnedSchedules(anyLong())).thenReturn(List.of());
  }

  @AfterEach
  void clearTenant() {
    TenantContext.clear();
  }

  @Test
  void createsWorkspaceAndHrAtomicallyAndPublishesTrialAfterCommit() {
    UserEntity user = user();
    Workspace workspace = creation.create(user.getId(), "new workspace", "", "1-10");
    assertThat(memberships.findTenantIdByUserId(user.getId())).contains(workspace.workspaceId());
    assertThat(in(workspace.workspaceId(), () -> members.getMember(user.getId()).memberType()))
        .isEqualTo(MemberType.HR);
    verify(subscriptions).createTrialSubscription(workspace.workspaceId());
    assertThat(TenantContext.getTenantId()).isNull();
  }

  @Test
  void rejectsAdditionalWorkspaceWithoutCreatingOrphanOrTrial() {
    UserEntity user = user();
    Workspace first = create(user);
    long before = workspaces.count();
    assertConflict(
        () -> creation.create(user.getId(), "forbidden", "", ""), "ACTIVE_WORKSPACE_EXISTS");
    assertThat(workspaces.count()).isEqualTo(before);
    verify(subscriptions, times(1)).createTrialSubscription(first.workspaceId());
  }

  @Test
  void rollsBackWorkspaceAndTrialWhenHrRegistrationFails() {
    UserEntity user = user();
    long before = workspaces.count();
    doThrow(new IllegalStateException("synthetic member write failure"))
        .when(
            (MemberStoreImpl)
                org.springframework.test.util.AopTestUtils.getUltimateTargetObject(memberStore))
        .save(user.getId(), MemberType.HR);
    assertThatThrownBy(() -> create(user)).isInstanceOf(IllegalStateException.class);
    assertThat(workspaces.count()).isEqualTo(before);
    assertThat(memberships.findTenantIdByUserId(user.getId())).isEmpty();
    verifyNoInteractions(subscriptions);
  }

  @Test
  void commonRegistrationCannotBypassSameOrOtherWorkspacePolicy() {
    UserEntity user = user();
    Workspace first = create(user);
    Workspace second = create(user());
    assertConflict(
        () ->
            in(
                first.workspaceId(),
                () -> members.registerMember(user.getId(), MemberType.INTERVIEWER)),
        "WORKSPACE_ALREADY_JOINED");
    assertConflict(
        () ->
            in(
                second.workspaceId(),
                () -> members.registerMember(user.getId(), MemberType.INTERVIEWER)),
        "ACTIVE_WORKSPACE_EXISTS");
    assertThat(activeCount(user)).isEqualTo(1);
  }

  @Test
  void commonRegistrationRejectsMissingOrDeletedWorkspace() {
    UserEntity user = user();
    assertThatThrownBy(() -> members.registerMember(user.getId(), MemberType.HR))
        .isInstanceOf(CoreException.class);
    assertThatThrownBy(
            () ->
                in(
                    UUID.randomUUID().toString(),
                    () -> members.registerMember(user.getId(), MemberType.HR)))
        .isInstanceOf(CoreException.class);
    Workspace workspace = create(user());
    workspaces
        .findByWorkspaceId(workspace.workspaceId())
        .ifPresent(
            entity -> {
              entity.deleted();
              workspaces.save(entity);
            });
    assertThatThrownBy(
            () ->
                in(
                    workspace.workspaceId(),
                    () -> members.registerMember(user.getId(), MemberType.HR)))
        .isInstanceOf(CoreException.class);
    assertThat(activeCount(user)).isZero();
  }

  @Test
  void simultaneousInvitationsProduceOneMembershipAndKeepLosingInvitationPending() {
    UserEntity target = user();
    Workspace a = create(user());
    Workspace b = create(user());
    String ta = invitation(target, a, LocalDateTime.now().plusHours(1));
    String tb = invitation(target, b, LocalDateTime.now().plusHours(1));
    assertRace(() -> accept(target, a, ta), () -> accept(target, b, tb));
    assertThat(activeCount(target)).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from workspace_invitations where token in (?, ?) and invitation_status='ACCEPTED'",
                Long.class,
                ta,
                tb))
        .isEqualTo(1);
  }

  @Test
  void unrelatedUsersCanCreateWorkspacesConcurrently() {
    UserEntity a = user();
    UserEntity b = user();
    assertThat(concurrentResults(() -> create(a), () -> create(b)))
        .containsExactly("success", "success");
    assertThat(activeCount(a)).isEqualTo(1);
    assertThat(activeCount(b)).isEqualTo(1);
  }

  @Test
  void createAndAcceptRaceCannotCreateTwoActiveMemberships() {
    UserEntity target = user();
    Workspace workspace = create(user());
    String token = invitation(target, workspace, LocalDateTime.now().plusHours(1));
    long before = workspaces.count();
    assertRace(() -> create(target), () -> accept(target, workspace, token));
    String actual = memberships.findTenantIdByUserId(target.getId()).orElseThrow();
    assertThat(activeCount(target)).isEqualTo(1);
    assertThat(workspaces.count())
        .isEqualTo(before + (actual.equals(workspace.workspaceId()) ? 0 : 1));
  }

  @Test
  void sameInvitationIsAcceptedOnlyOnceEvenConcurrently() {
    UserEntity target = user();
    Workspace workspace = create(user());
    String token = invitation(target, workspace, LocalDateTime.now().plusHours(1));
    assertRace(() -> accept(target, workspace, token), () -> accept(target, workspace, token));
    assertThat(activeCount(target)).isEqualTo(1);
  }

  @Test
  void acceptanceRechecksEmailExpiryStatusAndDestinationInTransaction() {
    UserEntity target = user();
    Workspace a = create(user());
    Workspace b = create(user());
    String expired = invitation(target, a, LocalDateTime.now().minusMinutes(1));
    assertConflict(() -> accept(target, a, expired), "INVITATION_UNAVAILABLE");
    String token = invitation(target, a, LocalDateTime.now().plusHours(1));
    assertConflict(() -> accept(user(), a, token), "INVITATION_EMAIL_MISMATCH");
    assertConflict(() -> accept(target, b, token), "TENANT_MISMATCH");
    jdbc.update(
        "update workspace_invitations set invitation_status='ACCEPTED' where token=?", token);
    assertConflict(() -> accept(target, a, token), "INVITATION_UNAVAILABLE");
    assertThat(activeCount(target)).isZero();
  }

  @Test
  void invitationCreationRejectsExistingMemberAndSpoofedWorkspace() {
    UserEntity owner = user();
    Workspace a = create(owner);
    UserEntity other = user();
    create(other);
    assertConflict(
        () ->
            in(
                a.workspaceId(),
                () ->
                    invitationService.createInvitation(
                        a.workspaceId(), owner.getEmail(), MemberType.HR, owner.getId())),
        "WORKSPACE_ALREADY_JOINED");
    assertConflict(
        () ->
            in(
                a.workspaceId(),
                () ->
                    invitationService.createInvitation(
                        a.workspaceId(), other.getEmail(), MemberType.HR, owner.getId())),
        "ACTIVE_WORKSPACE_EXISTS");
    assertConflict(
        () ->
            in(
                "forged",
                () ->
                    invitationService.createInvitation(
                        a.workspaceId(), user().getEmail(), MemberType.HR, owner.getId())),
        "TENANT_MISMATCH");
    verifyNoInteractions(email);
  }

  @Test
  void leaveAndRejoinPreserveMultipleDeletedHistories() {
    Workspace a = create(user());
    UserEntity target = user();
    for (int i = 0; i < 2; i++) {
      in(a.workspaceId(), () -> members.registerMember(target.getId(), MemberType.INTERVIEWER));
      in(
          a.workspaceId(),
          () -> {
            members.leaveWorkspace(target.getId());
            return null;
          });
    }
    Workspace newWorkspace = create(target);
    assertThat(memberships.findTenantIdByUserId(target.getId()))
        .contains(newWorkspace.workspaceId());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from workspace_members where user_id=? and status='DELETED' and active_user_id is null",
                Long.class,
                target.getId()))
        .isEqualTo(2);
  }

  @Test
  void lastHrCannotLeaveOrBeRemoved() {
    UserEntity owner = user();
    Workspace workspace = create(owner);
    Long memberId = in(workspace.workspaceId(), () -> members.getMember(owner.getId()).id());
    assertConflict(
        () ->
            in(
                workspace.workspaceId(),
                () -> {
                  members.updateMemberType(memberId, MemberType.INTERVIEWER);
                  return null;
                }),
        "LAST_HR_CANNOT_LEAVE");
    assertConflict(
        () ->
            in(
                workspace.workspaceId(),
                () -> {
                  members.leaveWorkspace(owner.getId());
                  return null;
                }),
        "LAST_HR_CANNOT_LEAVE");
    assertConflict(
        () ->
            in(
                workspace.workspaceId(),
                () -> {
                  members.removeMember(-1L, memberId);
                  return null;
                }),
        "LAST_HR_CANNOT_LEAVE");
    assertThat(activeCount(owner)).isEqualTo(1);
  }

  @Test
  void databaseUniqueIndexStopsDirectWritesAndMapsTo409() {
    UserEntity target = user();
    Workspace workspace = create(target);
    DataIntegrityViolationException failure =
        catchThrowableOfType(
            () -> insertMembership(target, UUID.randomUUID().toString()),
            DataIntegrityViolationException.class);
    assertThat(failure).isNotNull();
    assertThat(
            new ApiControllerAdvice()
                .handleIntegrityViolation(failure, new MockHttpServletRequest())
                .getStatusCode()
                .value())
        .isEqualTo(409);
    assertThat(memberships.findTenantIdByUserId(target.getId())).contains(workspace.workspaceId());
    assertThat(
            new ApiControllerAdvice()
                .handleIntegrityViolation(
                    new DataIntegrityViolationException("other constraint"),
                    new MockHttpServletRequest())
                .getStatusCode()
                .value())
        .isEqualTo(500);
  }

  @Test
  void legacyDuplicateDataFailsExplicitlyInsteadOfSelectingFirst() {
    UserEntity target = user();
    boolean mysql = System.getenv("CALFIT_QA_MYSQL_URL") != null;
    jdbc.execute(
        mysql
            ? "alter table workspace_members drop index uq_workspace_members_active_user"
            : "alter table workspace_members drop constraint uq_workspace_members_active_user");
    try {
      insertMembership(target, UUID.randomUUID().toString());
      insertMembership(target, UUID.randomUUID().toString());
      assertConflict(
          () -> memberships.findTenantIdByUserId(target.getId()), "MULTIPLE_ACTIVE_MEMBERSHIPS");
    } finally {
      // Retain both synthetic legacy histories while restoring the constraint.
      jdbc.update("update workspace_members set status='DELETED' where user_id=?", target.getId());
      jdbc.execute(
          "alter table workspace_members add constraint uq_workspace_members_active_user unique(active_user_id)");
    }
  }

  @Test
  void oauthBindsCorrectWorkspaceAndHibernateSessionAndRejectsReplay() {
    UserEntity owner = user();
    Workspace workspace = create(owner);
    in(
        workspace.workspaceId(),
        () -> {
          var authorization = states.issue(owner.getId(), REDIRECT);
          when(google.resolveWorkspaceCalendarId(
                  anyString(), eq(workspace.workspaceId()), anyString()))
              .thenAnswer(
                  call -> {
                    var em =
                        EntityManagerFactoryUtils.getTransactionalEntityManager(
                            entityManagerFactory);
                    assertThat(em).isNotNull();
                    assertThat(em.unwrap(Session.class).getTenantIdentifierValue())
                        .isEqualTo(workspace.workspaceId());
                    return "mock-calendar-" + workspace.workspaceId();
                  });
          CalendarConnectRequest request =
              new CalendarConnectRequest("mock-code", REDIRECT, authorization.state());
          calendarController.connectGoogleCalendar(
              request, new SecurityUser(owner.getId(), owner.getEmail(), "MEMBER"));
          assertThat(calendarFacade.isConnected(owner.getId())).isTrue();
          assertThat(
                  jdbc.queryForObject(
                      "select tenant_id from external_calendars where user_id=?",
                      String.class,
                      owner.getId()))
              .isEqualTo(workspace.workspaceId());
          assertConflict(
              () ->
                  calendarController.connectGoogleCalendar(
                      request, new SecurityUser(owner.getId(), owner.getEmail(), "MEMBER")),
              "GOOGLE_OAUTH_STATE_INVALID");
          verify(oauth, times(1)).exchangeAuthorizationCode("mock-code", REDIRECT);
          return null;
        });
  }

  @Test
  void oauthUpstreamFailureDoesNotMakeStateReusable() {
    UserEntity owner = user();
    Workspace workspace = create(owner);
    in(
        workspace.workspaceId(),
        () -> {
          var authorization = states.issue(owner.getId(), REDIRECT);
          when(oauth.exchangeAuthorizationCode(anyString(), anyString()))
              .thenThrow(new IllegalStateException("GOOGLE_MOCK_FAILURE"));
          var request = new CalendarConnectRequest("failing-code", REDIRECT, authorization.state());
          assertThatThrownBy(
                  () ->
                      calendarController.connectGoogleCalendar(
                          request, new SecurityUser(owner.getId(), owner.getEmail(), "MEMBER")))
              .isInstanceOf(IllegalStateException.class);
          assertConflict(
              () ->
                  calendarController.connectGoogleCalendar(
                      request, new SecurityUser(owner.getId(), owner.getEmail(), "MEMBER")),
              "GOOGLE_OAUTH_STATE_INVALID");
          verify(oauth, times(1)).exchangeAuthorizationCode("failing-code", REDIRECT);
          return null;
        });
  }

  @Test
  void concurrentStateConsumptionSucceedsExactlyOnce() {
    UserEntity owner = user();
    Workspace workspace = create(owner);
    var state = in(workspace.workspaceId(), () -> states.issue(owner.getId(), REDIRECT));
    assertRace(
        () ->
            in(
                workspace.workspaceId(),
                () -> states.consume(owner.getId(), state.state(), REDIRECT)),
        () ->
            in(
                workspace.workspaceId(),
                () -> states.consume(owner.getId(), state.state(), REDIRECT)));
  }

  @Test
  void leaveDisablesCalendarAndPendingStateBeforeRejoinAndWebhook() {
    UserEntity owner = user();
    Workspace workspace = create(owner);
    UserEntity target = user();
    in(
        workspace.workspaceId(),
        () -> {
          members.registerMember(target.getId(), MemberType.INTERVIEWER);
          var state = states.issue(target.getId(), REDIRECT);
          ExternalCalendar calendar =
              new TransactionTemplate(transactionManager)
                  .execute(
                      status ->
                          calendars.store(
                              new ExternalCalendar(
                                  null,
                                  target.getId(),
                                  "mock-departure",
                                  "mock-access",
                                  "mock-refresh",
                                  LocalDateTime.now().plusHours(1),
                                  null,
                                  true,
                                  "departure-channel",
                                  "departure-resource",
                                  LocalDateTime.now().plusHours(1))));
          assertThat(calendar).isNotNull();
          members.leaveWorkspace(target.getId());
          assertThat(calendarReader.read(calendar.id()).isSyncEnabled()).isFalse();
          assertThatThrownBy(() -> tokens.getValidAccessToken(calendar.id()))
              .isInstanceOf(IllegalStateException.class);
          members.registerMember(target.getId(), MemberType.INTERVIEWER);
          assertConflict(
              () -> states.consume(target.getId(), state.state(), REDIRECT),
              "GOOGLE_OAUTH_STATE_INVALID");
          calendarFacade.handleGoogleCalendarNotification(
              "departure-channel",
              "departure-resource",
              channels.createToken(workspace.workspaceId(), calendar.id()),
              "exists");
          verifyNoInteractions(oauth, google);
          return null;
        });
  }

  @Test
  void watchStopIsSafeAndRetainsMetadataOnFailure() {
    UserEntity owner = user();
    Workspace workspace = create(owner);
    in(
        workspace.workspaceId(),
        () -> {
          ExternalCalendar calendar =
              new TransactionTemplate(transactionManager)
                  .execute(
                      status ->
                          calendars.store(
                              new ExternalCalendar(
                                  null,
                                  owner.getId(),
                                  "mock-watch",
                                  "mock-access",
                                  "mock-refresh",
                                  LocalDateTime.now().plusHours(1),
                                  null,
                                  true,
                                  "watch-id",
                                  "resource-id",
                                  LocalDateTime.now().plusHours(1))));
          assertThat(calendar).isNotNull();
          var event =
              new CalendarWatchStopRequestedEvent(
                  workspace.workspaceId(), calendar.id(), "watch-id", "resource-id", 1L);
          lifecycle.stopDisabledWatch(event);
          verifyNoInteractions(google);
          lifecycle.disconnectUser(owner.getId());
          doThrow(new IllegalStateException("mock network error"))
              .when(google)
              .stopWatch(anyString(), anyString(), anyString());
          lifecycle.stopDisabledWatch(event);
          assertThat(calendarReader.read(calendar.id()).channelId()).isEqualTo("watch-id");
          doNothing().when(google).stopWatch(anyString(), anyString(), anyString());
          lifecycle.stopDisabledWatch(event);
          assertThat(calendarReader.read(calendar.id()).channelId()).isNull();
          assertThat(calendarReader.read(calendar.id()).isSyncEnabled()).isFalse();
          return null;
        });
  }

  private UserEntity user() {
    return users.save(
        UserEntity.createMember(
            UUID.randomUUID() + "@mock.invalid", "not-a-real-password", "QA member"));
  }

  private Workspace create(UserEntity user) {
    return creation.create(user.getId(), "QA " + UUID.randomUUID(), "", "1-10");
  }

  private long activeCount(UserEntity user) {
    return java.util.Objects.requireNonNull(
        jdbc.queryForObject(
            "select count(*) from workspace_members where user_id=? and status='ACTIVE'",
            Long.class,
            user.getId()));
  }

  private void insertMembership(UserEntity user, String tenant) {
    jdbc.update(
        "insert into workspace_members (user_id, tenant_id, member_type, status, created_at, updated_at) values (?, ?, 'HR', 'ACTIVE', current_timestamp, current_timestamp)",
        user.getId(),
        tenant);
  }

  private String invitation(UserEntity user, Workspace workspace, LocalDateTime expires) {
    String token = UUID.randomUUID().toString();
    invitationRepository.save(
        WorkspaceInvitationEntity.create(
            workspace.workspaceId(),
            user.getEmail(),
            token,
            MemberType.INTERVIEWER,
            user.getId(),
            expires));
    return token;
  }

  private Object accept(UserEntity user, Workspace workspace, String token) {
    return in(
        workspace.workspaceId(),
        () -> {
          invitations.acceptInvitation(token, user.getId(), MemberType.HR);
          return null;
        });
  }

  private <T> T in(String tenant, Supplier<T> action) {
    String previous = TenantContext.getTenantId();
    TenantContext.setTenantId(tenant);
    try {
      return action.get();
    } finally {
      if (previous == null) TenantContext.clear();
      else TenantContext.setTenantId(previous);
    }
  }

  private void assertConflict(Callable<?> action, String code) {
    assertThatThrownBy(action::call)
        .isInstanceOfSatisfying(
            CoreException.class, error -> assertThat(error.getCustomCode()).isEqualTo(code));
  }

  private void assertRace(Callable<?> first, Callable<?> second) {
    assertThat(concurrentResults(first, second)).containsExactlyInAnyOrder("success", "conflict");
  }

  private List<String> concurrentResults(Callable<?> first, Callable<?> second) {
    var executor = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      var a = executor.submit(() -> raceResult(start, first));
      var b = executor.submit(() -> raceResult(start, second));
      start.countDown();
      return List.of(a.get(45, TimeUnit.SECONDS), b.get(45, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
      throw new AssertionError(e);
    } finally {
      executor.shutdownNow();
    }
  }

  private String raceResult(CountDownLatch start, Callable<?> action) {
    try {
      if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("race start timeout");
      action.call();
      return "success";
    } catch (CoreException e) {
      assertThat(e.getErrorType().getStatus().value()).isEqualTo(409);
      return "conflict";
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError(e);
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      TenantContext.clear();
    }
  }

  @Configuration(proxyBeanMethods = false)
  @Profile("single-workspace-test")
  @EnableAutoConfiguration
  @Import({
    CoreJpaConfig.class,
    TenantIdentifierResolver.class,
    ActiveMembershipPolicy.class,
    MemberStoreImpl.class,
    MemberReaderImpl.class,
    WorkspaceReaderImpl.class,
    WorkspaceService.class,
    MemberService.class,
    WorkspaceCreationFacade.class,
    InvitationService.class,
    InvitationFacade.class,
    DomainEventPublisher.class,
    BillingEventHandler.class,
    ExternalCalendarStoreImpl.class,
    ExternalCalendarReaderImpl.class,
    GoogleCalendarTokenService.class,
    GoogleCalendarOAuthStateService.class,
    CalendarConnectionLifecycleService.class,
    CalendarConnectService.class,
    CalendarConnectFacade.class,
    CalendarConnectController.class,
    GoogleChannelTokenService.class
  })
  static class Config {
    @Bean
    Clock clock() {
      return Clock.systemUTC();
    }

    @Bean
    ZoneId appZoneId() {
      return ZoneId.of("Asia/Seoul");
    }

    @Bean
    EmailProperties emailProperties() {
      return new EmailProperties();
    }
  }
}
