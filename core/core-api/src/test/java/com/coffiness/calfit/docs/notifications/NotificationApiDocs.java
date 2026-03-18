package com.coffiness.calfit.docs.notifications;

import static com.coffiness.calfit.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.coffiness.calfit.core.api.controller.v1.NotificationController;
import com.coffiness.calfit.core.api.facade.notification.NotificationFacade;
import com.coffiness.calfit.core.api.facade.notification.NotificationSseFacade;
import com.coffiness.calfit.core.enums.NotificationTargetType;
import com.coffiness.calfit.core.enums.NotificationType;
import com.coffiness.calfit.docs.RestDocsTest;
import com.coffiness.calfit.domain.notification.Notification;
import com.coffiness.calfit.domain.notification.NotificationCategory;
import com.coffiness.calfit.domain.notification.NotificationPage;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Tag("restdocs")
public class NotificationApiDocs extends RestDocsTest {

  private final NotificationFacade notificationFacade = mock(NotificationFacade.class);
  private final NotificationSseFacade notificationSseFacade = mock(NotificationSseFacade.class);
  private final NotificationController notificationController =
      new NotificationController(notificationFacade, notificationSseFacade);
  private final SecurityUser mockSecurityUser = new SecurityUser(1L, "hr@calfit.com", "HR");

  @BeforeEach
  @Override
  public void setUp(RestDocumentationContextProvider restDocumentation) {
    super.setUp(restDocumentation);
    setUpMockMvc(
        notificationController,
        restDocumentation,
        new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(SecurityUser.class);
          }

          @Override
          public Object resolveArgument(
              MethodParameter parameter,
              ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest,
              WebDataBinderFactory binderFactory) {
            return mockSecurityUser;
          }
        });
  }

  @Test
  void document_notification_list() throws Exception {
    when(notificationFacade.getRecentNotifications(
            anyLong(), any(NotificationCategory.class), anyInt(), anyInt()))
        .thenReturn(mockNotificationPage(false));

    mockMvc
        .perform(
            get("/api/v1/notifications")
                .queryParam("type", "ANNOUNCEMENT")
                .queryParam("page", "0")
                .queryParam("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "notifications/list",
                responsePreprocessor(),
                queryParameters(
                    parameterWithName("type")
                        .optional()
                        .description(
                            "Notification category (`ANNOUNCEMENT`, `APPLICATION`, `MEETING_ROOM`, `INTERVIEW`, `SCHEDULE`)"),
                    parameterWithName("page").optional().description("Page number starting from 0"),
                    parameterWithName("size").optional().description("Page size")),
                responseFields(notificationPageResponseFields())));
  }

  @Test
  void document_unread_notification_list() throws Exception {
    when(notificationFacade.getUnreadNotifications(
            anyLong(), any(NotificationCategory.class), anyInt(), anyInt()))
        .thenReturn(mockNotificationPage(false));

    mockMvc
        .perform(
            get("/api/v1/notifications/unread")
                .queryParam("type", "ANNOUNCEMENT")
                .queryParam("page", "0")
                .queryParam("size", "20")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "notifications/unread",
                responsePreprocessor(),
                queryParameters(
                    parameterWithName("type")
                        .optional()
                        .description(
                            "Notification category (`ANNOUNCEMENT`, `APPLICATION`, `MEETING_ROOM`, `INTERVIEW`, `SCHEDULE`)"),
                    parameterWithName("page").optional().description("Page number starting from 0"),
                    parameterWithName("size").optional().description("Page size")),
                responseFields(notificationPageResponseFields())));
  }

  @Test
  void document_unread_notification_count() throws Exception {
    when(notificationFacade.countUnreadNotifications(anyLong())).thenReturn(3L);

    mockMvc
        .perform(
            get("/api/v1/notifications/unread-count")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "notifications/unread-count",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("Result status"),
                    fieldWithPath("data.unreadCount").description("Unread notification count"),
                    fieldWithPath("error").description("Error information").optional())));
  }

  @Test
  void document_read_notification() throws Exception {
    when(notificationFacade.readNotification(anyLong(), anyLong())).thenReturn(mockReadNotification());

    mockMvc
        .perform(
            patch("/api/v1/notifications/{notificationId}/read", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "notifications/read",
                responsePreprocessor(),
                pathParameters(parameterWithName("notificationId").description("Notification ID to mark as read")),
                responseFields(notificationSingleResponseFields())));
  }

  @Test
  void document_read_all_notifications() throws Exception {
    doNothing().when(notificationFacade).readAllNotifications(anyLong());

    mockMvc
        .perform(
            patch("/api/v1/notifications/read-all")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "notifications/read-all",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("Result status"),
                    fieldWithPath("data").description("Response payload").optional(),
                    fieldWithPath("error").description("Error information").optional())));
  }

  @Test
  void document_delete_notification() throws Exception {
    doNothing().when(notificationFacade).deleteNotification(anyLong(), anyLong());

    mockMvc
        .perform(
            delete("/api/v1/notifications/{notificationId}", 101L)
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "notifications/delete",
                responsePreprocessor(),
                pathParameters(parameterWithName("notificationId").description("Notification ID to delete")),
                responseFields(
                    fieldWithPath("result").description("Result status"),
                    fieldWithPath("data").description("Response payload").optional(),
                    fieldWithPath("error").description("Error information").optional())));
  }

  @Test
  void document_delete_all_notifications() throws Exception {
    doNothing().when(notificationFacade).deleteAllNotifications(anyLong());

    mockMvc
        .perform(
            delete("/api/v1/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(
            document(
                "notifications/delete-all",
                responsePreprocessor(),
                responseFields(
                    fieldWithPath("result").description("Result status"),
                    fieldWithPath("data").description("Response payload").optional(),
                    fieldWithPath("error").description("Error information").optional())));
  }

  private NotificationPage mockNotificationPage(boolean hasNext) {
    return new NotificationPage(List.of(mockUnreadNotification()), hasNext);
  }

  private Notification mockUnreadNotification() {
    return new Notification(
        101L,
        1L,
        NotificationType.ANNOUNCEMENT_CREATED,
        "A new announcement was posted.",
        "Please check the March hiring operations notice.",
        NotificationTargetType.ANNOUNCEMENT,
        201L,
        "/announcements/201",
        false,
        null,
        LocalDateTime.of(2026, 3, 18, 9, 0));
  }

  private Notification mockReadNotification() {
    return new Notification(
        101L,
        1L,
        NotificationType.ANNOUNCEMENT_CREATED,
        "A new announcement was posted.",
        "Please check the March hiring operations notice.",
        NotificationTargetType.ANNOUNCEMENT,
        201L,
        "/announcements/201",
        true,
        LocalDateTime.of(2026, 3, 18, 9, 5),
        LocalDateTime.of(2026, 3, 18, 9, 0));
  }

  private FieldDescriptor[] notificationPageResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("Result status"),
      fieldWithPath("data.contents[].id").description("Notification ID"),
      fieldWithPath("data.contents[].type").description("Notification type"),
      fieldWithPath("data.contents[].title").description("Notification title"),
      fieldWithPath("data.contents[].content").description("Notification content"),
      fieldWithPath("data.contents[].targetType").description("Notification target type"),
      fieldWithPath("data.contents[].targetId").description("Notification target ID").optional(),
      fieldWithPath("data.contents[].actionUrl").description("Navigation URL for the notification").optional(),
      fieldWithPath("data.contents[].isRead").description("Whether the notification has been read"),
      fieldWithPath("data.contents[].readAt").description("Timestamp when the notification was read").optional(),
      fieldWithPath("data.contents[].createdAt").description("Notification creation timestamp"),
      fieldWithPath("data.hasNext").description("Whether there is another page"),
      fieldWithPath("error").description("Error information").optional()
    };
  }

  private FieldDescriptor[] notificationSingleResponseFields() {
    return new FieldDescriptor[] {
      fieldWithPath("result").description("Result status"),
      fieldWithPath("data.id").description("Notification ID"),
      fieldWithPath("data.type").description("Notification type"),
      fieldWithPath("data.title").description("Notification title"),
      fieldWithPath("data.content").description("Notification content"),
      fieldWithPath("data.targetType").description("Notification target type"),
      fieldWithPath("data.targetId").description("Notification target ID").optional(),
      fieldWithPath("data.actionUrl").description("Navigation URL for the notification").optional(),
      fieldWithPath("data.isRead").description("Whether the notification has been read"),
      fieldWithPath("data.readAt").description("Timestamp when the notification was read").optional(),
      fieldWithPath("data.createdAt").description("Notification creation timestamp"),
      fieldWithPath("error").description("Error information").optional()
    };
  }
}
