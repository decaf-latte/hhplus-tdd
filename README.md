# 동시성 제어 방식 분석

## **해당 프로젝트에서의 요구 사항**
- **요구사항**: 동시에 여러 요청이 들어오더라도 **순서대로** 또는 **한 번에 하나의 요청씩만** 처리될 수 있도록 구현.
- 동시에 여러 건의 포인트 충전, 이용 요청이 들어올 경우 **순차적으로 처리**

## **요구 사항 분석**
1. 처음에는 **동시에 여러 요청이 들어와도 순차적으로 처리되어야 한다는 데에만 집중**해서 모든 요청이 들어와도 무조건 순차적으로 처리해야한다고 생각
2. 하지만 포인트 충전/사용 특성상 사용자A의 조회 요청이 사용자B가 먼저 충전중이라고 해서 대기상태에 들어가 있으면 안됨
3. 즉, 유저 별 각각의 요청은 병렬로 처리되어야 하며, 동일한 유저의 요청은 순서대로 처리되어야 함

### **요구사항 최종 분석**
- **동일 유저의 요청은 순차적으로 처리**하며, **다른 유저의 요청은 병렬로 처리**해야 한다.
- 따라서, **유저별로 독립적인 동시성 제어가 필요**하다.


-------
## **동시성 제어를 이해하기 위해 알아야 할 지식**

| 구분           | 프로세스 (Process)                                | 스레드 (Thread)                               | 멀티 스레드 (Multi-thread)                       |
|----------------|---------------------------------------------------|-----------------------------------------------|--------------------------------------------------|
| 정의           | 작업 중인 프로그램. (메모리에 적재되고 CPU 자원을 할당받아 실행 중인 상태)                              | 프로세스 내에서 실행되는 작업 흐름 단위.                    | 프로세스 내 여러 스레드가 병렬로 실행되는 상태.  |
| 메모리 구조    | 독립적인 메모리 공간 사용 (코드/데이터/스택/힙).                        | 스택을 제외한 메모리(Data, Code, Heap) 공유.   | 스택은 각각 독립적이며, 나머지는 공유(자원 공유).                      |
| 생성 비용      | 생성 및 관리 비용이 높음.                                      | 상대적으로 비용이 낮음.                                   | 자원 공유로 효율적이나 동기화 비용 증가.                               |
| 통신 방법      | 별도의 통신 방법 필요(IPC).                              | 공유 메모리를 사용(Stack 제외).                  | 공유 메모리를 사용하며, 동기화 필요.                   |
| 장점           | 독립적 실행으로 안정성이 높고, 충돌 방지가 쉬움.                          | 가볍고 빠르며, 효율적.                                   | 병렬 처리로 작업 속도 향상.                              |
| 단점           | 자원 소모가 큼.                                   | 동기화 필요, 충돌 가능성 존재.                      | 동기화 및 자원 관리로 인해 복잡성 증가.                       |
| 활용 사례      | 독립 실행 프로그램(웹 브라우저, DB 서버 등).         | 이벤트 처리, 병렬 작업 실행.                   | 서버 요청 처리, 대규모 데이터 병렬 작업.                |
| 생명 주기      | **생성** → **준비** → **실행** → **대기** → **종료**. (운영체제가 관리)  | **생성** → **준비** → **실행** → **대기** → **종료**. (프로세스에 종속) | 각 스레드는 독립적인 생명 주기를 가지며, 프로세스 종료 시 함께 종료. |

---

## **그래서 동시성이 뭘까? (Concurrency)**

- 동시성은 여러 작업이 번갈아가며 실행되어 **마치 동시에 실행되는 것처럼 보이는 상태** 
- 운영체제가 **프로세스나 스레드에 CPU 자원을 빠르게 할당**하여 이런 효과를 만듦.

---

### **JVM에서의 동시성**
- **JVM**은 멀티 스레드를 지원
- 스레드들은 **Stack 영역을 제외한 Data, Code, Heap 영역을 공유**
- **Heap 영역**에서는 참조 변수들이 로드되어 여러 스레드가 공유

---

### **동시성 문제(Concurrency Problem)**
- 여러 스레드가 **동시에 Heap 영역의 데이터를 접근**하면, **예상과 다른 결과**가 발생할 수 있음
  예를 들어, 두 스레드가 같은 변수에 값을 계산하여 저장할 때 **경쟁 상태(Race Condition)**가 발생가능
  즉, 일부 메모리 공간을 공유하기 때문에 각 스레드가 리소스를 점유하려는 현상이 발생 가능
- 이러한 문제를 제어하는 것이 **동시성 제어(Concurrency Control)**

---

## **JVM 에서의 동시성 제어 방식**

### **1. synchronized**
- 가장 기본적인 동기화 방법
- 하나의 스레드에서만 접근이 가능하도록 Lock을 걸어버림. 

- 문제가 되는 메소드, 변수 등에 synchronized 키워드를 걸면 됨. 
-> 메소드가 끝날 때까지 동기화를 보장함

#### **예제**
```java
class SharedResource {
    private int count = 0;

    // 동기화된 메서드
    synchronized void increment() {
        count++;
    }

    // 동기화된 블록
    void decrement() {
        synchronized (this) {
            count--;
        }
    }

    int getCount() {
        return count;
    }
}

public class SynchronizedExample {
    public static void main(String[] args) {
        SharedResource resource = new SharedResource();

        // 동기화된 메서드 호출
        // 메서드 전체를 동기화하여, 동시에 여러 스레드가 호출하지 못하게 제한
        resource.increment();

        // 동기화된 블록 호출
        // 특정 블록만 동기화하여, 더 유연하게 동기화 범위를 설정
        resource.decrement();

        System.out.println("최종 카운트: " + resource.getCount());
    }
}
```

#### **한계**
- **확장성 부족** 메서드 또는 객체 단위로만 동기화를 지원 
-> 객체 단위로 동기화되므로, 하나의 스레드가 객체를 잠그면 다른 모든 스레드가 대기
- **교착 상태 (Deadlock)** 두 개 이상의 스레드가 서로 다른 synchronized 블록에서 자원을 기다리다가 무한 대기 상태에 빠질 수 있음
- **성능저하** 동기화는 CPU가 스레드 간의 자원 접근을 조율하기 때문에 오버헤드가 발생



---

### **2. java.util.concurrent 패키지**

**`ReentrantLock`**: 
- Lock 인터페이스를 구현한 클래스.
- synchronized보다 세부적인 잠금 제어 가능.
- 명시적으로 잠금(lock)과 해제(unlock)을 수행
**`ReentrantReadWriteLock`**: 
- 읽기와 쓰기를 구분하여, 여러 스레드가 동시에 읽기 작업을 수행 가능.

#### **예제**
```java
import java.util.concurrent.locks.ReentrantReadWriteLock;

ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

public void read() {
    rwLock.readLock().lock();
    try {
        // 읽기 작업
    } finally {
        rwLock.readLock().unlock();
    }
}

public void write() {
    rwLock.writeLock().lock();
    try {
        // 쓰기 작업
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

---

### **3. Atomic 클래스**
`java.util.concurrent.atomic` 패키지의 클래스들.
원자적 연산을 제공하여 동기화 없이도 안전한 변수 연산 가능.

#### **예제**
```java
import java.util.concurrent.atomic.AtomicInteger;

AtomicInteger counter = new AtomicInteger();

public void increment() {
    counter.incrementAndGet(); // 원자적 증가
}
```

- 주요 클래스: `AtomicInteger`, `AtomicLong`, `AtomicBoolean`, `AtomicReference`

---

### **4. Concurrent Collections**
- 다중 스레드 환경에서 안전하게 사용할 수 있는 컬렉션 구현체를 제공
- 동기화를 내부적으로 처리하여 데이터 일관성과 스레드 안전성을 보장하면서도 성능을 최적화
- **대표 클래스**: `ConcurrentHashMap`, `CopyOnWriteArrayList`, `CopyOnWriteArraySet`,`ConcurrentLinkedQueue`,`BlockingQueue`

---

### **5. CompletableFuture**
비동기 작업을 조합하거나 처리 흐름을 제어하는 데 사용.

#### **예제**
```java
import java.util.concurrent.CompletableFuture;

CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    // 비동기 작업
});
future.thenRun(() -> {
    // 작업 완료 후 실행
});
```

---

## **구현 방향**

#### **시도1. `synchronized` 키워드 사용**
- `synchronized`를 사용하여 동기화 처리하려고함. 
- 문제: **모든 요청이 직렬화**되어, 다른 유저의 요청도 대기상태가 되어버림.

#### **시도2. `ConcurrentHashMap`과 `ReentrantLock` 사용**
- **유저별로 독립적인 락**을 관리하여, 동일한 유저의 요청만 직렬화하고 다른 유저의 요청은 병렬로 처리.
- `ConcurrentHashMap`을 사용하여 유저 ID를 키로 하는 락을 관리.

---

### **최종 구현**
- `PointService` 클래스에서 `ConcurrentHashMap`을 통해 유저ID별 락을 관리.
- 포인트 충전 및 사용 요청에서 동일 유저ID의 요청은 락을 획득하여 처리.
- 다른 유저의 요청은 락을 공유하지 않으므로 병렬로 처리 가능.

---

## **동시성 제어 기능 검증**

### **검증 시나리오**
**동시에 유저A와 유저B의 여러 요청이 오는 경우**
- 비동기 방식으로 서로 다른 사용자의 충전 요청을 여러 번 요청하고 난 후 각 유저의 잔여 포인트 양과 전체 충전/사용 내역 검증
