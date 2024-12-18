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
}