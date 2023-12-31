package pickup_shuttle.pickup.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pickup_shuttle.pickup.domain.user.dto.request.ApproveUserRq;
import pickup_shuttle.pickup.domain.user.dto.request.RejectUserRq;
import pickup_shuttle.pickup.security.service.JwtService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@Sql("classpath:db/teardown.sql")
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class UserControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private JwtService jwtService;

    @Nested
    class testUserAuthStatus {
        @Test
        @DisplayName("성공 : 학생증 인증을 신청한 일반회원의 경우")
        void testUserAuthStatus1() throws Exception {
            //given
            String accessToken = "Bearer " + jwtService.createAccessToken("2"); //USER

            //when
            ResultActions resultActions = mvc.perform(
                    get("/mypage/auth")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", accessToken)
            );

            //eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            System.out.println("testUserAuthStatus1 : " + responseBody);

            //then
            resultActions.andExpect(jsonPath("$.success").value("true"));
            resultActions.andExpect(jsonPath("$.response").value("인증 진행 중"));
        }

        @Test
        @DisplayName("성공 : 학생증 인증을 신청하지 않은 일반회원의 경우")
        void testUserAuthStatus2() throws Exception {
            //given
            String accessToken = "Bearer " + jwtService.createAccessToken("7");

            //when
            ResultActions resultActions = mvc.perform(
                    get("/mypage/auth")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", accessToken)
            );

            //eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            System.out.println("testUserAuthStatus2 : " + responseBody);

            //then
            resultActions.andExpect(jsonPath("$.success").value("true"));
            resultActions.andExpect(jsonPath("$.response").value("미인증"));
        }

        @Test
        @DisplayName("성공 : 학생회원의 경우")
        void testUserAuthStatus3() throws Exception {
            //given
            String accessToken = "Bearer " + jwtService.createAccessToken("3"); //STUDENT

            //when
            ResultActions resultActions = mvc.perform(
                    get("/mypage/auth")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", accessToken)
            );

            //eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            System.out.println("testUserAuthStatus3 : " + responseBody);

            //then
            resultActions.andExpect(jsonPath("$.success").value("true"));
            resultActions.andExpect(jsonPath("$.response").value("인증"));
        }
    }

    @Test
    @DisplayName("성공 : 마이페이지 조회")
    void tesMyPage() throws Exception {
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("2"); //USER

        //when
        ResultActions resultActions = mvc.perform(
                get("/mypage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testMyPage : " + responseBody);

        //then
        resultActions.andExpect(jsonPath("$.success").value("true"));
        resultActions.andExpect(jsonPath("$.response.role").value("ROLE_USER"));
        resultActions.andExpect(jsonPath("$.response.nickname").value("배경"));
    }

    @Test
    @DisplayName("성공 : 학생 인증 목록 보기")
    void testGetAuthList() throws Exception {
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("1"); //ADMIN

        //when
        ResultActions resultActions = mvc.perform(
                get("/admin/auth/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testGetAuthList : " + responseBody);

        //then
        resultActions.andExpect(jsonPath("$.success").value("true"));
        resultActions.andExpect(jsonPath("$.response.content[0].userId").value(2));
        resultActions.andExpect(jsonPath("$.response.content[0].nickname").value("배경"));

    }

    @Test
    @DisplayName("성공 : 학생 인증 상세보기")
    void testGetAuthDetail() throws Exception {
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("1"); //ADMIN
        Long userId = 2L; // 학생 인증을 신청한 일반회원
        //when
        ResultActions resultActions = mvc.perform(
                get("/admin/auth/list/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testGetAuthDetail : " + responseBody);
        //then
        resultActions.andExpect(jsonPath("$.success").value("true"));
        resultActions.andExpect(jsonPath("$.response.nickname").value("배경"));
    }

    @Nested
    class testAuthApprove {
        @Test
        @DisplayName("성공 : 학생 인증 승인")
        void testAuthApprove() throws Exception {
            //given
            String accessToken = "Bearer " + jwtService.createAccessToken("1"); //ADMIN
            Long userId = 2L; // 학생 인증을 신청한 일반회원
            ApproveUserRq requestDTO = ApproveUserRq.builder()
                    .userId(userId)
                    .build();
            String requestBody = om.writeValueAsString(requestDTO);
            //when
            ResultActions resultActions = mvc.perform(
                    patch("/admin/auth/approval")
                            .content(requestBody)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", accessToken)
            );

            //eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            System.out.println("testAuthApprove : " + responseBody);

            //then
            resultActions.andExpect(jsonPath("$.success").value("true"));
            resultActions.andExpect(jsonPath("$.response").value("학생 인증이 승인되었습니다"));
        }

        @Test
        @DisplayName("실패 : 일반 등급이 아닌 회원의 학생 인증을 시도하는 경우")
        void testAuthApproveInvalidRole() throws Exception {
            //given
            String accessToken = "Bearer " + jwtService.createAccessToken("1"); //ADMIN
            Long userId = 7L; // GUEST
            ApproveUserRq requestDTO = ApproveUserRq.builder()
                    .userId(userId)
                    .build();
            String requestBody = om.writeValueAsString(requestDTO);
            //when
            ResultActions resultActions = mvc.perform(
                    patch("/admin/auth/approval")
                            .content(requestBody)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", accessToken)
            );

            //eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            System.out.println("testAuthApproveInvalidRole : " + responseBody);

            //then
            resultActions.andExpect(jsonPath("$.success").value("false"));
            resultActions.andExpect(jsonPath("$.error.message").value("일반 회원이 아닙니다"));
        }
    }

    @Test
    @DisplayName("성공 : 학생 인증 거절")
    void testAuthReject() throws Exception{
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("1"); //ADMIN
        Long userId = 2L; // 학생 인증을 신청한 일반회원
        RejectUserRq requestDTO = RejectUserRq.builder()
                .userId(userId)
                .build();
        String requestBody = om.writeValueAsString(requestDTO);
        //when
        ResultActions resultActions = mvc.perform(
                put("/admin/auth/reject")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testAuthApprove : " + responseBody);

        //then
        resultActions.andExpect(jsonPath("$.success").value("true"));
        resultActions.andExpect(jsonPath("$.response.message").value("학생 인증이 거절되었습니다"));
    }

    @Test
    @DisplayName("실패 : 유저 권한이 일반사용자가 아닐 경우")
    void testFailAuthReject() throws Exception {
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("1"); //ADMIN
        Long userId = 3L; // 학생 인증을 신청한 일반회원
        RejectUserRq requestDTO = RejectUserRq.builder()
                .userId(userId)
                .build();
        String requestBody = om.writeValueAsString(requestDTO);
        //when
        ResultActions resultActions = mvc.perform(
                put("/admin/auth/reject")
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testAuthApprove : " + responseBody);

        //then
        resultActions.andExpect(jsonPath("$.success").value("false"));
        resultActions.andExpect(jsonPath("$.error.message").value("일반 회원이 아닙니다"));
    }
    @Nested
    class testGetRequesterDetail{
        @Test
        @DisplayName("성공 : (작성자) 공고글 매칭 전 상세조회")
        void testGetRequesterDetailBeforeMatched() throws Exception {
            //given
            String accessToken = "Bearer " + jwtService.createAccessToken("6");
            Long boardId = 6L;

            //when
            ResultActions resultActions = mvc.perform(
                    get("/mypage/requester/detail/{boardId}", boardId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", accessToken)
            );

            //eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            System.out.println("testGetRequesterDetailBeforeMatched : " + responseBody);

            //then
            resultActions.andExpect(jsonPath("$.success").value("true"));
            resultActions.andExpect(jsonPath("$.response.boardId").value(boardId));
            resultActions.andExpect(jsonPath("$.response.isMatch").value(false));
        }
        @Test
        @DisplayName("성공 : (작성자) 공고글 매칭 후 상세조회")
        void testGetRequesterDetailAfterMatched() throws Exception {
            //given
            String accessToken = "Bearer " + jwtService.createAccessToken("3"); //ADMIN
            Long boardId = 3L;

            //when
            ResultActions resultActions = mvc.perform(
                    get("/mypage/requester/detail/{boardId}", boardId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", accessToken)
            );

            //eye
            String responseBody = resultActions.andReturn().getResponse().getContentAsString();
            System.out.println("testGetRequesterDetailAfterMatched : " + responseBody);

            //then
            resultActions.andExpect(jsonPath("$.success").value("true"));
            resultActions.andExpect(jsonPath("$.response.boardId").value(boardId));
            resultActions.andExpect(jsonPath("$.response.isMatch").value(true));
        }
    }
    @Test
    @DisplayName("성공 : (작성자) 공고글 목록 보기")
    void testGetRequesterList() throws Exception {
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("6");

        //when
        ResultActions resultActions = mvc.perform(
                get("/mypage/requester/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testGetRequesterList : " + responseBody);

        //then
        resultActions.andExpect(jsonPath("$.success").value("true"));
        resultActions.andExpect(jsonPath("$.response.content[0].boardId").value(6));
    }

    @Test
    @DisplayName("성공 : 피커 공고글 목록")
    void testPickerBoardList() throws Exception {
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("2");
        String refreshToken = jwtService.createRefreshToken();
        System.out.println("refreshToken: " + refreshToken);
        String offset = "";
        String limit = "10";
        //when
        ResultActions resultActions = mvc.perform(
                get("/mypage/picker/list")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testMyPage : " + responseBody);

        //then
        resultActions.andExpect(jsonPath("$.success").value("true"));
        resultActions.andExpect(jsonPath("$.response.pageable.last").value("true"));
        resultActions.andExpect(jsonPath("$.response.content[0].boardId").value("4"));
        resultActions.andExpect(jsonPath("$.response.content[0].destination").value("전남대 공대7 220호관"));
    }

    @Test
    @DisplayName("실패 : 수락한 공고글이 없는 경우")
    void testFailPickerBoardList() throws Exception {
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("1");
        String offset = "";
        String limit = "10";
        //when
        ResultActions resultActions = mvc.perform(
                get("/mypage/picker/list")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testMyPage : " + responseBody);

        //then
        resultActions.andExpect(jsonPath("$.success").value("false"));
        resultActions.andExpect(jsonPath("$.error.message").value("수락한 공고글이 없습니다"));
    }

    @Test
    @DisplayName("성공 : 피커 공고글 상세보기")
    void testPickerBoardDetail() throws Exception {
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("2");
        Long boardId = 4L;
        //when
        ResultActions resultActions = mvc.perform(
                get("/mypage/picker/list/{boardId}", boardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testPickerBoardDetail : " + responseBody);

        //then
        resultActions.andExpect(jsonPath("$.success").value("true"));
        resultActions.andExpect(jsonPath("$.response.boardId").value("4"));
        resultActions.andExpect(jsonPath("$.response.shopName").value("이디야"));
        resultActions.andExpect(jsonPath("$.response.destination").value("전남대 공대7 220호관"));
        resultActions.andExpect(jsonPath("$.response.beverage[0].name").value("카페모카 3잔"));
        resultActions.andExpect(jsonPath("$.response.beverage[1].name").value("초코라떼 2잔"));
        resultActions.andExpect(jsonPath("$.response.beverage[2].name").value("딸기라떼 1잔"));
        resultActions.andExpect(jsonPath("$.response.tip").value("1000"));
        resultActions.andExpect(jsonPath("$.response.request").value("빨리 와주세요4"));
    }

    @Test
    @DisplayName("실패 : 매칭이 완료되지 않은 공고글을 조회한 경우")
    void testFailPickerBoardDetail1() throws Exception {
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("2");
        Long boardId = 1L;
        //when
        ResultActions resultActions = mvc.perform(
                get("/mypage/picker/list/{boardId}", boardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testPickerBoardDetail : " + responseBody);

        //then
        resultActions.andExpect(jsonPath("$.success").value("false"));
        resultActions.andExpect(jsonPath("$.error.message").value("매칭이 완료되지 않은 공고글입니다"));
    }

    @Test
    @DisplayName("실패 : 해당 공고글의 피커가 아닐 경우")
    void testFailPickerBoardDetail2() throws Exception {
        //given
        String accessToken = "Bearer " + jwtService.createAccessToken("1");
        Long boardId = 4L;
        //when
        ResultActions resultActions = mvc.perform(
                get("/mypage/picker/list/{boardId}", boardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", accessToken)
        );

        //eye
        String responseBody = resultActions.andReturn().getResponse().getContentAsString();
        System.out.println("testPickerBoardDetail : " + responseBody);

        //then
        resultActions.andExpect(jsonPath("$.success").value("false"));
        resultActions.andExpect(jsonPath("$.error.message").value("해당 공고글의 피커가 아닙니다"));
    }
}