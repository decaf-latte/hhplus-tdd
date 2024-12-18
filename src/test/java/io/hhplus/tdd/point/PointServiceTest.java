package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.PointException;
import io.hhplus.tdd.point.service.PointService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @InjectMocks
    private PointService pointService; //테스트 대상
    @Mock
    private UserPointTable userPointTable;// Mock 객체
    @Mock
    private PointHistoryTable pointHistoryTable; //Mock 객체

    @Test
    @DisplayName("특정 유저의 포인트 조회 성공 케이스")
    void getUserPointByUserId() {
        // 테스트 데이터 초기화
        UserPoint userPoint = new UserPoint(1L, 100, System.currentTimeMillis());

        // Mock 동작 정의
        when(pointService.getUserPointByUserId(1L)).thenReturn(userPoint);

        // 테스트 실행
        UserPoint result = pointService.getUserPointByUserId(1L);

        // 검증
        assertNotNull(result);
        assertEquals(100, result.point());
        verify(userPointTable, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("특정 유저의 포인트 충전/이용 내역 조회 성공 케이스")
    void getPointHistoryByUserId() {

        // 테스트 데이터 초기화
        List<PointHistory> pointHistoryList = Arrays.asList(
                new PointHistory(1L, 1L, 100, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, 1L, 50, TransactionType.USE, System.currentTimeMillis())
        );

        // Mock 동작 정의
        when(pointHistoryTable.selectAllByUserId(1L)).thenReturn(pointHistoryList);

        // 테스트 실행
        List<PointHistory> resultList = pointService.getPointHistoryByUserId(1L);

        // 검증
        assertNotNull(resultList);
        assertEquals(2, resultList.size());
        assertEquals(TransactionType.CHARGE, resultList.get(0).type());
        assertEquals(50, resultList.get(1).amount());

        // Mock 메서드 호출 검증
        verify(pointHistoryTable, times(1)).selectAllByUserId(1L);
    }

    @Test
    @DisplayName("유효하지 않은 ID로 포인트 내역 조회")
    void getPointHistoryByInvalidId() {
        long invalidId = -1L;
        // Mock 동작 정의: 빈 리스트 반환
        when(pointHistoryTable.selectAllByUserId(invalidId)).thenReturn(Collections.emptyList());

        // 예외 발생 검증
        PointException exception = assertThrows(PointException.class, () -> {
            pointService.getPointHistoryByUserId(invalidId);
        });

        // 예외 메시지 검증
        assertEquals("포인트 내역이 존재하지 않습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 내역이 없는 사용자 조회")
    void getPointHistoryWithNoHistory() {
        long validId = 1L;

        // Mock 동작 정의: 빈 리스트 반환
        when(pointHistoryTable.selectAllByUserId(validId)).thenReturn(Collections.emptyList());

        // 예외 발생 검증
        PointException exception = assertThrows(PointException.class, () -> {
            pointService.getPointHistoryByUserId(validId);
        });

        // 예외 메시지 검증
        assertEquals("포인트 내역이 존재하지 않습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("포인트 충전 성공 케이스")
    void chargeUserPoint() {
        UserPoint userPoint = new UserPoint(1L, 100, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(1L, 200, System.currentTimeMillis());

        when(userPointTable.selectById(1L)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(1L, 200)).thenReturn(updatedUserPoint);

        UserPoint result = pointService.chargeUserPoint(1L, 100);

        assertNotNull(result);
        assertEquals(200, result.point());
        verify(userPointTable, times(1)).selectById(1L);
        verify(userPointTable, times(1)).insertOrUpdate(1L, 200);
    }

    @Test
    @DisplayName("최대 잔고 초과 포인트 충전")
    void chargeUserPointExceedMaxBalance() {
        long maxBalance = 1_000_000L;
        UserPoint userPoint = new UserPoint(1L, maxBalance - 10, System.currentTimeMillis());

        when(userPointTable.selectById(1L)).thenReturn(userPoint);

        PointException exception = assertThrows(PointException.class, () -> {
            pointService.chargeUserPoint(1L, 20);
        });

        assertEquals("최대 잔고는 " + maxBalance + "을 초과할 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("잘못된 금액 (0 이하) 충전")
    void chargeUserPointWithZeroOrNegativeAmount() {
        long invalidAmount = 0L;

        PointException exception = assertThrows(PointException.class, () -> {
            pointService.chargeUserPoint(1L, invalidAmount);
        });

        assertEquals("충전 금액은 0보다 커야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("잘못된 금액 (- 금액) 충전")
    void chargeUserPointWithNegativeAmount() {
        long invalidAmount = -100L;

        PointException exception = assertThrows(PointException.class, () -> {
            pointService.chargeUserPoint(1L, invalidAmount);
        });

        assertEquals("충전 금액은 0보다 커야 합니다.", exception.getMessage());
    }


    @Test
    @DisplayName("포인트 사용 성공 케이스")
    void usePoint() {
        // 테스트 데이터 초기화
        UserPoint userPoint = new UserPoint(1L, 100, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(1L, 50, System.currentTimeMillis());

        // Mock 동작 정의
        when(userPointTable.selectById(1L)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(1L, 50)).thenReturn(updatedUserPoint);

        // 테스트 실행
        UserPoint result = pointService.usePoint(1L, 50);

        // 검증
        assertNotNull(result);
        assertEquals(50, result.point());
        verify(userPointTable, times(1)).selectById(1L);
        verify(userPointTable, times(1)).insertOrUpdate(1L, 50);
    }

    @Test
    @DisplayName("잔액 부족으로 포인트 사용 실패")
    void usePointInsufficientBalance() {
        // 사용자 포인트가 50인데 100을 사용하려 함
        UserPoint userPoint = new UserPoint(1L, 50, System.currentTimeMillis());

        // Mock 동작 정의
        when(userPointTable.selectById(1L)).thenReturn(userPoint);

        // 예외 검증
        PointException exception = assertThrows(PointException.class, () -> {
            pointService.usePoint(1L, 100);
        });

        assertEquals("포인트가 부족합니다. 현재 잔액: " + userPoint.point() + "원, 요청 금액: " + 100 + "원", exception.getMessage());
    }

    @Test
    @DisplayName("잘못된 금액 (0 이하) 포인트 사용")
    void usePointWithZeroOrNegativeAmount() {
        long invalidAmount = -50L;

        // 예외 검증
        PointException exception = assertThrows(PointException.class, () -> {
            pointService.usePoint(1L, invalidAmount);
        });

        assertEquals("사용 금액은 0보다 커야 합니다.", exception.getMessage());
    }

    @Test
    @DisplayName("고객별 동시성 처리 테스트")
    void testCustomerConcurrency() throws InterruptedException {
        // Mock 객체 생성
        UserPointTable userPointTable = mock(UserPointTable.class);
        PointHistoryTable pointHistoryTable = mock(PointHistoryTable.class);

        // Mock 상태를 유지하기 위한 데이터
        // ConcurrentHashMap은 스레드 안전성을 보장하며, 테스트 중 상태를 동기화
        ConcurrentHashMap<Long, UserPoint> userPoints = new ConcurrentHashMap<>();
        userPoints.put(1L, new UserPoint(1L, 500, System.currentTimeMillis()));
        userPoints.put(2L, new UserPoint(2L, 1000, System.currentTimeMillis()));

        // Mock 동작 정의
        when(userPointTable.selectById(anyLong())).thenAnswer(invocation -> {
            long userId = invocation.getArgument(0);
            return userPoints.get(userId);
        });

        doAnswer(invocation -> {
            long userId = invocation.getArgument(0);
            long updatedAmount = invocation.getArgument(1);
            userPoints.put(userId, new UserPoint(userId, updatedAmount, System.currentTimeMillis()));
            return null;
        }).when(userPointTable).insertOrUpdate(anyLong(), anyLong());

        // PointService 인스턴스 생성: Mock 객체 주입
        PointService pointService = new PointService(pointHistoryTable, userPointTable);

        // 스레드 풀 생성: 병렬 작업을 처리하기 위해 4개의 스레드 사용
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        // 병렬 작업 실행: 고객 1과 고객 2의 충전 및 사용 요청
        executorService.submit(() -> pointService.chargeUserPoint(1L, 300)); // 500 + 300 = 800
        executorService.submit(() -> pointService.usePoint(1L, 100));       // 800 - 100 = 700
        executorService.submit(() -> pointService.chargeUserPoint(2L, 200)); // 1000 + 200 = 1200
        executorService.submit(() -> pointService.usePoint(2L, 400));       // 1200 - 400 = 800

        // 스레드 종료 및 대기
        // 모든 작업이 5초 내에 완료되었는지 검증
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));

        // 결과 검증
        // 고객 1의 최종 잔액 = 700, 고객 2의 최종 잔액 = 800
        assertEquals(700, userPoints.get(1L).point()); // 고객 1 최종 잔액
        assertEquals(800, userPoints.get(2L).point()); // 고객 2 최종 잔액
    }

    @Test
    @DisplayName("동시에 동일 고객 요청 처리 시 충돌 방지 테스트")
    void testSameCustomerConcurrency() throws InterruptedException {
        // Mock 객체 생성
        UserPointTable userPointTable = mock(UserPointTable.class);
        PointHistoryTable pointHistoryTable = mock(PointHistoryTable.class);

        // Mock 상태 관리: ConcurrentHashMap을 이용하여 스레드 안전하게 고객 상태 관리
        ConcurrentHashMap<Long, UserPoint> userPoints = new ConcurrentHashMap<>();
        userPoints.put(1L, new UserPoint(1L, 1000, System.currentTimeMillis()));

        // Mock 동작 정의
        when(userPointTable.selectById(anyLong())).thenAnswer(invocation -> {
            long userId = invocation.getArgument(0);
            return userPoints.get(userId);
        });

        doAnswer(invocation -> {
            long userId = invocation.getArgument(0);
            long updatedAmount = invocation.getArgument(1);
            userPoints.put(userId, new UserPoint(userId, updatedAmount, System.currentTimeMillis()));
            return null;
        }).when(userPointTable).insertOrUpdate(anyLong(), anyLong());

        PointService pointService = new PointService(pointHistoryTable, userPointTable);

        // 스레드 풀 생성: 병렬 작업을 처리하기 위해 2개의 스레드 사용
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // 병렬 작업 실행: 동일 고객에 대한 충전 및 사용 요청
        executorService.submit(() -> pointService.chargeUserPoint(1L, 500)); // 1000 + 500
        executorService.submit(() -> pointService.usePoint(1L, 300));       // 1500 - 300

        // 스레드 종료 및 대기
        executorService.shutdown();
        assertTrue(executorService.awaitTermination(5, TimeUnit.SECONDS));

        // 최종 상태 검증: 고객 1의 잔액이 동시성 제어로 인해 올바르게 계산되었는지 확인
        assertEquals(1200, userPoints.get(1L).point()); // 고객 1 최종 잔액 = 1200
    }
}