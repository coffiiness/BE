package com.team.voteland.docs.votes;

import com.team.voteland.core.api.controller.v1.VoteController;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.docs.RestDocsTest;
import com.team.voteland.domain.vote.api.v1.request.CreateVoteRequest;
import com.team.voteland.domain.vote.api.v1.request.VoteSubmitRequest;
import com.team.voteland.domain.vote.api.v1.response.VoteSubmitResponse;
import com.team.voteland.domain.vote.domain.VoteService;
import com.team.voteland.support.security.jwt.SecurityUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static com.team.voteland.docs.RestDocsUtils.responsePreprocessor;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class VoteApiDocs extends RestDocsTest {

    private final VoteService voteService = mock(VoteService.class);

    private final VoteController voteController = new VoteController(voteService);

    private final SecurityUser mockSecurityUser = new SecurityUser(1L, "test@example.com", "USER");

    @BeforeEach
    @Override
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        super.setUp(restDocumentation);
        setUpMockMvcWithSecurityUser(voteController, restDocumentation);
    }

    private void setUpMockMvcWithSecurityUser(Object controller, RestDocumentationContextProvider restDocumentation) {
        setUpMockMvc(controller, restDocumentation, new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(SecurityUser.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return mockSecurityUser;
            }
        });
    }

    @Test
    void 투표생성_API_문서화() throws Exception {
        // given
        doNothing().when(voteService)
            .createVote(anyLong(), anyString(), anyString(), any(VoteType.class), anyList(), any(LocalDateTime.class));

        List<String> options = List.of("option1", "option2", "option3");
        CreateVoteRequest request = new CreateVoteRequest("title", "description", VoteType.SINGLE, options,
                LocalDateTime.now().plusHours(1));

        // when & then
        mockMvc
            .perform(post("/api/v1/votes").contentType(MediaType.APPLICATION_JSON)
                .characterEncoding("UTF-8")
                .accept(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isOk())
            .andDo(document("votes", responsePreprocessor(), requestFields(fieldWithPath("title").description("제목"),
                    fieldWithPath("description").description("설명"),
                    fieldWithPath("voteType").description("투표 타입(단일, 중복)"),
                    fieldWithPath("options").description("투표 항목"), fieldWithPath("deadline").description("투표 마감 기간")),
                    responseFields(fieldWithPath("result").description("결과 타입 (SUCCESS/ERROR)"),
                            fieldWithPath("data").description("응답 데이터").optional(),
                            fieldWithPath("error").description("에러 정보").optional())));
    }

    @Test
    void 투표참여_API_문서화() throws Exception {
        // given
        VoteSubmitResponse response = new VoteSubmitResponse("투표가 완료되었습니다.", List.of(1L, 2L));

        when(voteService.submitVote(anyLong(), anyLong(), any(VoteSubmitRequest.class))).thenReturn(response);
        VoteSubmitRequest request = new VoteSubmitRequest(List.of(1L, 2L));

        // when & then
        mockMvc
            .perform(post("/api/v1/votes/{voteId}/submit", 1L).contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)))
            .andExpect(status().isOk())
            .andDo(document("vote-submit", responsePreprocessor(),

                    pathParameters(parameterWithName("voteId").description("참여할 투표의 ID")),

                    requestFields(fieldWithPath("itemIds").description("선택한 투표 항목")),

                    responseFields(fieldWithPath("result").description("결과 타입"),
                            fieldWithPath("data.message").description("투표 완료 메시지"),
                            fieldWithPath("data.votedItems").description("투표한 항목 ID 리스트"),
                            fieldWithPath("error").description("에러 정보").optional())));
    }

}
