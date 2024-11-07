# Spark 데이터 처리 애플리케이션

## 개요

csv 형태의 로그 파일을 받아 처리 후 parquet 파일로 저장, 관련 데이터를 hive에 제공하는 Spark 어플리케이션입니다.

&nbsp;

## 주요 기능
1. Apache Spark를 활용한 CSV 데이터 처리

2. 타임스탬프 변환 및 시간대 처리

3. 날짜 기반 파티셔닝

4. Parquet 파일 저장 및 Snappy 압축

5. Hive 테이블 연동 및 파티션 처리

6. 자동 재시도 및 오류 처리

7. 오류 발생 시 Slack 알림 전송

8. 작업 완료 시 파일명명 기반 체크포인트 생성. 이후 배치 시 해당 체크포인트 체크하여 작업 진행

&nbsp;



## 필수 요구사항
- Apache Spark
- Scala
- SBT (Scala Build Tool)
- Apache Hive
- Slack Webhook URL (알림 기능용)
  
&nbsp;




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
       ├─jobs
       │      DataHandler.scala
       │
       └─utils
               SlackNotifier.scala
               SparkInitializer.scala
```

&nbsp;


## 출력 스키마
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

&nbsp;

## 오류 처리
- 최대 재시도 횟수: 3회
- 재시도 간격: 5초
- 최대 재시도 횟수 초과 시 Slack 알림으로 오류내역 송신
- 작업 완료 시 체크포인트 파일 생성


&nbsp;
&nbsp;

# 설정
## application.conf

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
  spark {
    mode = "local[*]"
    driverMemory = "16g"
    executorMemory = "16g"
    shufflePartitions = 400
    parquetSize = 134217728
    maxRecordsPerFile = 100000
  }
}
```

&nbsp;

## build.sbt
```scala
// build.sbt
ThisBuild / version := "0.1"
ThisBuild / scalaVersion := "2.12.18"

lazy val root = (project in file("."))
  .settings(
    name := "DataHandler",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.3",
      "org.apache.spark" %% "spark-sql" % "3.5.3",
      "org.apache.spark" %% "spark-hive" % "3.5.3",
      "com.typesafe" % "config" % "1.4.2",
      "org.scala-lang" % "scala-library" % "2.12.18"
),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  )
```
&nbsp;

## 애플리케이션 실행 방법
다음 명령어로 애플리케이션을 실행

```bash
spark-submit \
  --class AppMain \
  --master local[*] \
  --driver-memory 32g \
  --executor-memory 4g \
  application.jar
```
