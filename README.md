 # CalFit
  ### **B2B 채용 일정 관리 ATS**

  <div align="center">
    <img
      src="[대표 이미지 URL 입력]"
      alt="CalFit 대표 이미지"
      width="240"
    />
  </div>

  ---

  <br/>

  ## 목차
  ### 1.  팀 소개
  ### 2.  프로젝트 개요
  ### 3.  프로젝트 선정 이유
  ### 4.  핵심 기능
  ### 5.  유사 서비스
  ### 6.  개발 환경 및 기술 스택
  ### 7.  프로젝트 구조
  ### 8.  프로젝트 기획
  ### 9.  컨벤션
  ### 10. 프로젝트 기능 상세
  ### 11. 트러블 슈팅
  ### 12. 아쉬운 점 및 한계
  ### 13. 회고

  <br/>

  ---

  <br/>

  ## 1. 팀 소개
  <table width="100%">
    <thead>
      <tr align="center">
        <th width="20%"> [팀원1 이름] </th>
        <th width="20%"> [팀원2 이름] </th>
        <th width="20%"> [팀원3 이름] </th>
        <th width="20%"> [팀원4 이름] </th>
      </tr>
    </thead>
    <tbody>
      <tr align="center">
        <td><img src="[팀원1 이미지 URL]" width="80%" /></td>
        <td><img src="[팀원2 이미지 URL]" width="80%" /></td>
        <td><img src="[팀원3 이미지 URL]" width="80%" /></td>
        <td><img src="[팀원4 이미지 URL]" width="80%" /></td>
      </tr>
    </tbody>
    <tbody>
      <tr align="center">
        <td> [담당 기능 입력] </td>
        <td> [담당 기능 입력] </td>
        <td> [담당 기능 입력] </td>
        <td> [담당 기능 입력] </td>
      </tr>
    </tbody>
  </table>

  <br/>

  ---

  <br/>

  ## 2. 프로젝트 개요

  ### 소개
  - **CalFit**은 채용 공고 관리, 지원자 추적, 면접 일정 조율, 회의실 예약, 구글 캘린더 연동, 리포트 분석까지 통합 지원하는 **B2B 채용 일정 관리 ATS**입니다.
  - 기업은 채용 과정에서 발생하는 지원자 관리와 면접 운영 업무를 하나의 서비스 안에서 처리할 수 있으며, 면접관은 별도 조율 없이 배정된 면접 일정을 빠르게 확인할 수 있습니
  다.
  - 단순한 지원자 관리 시스템을 넘어, **사람과 공간, 시간을 함께 조율하는 채용 운영 플랫폼**을 목표로 개발하였습니다.

  ### 한 줄 소개
  > 지원자, 면접관, 회의실, 전형 일정을 하나의 흐름으로 연결한 B2B 채용 운영 플랫폼

  <br/>

  ---

  <br/>

  ## 3. 프로젝트 선정 이유
  - 채용 운영은 공고, 지원자, 면접관, 회의실, 일정 등 여러 자원을 동시에 조율해야 하는 복잡한 도메인이기 때문에 협업 프로젝트 주제로 적합하다고 판단하였습니다.
  - 단순 CRUD를 넘어 **일정 충돌 방지, 자동 배정, 알림, 외부 캘린더 연동, 리포트 분석** 등 실무적인 문제를 폭넓게 경험할 수 있는 주제였습니다.
  - 실제 기업 환경에서 활용 가능한 **B2B SaaS 구조**를 설계하고 구현해보는 경험을 얻고자 하였습니다.
  - 사용자 화면 구현뿐 아니라, 도메인 설계와 운영 흐름을 함께 고려하는 프로젝트를 만들고자 하였습니다.

  <br/>

  ---

  <br/>

  ## 4. 핵심 기능

  ### 맞춤형 채용 설계
  - 채용 공고 생성 및 관리
  - 지원서 템플릿 생성 및 적용
  - 전형 단계 기반 채용 프로세스 구성

  ### 칸반 기반 지원자 관리
  - 전형 단계별 지원자 상태 시각화
  - 직관적인 상태 추적 및 관리
  - 자동 트리거와 이메일 알림 지원

  ### 스마트 일정 & 공간 예약
  - 면접 일정 자동 배정
  - 면접관 일정 자동 반영
  - 회의실 예약 및 일정 충돌 방지

  ### 구글 캘린더 연동
  - OAuth 2.0 기반 외부 캘린더 연동
  - 내부 일정과 외부 캘린더 동기화
  - 기존 업무 환경과 자연스러운 연결

  ### 데이터 리포트 & 자동 결제
  - 채용 성과 통계 분석
  - 월별 신규 지원자 추이 추적
  - 정기 결제 및 인보이스 통합 관리

  <br/>

  ---

  <br/>

  ## 5. 유사 서비스

  ### Greenhouse / Lever / Workable
  - 기업 채용 프로세스를 통합 관리하는 대표적인 ATS 서비스입니다.
  - 지원자 추적, 채용 파이프라인 관리, 일정 조율 등 채용 운영에 필요한 기능을 제공합니다.
  - CalFit은 이러한 ATS 구조를 참고하되, **회의실 예약, 면접 일정 자동 배정, 면접관 일정 반영, 구글 캘린더 연동**을 더 긴밀하게 연결하는 데 초점을 두었습니다.

  <br/>

  ---

  <br/>

  ## 6. 개발 환경 및 기술 스택

  ### Backend
  - Java 17 <img src="https://img.shields.io/badge/Java_17-007396?style=for-the-badge&logo=openjdk&logoColor=white">
  - Spring Boot 3.5.3 <img src="https://img.shields.io/badge/SpringBoot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
  - Spring Security <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white">
  - Gradle Multi Module <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white">
  - MariaDB <img src="https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white">
  - Redis <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white">

  ### Frontend
  - Vue 3 <img src="https://img.shields.io/badge/Vue.js-4FC08D?style=for-the-badge&logo=vue.js&logoColor=white">
  - Vite <img src="https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white">
  - Vue Router <img src="https://img.shields.io/badge/Vue_Router-4FC08D?style=for-the-badge&logo=vue.js&logoColor=white">
  - Pinia <img src="https://img.shields.io/badge/Pinia-FFD859?style=for-the-badge&logo=vue.js&logoColor=black">
  - Axios <img src="https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white">
  - Tailwind CSS <img src="https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white">

  ### External Integration
  - Google Calendar API
  - OAuth 2.0
  - Toss Payments SDK

  ### IDE & Tools
  - IntelliJ IDEA <img src="https://img.shields.io/badge/IntelliJ_IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white">
  - VS Code <img src="https://img.shields.io/badge/VS_Code-007ACC?style=for-the-badge&logo=visualstudiocode&logoColor=white">
  - Git <img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white">

  ### Collaboration
  - GitHub <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white">
  - Jira <img src="https://img.shields.io/badge/Jira-0052CC?style=for-the-badge&logo=jira&logoColor=white">
  - Notion <img src="https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white">
  - Discord <img src="https://img.shields.io/badge/Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white">

  <br/>

  ---

  <br/>

  ## 7. 프로젝트 구조

  ### Back-end 구조
  ```bash
  BE
  ├─ clients
  │  └─ google-calendar
  ├─ core
  │  ├─ core-api
  │  ├─ core-enum
  │  ├─ domain-announcement-board
  │  ├─ domain-applicant
  │  ├─ domain-application
  │  ├─ domain-billing
  │  ├─ domain-calendar
  │  ├─ domain-career
  │  ├─ domain-interview
  │  ├─ domain-meeting-room
  │  ├─ domain-notification
  │  ├─ domain-payment
  │  ├─ domain-recruitment
  │  ├─ domain-report
  │  ├─ domain-user
  │  └─ domain-workspace
  ├─ storage
  │  └─ db-core
  └─ support
     ├─ email
     ├─ error
     ├─ event
     ├─ logging
     ├─ monitoring
     └─ security

  ### Front-end 구조

  FE
  ├─ public
  ├─ src
  │  ├─ api
  │  ├─ components
  │  │  ├─ meeting-rooms
  │  │  ├─ recruitment
  │  │  ├─ schedule
  │  │  └─ ...
  │  ├─ router
  │  ├─ stores
  │  ├─ views
  │  │  ├─ interview
  │  │  ├─ meeting-rooms
  │  │  └─ ...
  │  └─ ...
  └─ ...

  ### 아키텍처 특징

  - 프론트엔드와 백엔드를 분리한 구조
  - Spring Boot 기반 멀티 모듈 아키텍처 적용
  - 도메인별 책임 분리를 위한 모듈화
  - 복잡한 비즈니스 흐름은 퍼사드 계층을 통해 조율
  <br/>

  ———
  <br/>

  ## 8. 프로젝트 기획

  ### 요구사항 명세서

  - [요구사항 명세서 이미지 또는 링크 입력]

  ### ERD

  - [ERD 이미지 또는 링크 입력]

  ### 스토리보드 / 와이어프레임

  - [Figma 링크 또는 이미지 입력]

  ### 테스트 케이스

  - [테스트 케이스 이미지 또는 링크 입력]

  ### 화면 설계

  - [주요 화면 이미지 입력]
  <br/>

  ———
  <br/>

  ## 9. 컨벤션

  ### 공통 규칙

  - 이슈 생성 후 브랜치 생성 및 작업 진행
  - 기능 단위로 Pull Request 생성
  - 최소 1명 이상의 리뷰 후 병합
  - 실행 확인 후 커밋, 푸시, 머지 진행

  ### Branch Convention

  - main
  - develop
  - feature/[이슈번호]-[기능명]
  - fix/[이슈번호]-[버그명]

  ### Commit Convention

  - feat: 새로운 기능 구현
  - fix: 버그 수정
  - refactor: 리팩터링
  - docs: 문서 수정
  - test: 테스트 코드 작성 및 수정
  - chore: 설정, 의존성, 구조 변경
  - style: 스타일 및 포맷 수정

  예시:

  [feat] #12 면접 일정 자동 배정 API 구현
  [fix] #21 회의실 예약 충돌 처리 수정
  [docs] #30 README 업데이트

  ### Code Convention

  - 클래스: UpperCamelCase
  - 메서드 / 변수: lowerCamelCase
  - 상수: UPPER_SNAKE_CASE
  - 패키지 및 폴더: 소문자 기반 명명
  - 공통 로직은 서비스/스토어/유틸로 분리
  <br/>

  ———
  <br/>

  ## 10. 프로젝트 기능 상세

  ### 1. 맞춤형 채용 설계

  - 채용 공고 생성부터 관리까지 지원
  - 지원서 템플릿을 생성하고 공고별로 적용 가능
  - 전형 단계 기반 채용 프로세스를 구성할 수 있도록 설계

  ### 2. 칸반 기반 지원자 관리

  - 전형 단계별 지원자 분포를 한눈에 확인 가능
  - 지원자 상태를 칸반 보드 방식으로 직관적으로 추적 가능
  - 자동 트리거와 이메일 알림을 통한 후속 처리 지원

  ### 3. 면접 일정 생성

  - 채용 공고 상세페이지에서 면접관, 지원자, 회의실을 선택해 면접 일정 생성 가능
  - 필요한 자원을 한 화면에서 조율하며 일정을 빠르게 등록 가능
  - 면접 생성 후 관련 정보가 연결된 상태로 저장됨

  ### 4. 면접 일정 자동 배정

  - 면접관, 회의실, 시간 충돌을 종합적으로 고려해 일정 자동 조율 가능
  - 수작업 일정 조정 부담을 줄이고 운영 효율 향상
  - 복잡한 내부 자원 조율 과정을 시스템이 보조

  ### 5. 면접관 일정 연동

  - 면접 일정 생성 시 면접관에게 즉시 알림 전달
  - 생성된 면접 일정이 면접관 개인 일정에 자동 반영
  - 면접관 로그인 후 내 일정에서 배정된 면접 일정 확인 가능

  ### 6. 회의실 수동 예약

  - 회의실 화면에서 일, 주, 월 단위 일정표 확인 가능
  - 원하는 시간 칸을 직접 선택해 예약 시작 가능
  - 회의 제목, 시간, 참석자 등 예약 정보 입력 후 수동 예약 생성 가능
  - 예약 완료 시 일정표에 즉시 반영되어 확인 가능

  ### 7. 구글 캘린더 연동

  - 구글 계정 인증을 통해 캘린더 접근 권한 획득
  - 내부 일정들을 구글 캘린더와 연동하여 외부 환경에서도 확인 가능
  - 기존 업무 환경에서 일정을 빠르게 확인할 수 있도록 사용성 강화

  ### 8. 내 일정 관리

  - 개인 일정 생성 및 수정 가능
  - 참석자 일정 현황 및 충돌 여부 확인 가능
  - 본인 일정, 회의실 예약, 면접 일정이 연결된 통합 일정 관리 제공

  ### 9. 데이터 리포트

  - 전형 단계별 지원자 분포 확인 가능
  - 월별 신규 지원자 추이 추적 가능
  - 공고별 지원자 현황 및 면접 운영 현황 확인 가능
  - 자동화 실행 상태와 채널별 통계 확인 가능

  ### 10. 자동 결제 및 인보이스 관리

  - 정기 결제 상태 관리
  - 인보이스 통합 조회
  - 기업 단위 서비스 운영을 고려한 결제 흐름 지원
  <br/>

  ———
  <br/>

  ## 11. 트러블 슈팅

  ### 1. Race Condition 경험

  - 일정 생성과 예약 반영 과정에서 동시 요청에 따른 상태 불일치 가능성을 경험하였습니다.
  - 충돌 검증 시점과 저장 순서를 조정하며 안정성을 보완하였습니다.

  ### 2. 회의실 일정 알고리즘 복잡도

  - 일간, 주간, 월간 뷰를 모두 지원하면서 예약 슬롯과 시간축을 일관되게 관리해야 했습니다.
  - 회의실 예약 데이터와 화면 인터랙션을 함께 고려해야 해 구현 난도가 높았습니다.

  ### 3. 자동 배정 로직 구현

  - 면접관, 지원자, 회의실, 시간 조건을 동시에 고려해야 했기 때문에 단순 비교 이상의 설계가 필요했습니다.
  - 도메인 규칙을 반영한 자동 배정 로직을 구현하는 과정이 도전적인 경험이었습니다.

  ### 4. 구조 재정비

  - 개발 도중 복잡한 비즈니스 로직을 더 명확히 분리하기 위해 퍼사드 패턴 기반으로 구조를 재정비하였습니다.
  - 이후 흐름은 정리되었지만, 초기 설계의 중요성을 크게 체감할 수 있었습니다.
  <br/>

  ———
  <br/>

  ## 12. 아쉬운 점 및 한계

  ### 코드 품질 측면

  - 한정된 개발 일정으로 인해 충분한 코드 리뷰 시간을 확보하지 못해 협업 관점에서의 코드 품질 관리에 아쉬움이 남았습니다.
  - 기능 확장 과정에서 일부 로직이 한 파일에 집중되어 가독성과 유지보수성이 낮아진 부분이 있었습니다.
  - 테스트 코드와 예외 처리 보완이 더 필요했습니다.

  ### 아키텍처 측면

  - 초기 구조 설계가 명확하게 정리되지 않아 개발 중간에 퍼사드 패턴으로 구조를 재정비하는 과정이 필요했습니다.
  - 그만큼 구현 흐름과 일정 관리 측면에서 아쉬움이 남았습니다.

  ### 기술적 측면

  - 회의실, 구글 캘린더 연동, 내 일정 반영, 면접 일정 조율 등 내부 자원을 함께 조율하는 기능이 많아 도메인 복잡도가 높아졌습니다.
  - 동시성 문제와 상태 정합성을 더 안정적으로 제어할 필요가 있었습니다.

  ### UX 측면

  - 복잡한 기능을 한 화면에서 다루는 경우 정보량이 많아 일부 화면은 직관성을 더 보완할 필요가 있었습니다.
  - 화면 크기나 사용 환경에 따라 인터랙션 완성도를 더 세밀하게 다듬을 필요가 있었습니다.
  <br/>

  ———
  <br/>

  ## 13. 회고
  <details>
  <summary style="font-size:1.1em;"> [팀원1 이름] </summary>
  <div markdown="1">

  [회고 내용 입력]
  </div>
  </details>
  <details>
  <summary style="font-size:1.1em;"> [팀원2 이름] </summary>
  <div markdown="1">

  [회고 내용 입력]
  </div>
  </details>
  <details>
  <summary style="font-size:1.1em;"> [팀원3 이름] </summary>
  <div markdown="1">

  [회고 내용 입력]
  </div>
  </details>
  <details>
  <summary style="font-size:1.1em;"> [팀원4 이름] </summary>
  <div markdown="1">

  [회고 내용 입력]
  </div>
  </details>
  <br/>

  ———
  <br/>
