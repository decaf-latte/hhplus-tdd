package io.hhplus.tdd.point.controller;

import io.hhplus.tdd.exception.PointException;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.UserPoint;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class PointControllerConcurrencyTest {

    @Autowired
    private WebApplicationContext context; // Spring 애플리케이션 컨텍스트를 가져와 테스트 환경 구성

    private MockMvc mockMvc; // HTTP 요청-응답 테스트를 위한 MockMvc 객체

    @MockBean
    private PointService pointService; // 실제 PointService를 MockBean으로 대체하여 테스트

    @Test
    @DisplayName("동시에 여러 요청 처리 테스트")
    void testConcurrentRequests() throws Exception {
        // Mock 데이터 설정: 고객 1과 고객 2의 초기 데이터 설정
        long userId1 = 1L;
        long userId2 = 2L;

        // PointService 메서드 호출 결과를 Mock으로 설정
        when(pointService.chargeUserPoint(eq(userId1), anyLong())).thenReturn(new UserPoint(userId1, 800L, System.currentTimeMillis()));
        when(pointService.usePoint(eq(userId1), anyLong())).thenReturn(new UserPoint(userId1, 700L, System.currentTimeMillis()));
        when(pointService.chargeUserPoint(eq(userId2), anyLong())).thenReturn(new UserPoint(userId2, 1200L, System.currentTimeMillis()));
        when(pointService.usePoint(eq(userId2), anyLong())).thenReturn(new UserPoint(userId2, 800L, System.currentTimeMillis()));

        // MockMvc 초기화: 컨트롤러를 실제처럼 테스트하기 위해 초기화
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // CountDownLatch를 사용하여 모든 작업이 동시에 시작되도록 제어
        CountDownLatch latch = new CountDownLatch(4);

        // CompletableFuture로 병렬 작업 실행
        CompletableFuture<Void> user1Charge = CompletableFuture.runAsync(() -> {
            try {
                latch.countDown(); // 작업 준비 완료 신호
                latch.await(); // 다른 작업이 준비될 때까지 대기
                mockMvc.perform(patch("/point/{id}/charge", userId1) // 고객 1 충전 요청
                                .contentType("application/json")
                                .content("300"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.point", is(800))); // 충전 후 포인트 검증
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> user1Use = CompletableFuture.runAsync(() -> {
            try {
                latch.countDown(); // 작업 준비 완료 신호
                latch.await(); // 다른 작업이 준비될 때까지 대기
                mockMvc.perform(patch("/point/{id}/use", userId1) // 고객 1 사용 요청
                                .contentType("application/json")
                                .content("100"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.point", is(700))); // 사용 후 포인트 검증
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> user2Charge = CompletableFuture.runAsync(() -> {
            try {
                latch.countDown(); // 작업 준비 완료 신호
                latch.await(); // 다른 작업이 준비될 때까지 대기
                mockMvc.perform(patch("/point/{id}/charge", userId2) // 고객 2 충전 요청
                                .contentType("application/json")
                                .content("200"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.point", is(1200))); // 충전 후 포인트 검증
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        CompletableFuture<Void> user2Use = CompletableFuture.runAsync(() -> {
            try {
                latch.countDown(); // 작업 준비 완료 신호
                latch.await(); // 다른 작업이 준비될 때까지 대기
                mockMvc.perform(patch("/point/{id}/use", userId2) // 고객 2 사용 요청
                                .contentType("application/json")
                                .content("400"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.point", is(800))); // 사용 후 포인트 검증
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // 모든 CompletableFuture 완료 대기
        CompletableFuture.allOf(user1Charge, user1Use, user2Charge, user2Use).join();

        // Mock 검증: PointService가 올바르게 호출되었는지 확인
        verify(pointService, times(1)).chargeUserPoint(userId1, 300L);
        verify(pointService, times(1)).usePoint(userId1, 100L);
        verify(pointService, times(1)).chargeUserPoint(userId2, 200L);
        verify(pointService, times(1)).usePoint(userId2, 400L);
    }


    @Test
    @DisplayName("포인트 충전 시 예외 처리 테스트")
    void testChargePointExceptionHandling() throws Exception {
        // Mock 데이터 및 동작 정의
        long userId = 1L;
        long invalidAmount = -500L; // 잘못된 충전 금액
        doThrow(new PointException(HttpStatus.BAD_REQUEST, "충전 금액은 0보다 커야 합니다."))
                .when(pointService).chargeUserPoint(userId, invalidAmount);

        // MockMvc 초기화
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // HTTP 요청 및 응답 검증
        mockMvc.perform(patch("/point/{id}/charge", userId)
                        .contentType("application/json")
                        .content(String.valueOf(invalidAmount)))
                .andExpect(status().isBadRequest()) // HTTP 상태 코드 400 검증
                .andExpect(jsonPath("$.message").value("충전 금액은 0보다 커야 합니다.")); // 에러 메시지 검증
    }

    @Test
    @DisplayName("포인트 사용 시 잔액 부족 예외 처리 테스트")
    void testUsePointExceptionHandling() throws Exception {
        // Mock 데이터 및 동작 정의
        long userId = 1L;
        long excessiveAmount = 2000L; // 초과 금액 요청
        doThrow(new PointException(HttpStatus.BAD_REQUEST, "포인트가 부족합니다. 현재 잔액: 500원, 요청 금액: 2000원"))
                .when(pointService).usePoint(userId, excessiveAmount);

        // MockMvc 초기화
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // HTTP 요청 및 응답 검증
        mockMvc.perform(patch("/point/{id}/use", userId)
                        .contentType("application/json")
                        .content(String.valueOf(excessiveAmount)))
                .andExpect(status().isBadRequest()) // HTTP 상태 코드 400 검증
                .andExpect(jsonPath("$.message").value("포인트가 부족합니다. 현재 잔액: 500원, 요청 금액: 2000원")); // 에러 메시지 검증
    }
}
