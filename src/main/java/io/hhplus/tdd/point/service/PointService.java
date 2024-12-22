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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@RequiredArgsConstructor
@Service
public class PointService {

    private final PointHistoryTable pointHistoryTable;
    private final UserPointTable userPointTable;
    //최대 포인트 예시
    private static final long MAX_BALANCE = 1_000_000L;
    // 고객별로 락을 관리하기 위한 ConcurrentHashMap
    // 같은 고객 ID면 락을 걸기
    private final ConcurrentHashMap<Long, Lock> lockMap = new ConcurrentHashMap<>();

    // 특정 고객의 락을 가져오거나 새로 생성
    private Lock getLockForCustomer(long userId) {
        return lockMap.computeIfAbsent(userId, id -> new ReentrantLock());
    }

    //특정 유저의 포인트를 조회하는 기능
    public UserPoint getUserPointByUserId(long userId) {
        return userPointTable.selectById(userId);
    }

    //특정 유저의 포인트 충전/이용 내역을 조회하는 기능
    public List<PointHistory> getPointHistoryByUserId(long userId) {
        List<PointHistory> histories = pointHistoryTable.selectAllByUserId(userId);

        if (histories.isEmpty()) {
            throw new PointException(HttpStatus.NOT_FOUND, "포인트 내역이 존재하지 않습니다.");
        }

        return histories;
    }

    //특정 유저의 포인트를 충전하는 기능
    public UserPoint chargeUserPoint(long userId, long amount) {

        Lock lock = getLockForCustomer(userId); // 고객별 락 가져오기
        lock.lock();

        try {
            //충전 금액 0, 음수 예외처리
            if (amount <= 0) {
                throw new PointException(HttpStatus.BAD_REQUEST, "충전 금액은 0보다 커야 합니다.");
            }

            UserPoint userPoint = userPointTable.selectById(userId);
            long updatedPoint = userPoint.point() + amount;

            // 최대 잔고 초과 예외 처리
            if (updatedPoint > MAX_BALANCE) {
                throw new PointException(HttpStatus.BAD_REQUEST, "최대 잔고는 " + MAX_BALANCE + "을 초과할 수 없습니다.");
            }

            // 포인트 히스토리 추가
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return userPointTable.insertOrUpdate(userId, updatedPoint);
        } finally {
            //락 해제
            lock.unlock();
        }

    }

    //특정 유저의 포인트를 사용하는 기능을 작성
    public UserPoint usePoint(long userId, long amount) {

        Lock lock = getLockForCustomer(userId); // 고객별 락 가져오기
        lock.lock();

        try {
            //사용금액 0,음수 예외처리
            if (amount <= 0) {
                throw new PointException(HttpStatus.BAD_REQUEST, "사용 금액은 0보다 커야 합니다.");
            }

            UserPoint userPoint = userPointTable.selectById(userId);

            //포인트 잔고부족 예외처리
            if (userPoint.point() < amount) {
                throw new PointException(HttpStatus.BAD_REQUEST,
                        "포인트가 부족합니다. 현재 잔액: " + userPoint.point() + "원, 요청 금액: " + amount + "원");

            }

            // 포인트 히스토리 추가
            pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());

            long updatedPoint = userPoint.point() - amount;
            return userPointTable.insertOrUpdate(userId, updatedPoint);
        } finally {
            //락 해제
            lock.unlock();
        }

    }
}
