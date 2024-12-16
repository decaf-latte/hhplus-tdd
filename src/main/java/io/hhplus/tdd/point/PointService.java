package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return pointHistoryTable.selectAllByUserId(id);
    }

    //특정 유저의 포인트를 충전하는 기능
    public UserPoint chargeUserPoint(long id, long amount) {
        long userPoint = userPointTable.selectById(id).point();
        long totalPoint = userPoint + amount;
        return userPointTable.insertOrUpdate(id, totalPoint);
    }

    //특정 유저의 포인트를 사용하는 기능을 작성
    public UserPoint usePoint(long id, long amount) {
        long userPoint = userPointTable.selectById(id).point();
        long totalPoint = userPoint - amount;
        return userPointTable.insertOrUpdate(id, totalPoint);
    }
}
