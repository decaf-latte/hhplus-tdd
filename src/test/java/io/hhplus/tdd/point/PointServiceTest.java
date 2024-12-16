package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
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
    @DisplayName("getUserPointByUserId 성공 케이스")
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
    @DisplayName("getPointHistoryByUserId 성공 케이스")
    void getPointHistoryByUserId() {

        // 테스트 데이터 초기화
        List<PointHistory> pointHistoryList = Arrays.asList(
                new PointHistory(1L, 1L, 100, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, 1L, 50, TransactionType.USE, System.currentTimeMillis())
        );

        //성공 동작 정의
        when(pointService.getPointHistoryByUserId(1L)).thenReturn(pointHistoryList);

        //테스트 실행
        List<PointHistory> resultList = pointService.getPointHistoryByUserId(1L);

        //검증
        assertNotNull(resultList);
        assertEquals(2, resultList.size());
        assertEquals(TransactionType.CHARGE, resultList.get(0).type());
        assertEquals(50, resultList.get(1).amount());

        // Mock 메서드 호출 검증
        verify(pointHistoryTable, times(1)).selectAllByUserId(1L);
    }

    @Test
    @DisplayName("chargeUserPoint 성공 케이스")
    void chargeUserPoint() {

        // 테스트 데이터 초기화
        UserPoint userPoint = new UserPoint(1L, 100, System.currentTimeMillis());
        UserPoint updatedUserPoint = new UserPoint(1L, 200, System.currentTimeMillis());

        // Mock 동작 정의
        when(userPointTable.selectById(1L)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(1L, 200)).thenReturn(updatedUserPoint);

        // 테스트 실행
        UserPoint result = pointService.chargeUserPoint(1L, 100);

        // 검증
        assertNotNull(result);
        assertEquals(200, result.point());
        verify(userPointTable, times(1)).selectById(1L);
        verify(userPointTable, times(1)).insertOrUpdate(1L, 200);
    }

    @Test
    @DisplayName("usePoint 성공 케이스")
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
}