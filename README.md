# Spark 데이터 처리 애플리케이션

## 개요

지정 경로의 csv raw data를 다음과 같이 처리

1.타임스탬프 변환: event_time 컬럼을 기준으로 타임스탬프를 UTC에서 KST로 변환.

2.날짜 및 파티션 생성: timestamp_kst 컬럼을 기반으로 date와 date_partition 컬럼을 생성하여 데이터를 일별 파티셔닝.

3.데이터 최적화: 불필요한 데이터 삭제.

4.Parquet 포맷 저장: 최적화된 데이터를 Parquet 형식으로 압축하여 저장.

5.Hive 테이블 업데이트: 외부 Hive 테이블을 생성하고 파티션을 최신 상태로 업데이트.


## 주요 기능
1. Apache Spark를 활용한 CSV 데이터 처리

2. 타임스탬프 변환 및 시간대 처리

3. 날짜 기반 파티셔닝

4. Parquet 파일 저장 및 Snappy 압축

5. Hive 테이블 연동 및 파티션 처리

6. 자동 재시도 및 오류 처리

7. 오류 발생 시 Slack 알림 전송

8. 작업 완료 체크포인트 생성



## 필수 요구사항
- Apache Spark
- Scala
- SBT (Scala Build Tool)
- Apache Hive
- Slack Webhook URL (알림 기능용)




## 프로젝트 구조
```
└─main
   ├─resources
   │      application.conf
   │
   └─scala
       │  AppMain.scala
       │
       ├─config
       │      ConfigLoader.scala
       │
       └─jobs
               DataHandler.scala
               SlackNotifier.scala

```


## 설정 방법
배치 어플리케이션은 TypeSafe Config를 사용하여 설정을 관리합니다. 
다음 구조로 `application.conf` 파일을 생성합니다:

```hocon
app {
  input {
    path = ""                       # 입력 데이터 경로
  }
  output {
    path = ""                       # 출력 데이터 경로
    compression = ""                # 압축 방식
  }
  hive {
    database = ""                   # Hive 데이터베이스명
    table = ""                      # Hive 테이블명
    location = ""                   # Hive 저장 위치
  }
  partition {
    date_format = "yyyy/MM/dd"      # 날짜 파티션 형식
  }
  slack {
    webhookUrl = ""                 # Slack 웹훅 URL
  }
}
```




### 출력 스키마
```sql
CREATE EXTERNAL TABLE IF NOT EXISTS database.table (
  event_time STRING,         # 이벤트 발생 시간
  event_type STRING,         # 이벤트 유형
  product_id STRING,         # 상품 ID
  category_id STRING,        # 카테고리 ID
  category_code STRING,      # 카테고리 코드
  brand STRING,             # 브랜드
  price DOUBLE,             # 가격
  user_id STRING,           # 사용자 ID
  user_session STRING       # 세션 ID
)
PARTITIONED BY (date_partition STRING)
```

## 오류 처리
- 최대 재시도 횟수: 3회
- 재시도 간격: 5초
- 최대 재시도 횟수 초과 시 Slack 알림으로 오류내역 송신


## 모니터링
- 작업 실패 시 Slack 알림
- 작업 완료 시 체크포인트 파일 생성


## 애플리케이션 실행 방법
다음 명령어로 애플리케이션을 실행 합니다.

```bash
spark-submit \
  --class AppMain \
  --master local[*] \
  --driver-memory 32g \
  --executor-memory 4g \
  application.jar
```
