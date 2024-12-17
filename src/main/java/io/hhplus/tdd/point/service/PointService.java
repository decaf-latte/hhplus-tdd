package io.hhplus.tdd.point.service;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.PointException;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import io.hhplus.tdd.point.UserPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class PointService {

    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;

    //특정 유저의 포인트를 조회하는 기능
    public UserPoint getUserPointByUserId(long userId) {
        return userPointTable.selectById(userId);
    }

    //특정 유저의 포인트 충전/이용 내역을 조회하는 기능
    public List<PointHistory> getPointHistoryByUserId(long id) {
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(id);
        if (histories.isEmpty()) {
            throw new PointException(HttpStatus.NOT_FOUND, "포인트 내역이 존재하지 않습니다.");
        }
        return histories;
    }

    //특정 유저의 포인트를 충전하는 기능
    public UserPoint chargeUserPoint(long id, long amount) {

        if (amount <= 0) {
            throw new PointException(HttpStatus.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
        }

        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint == null) {
            throw new PointException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }

        long updatedPoint = userPoint.point() + amount;

        // 최대 잔고 초과 예외 처리
        if (updatedPoint < 0) { // Long overflow 방지
            throw new PointException(HttpStatus.BAD_REQUEST, "최대 잔고를 초과할 수 없습니다.");
        }

        // 포인트 히스토리 추가
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return userPointTable.insertOrUpdate(id, updatedPoint);
    }

    //특정 유저의 포인트를 사용하는 기능을 작성
    public UserPoint usePoint(long id, long amount) {
        if (amount <= 0) {
            throw new PointException(HttpStatus.BAD_REQUEST, "사용 금액은 0보다 커야 합니다.");
        }

        UserPoint userPoint = userPointTable.selectById(id);
        if (userPoint == null) {
            throw new PointException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다.");
        }

        if (userPoint.point() < amount) {
            throw new PointException(HttpStatus.BAD_REQUEST, "포인트가 부족합니다.");
        }

        // 포인트 히스토리 추가
        pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

        long updatedPoint = userPoint.point() - amount;
        return userPointTable.insertOrUpdate(id, updatedPoint);
    }
}
