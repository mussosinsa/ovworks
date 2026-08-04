# WebAdmin 및 REST API 사용자 세션 수 관리 검토서

## 1. 검토 결론

현재 구현에서 **WebAdmin 접속 세션과 REST API 클라이언트 접속 세션은 최대 세션 수를 별도로 계산하지 않는다.** 두 접속 방식 모두 `CreateUserSession`을 통해 동일한 Engine 세션 저장소에 등록되며, `ENGINE_MAX_USER_SESSIONS`는 접속 채널이 아니라 동일한 `DbUser.id`를 기준으로 유효 세션 전체에 적용된다.

따라서 제한값이 `N`이면 동일 사용자의 WebAdmin 세션과 REST API 세션을 합산한 수가 `N`에 도달한 뒤 생성되는 새 세션은 거부된다. 예를 들어 제한값이 3이고 WebAdmin 세션 2개와 지속형 REST API 세션 1개가 유효하다면, 해당 사용자의 추가 WebAdmin 및 REST API 세션 생성은 모두 거부된다.

## 2. 소스 코드 근거

### 2.1 공통 세션 생성 경로

* WebAdmin의 SSO 후처리는 `SsoPostLoginServlet`에서 `ActionType.CreateUserSession`을 실행한다.
* REST API 인증은 `SsoRestApiAuthFilter` 또는 `SsoRestApiNegotiationFilter`에서 `SsoUtils.createUserSession()`을 호출한다.
* `SsoUtils.createUserSession()`도 최종적으로 동일한 `ActionType.CreateUserSession`을 실행한다.

즉, 진입 필터와 인증 흐름은 다르지만 Engine 세션 생성 명령은 공통이다.

### 2.2 세션 제한 판정 기준

`CreateUserSessionCommand`는 `ENGINE_MAX_USER_SESSIONS`를 읽고 다음 조건으로 신규 세션을 거부한다.

```java
maxUserSessions != UNLIMITED_SESSIONS
        && sessionDataContainer.getNumUserSessions(user) >= maxUserSessions
```

`SessionDataContainer.getNumUserSessions()`는 세션의 WebAdmin/REST 구분, SSO scope, client ID 또는 source IP를 검사하지 않는다. 동일한 `DbUser.id`를 가진 유효 세션을 모두 세어 반환한다. `appScope`는 세션 생성 후 API 전용 scope 여부를 기록하는 데 사용될 뿐 최대 세션 수 계산의 분리 키로 사용되지 않는다.

## 3. 접속 방식별 동작 차이

| 구분 | WebAdmin | REST API |
|---|---|---|
| 인증 진입점 | SSO post-login servlet | REST 인증/협상 필터 |
| Engine 세션 생성 | `CreateUserSession` | `SsoUtils.createUserSession()` → `CreateUserSession` |
| 최대 세션 설정 | `ENGINE_MAX_USER_SESSIONS` | 동일 설정 |
| 최대 세션 집계 키 | `DbUser.id` | 동일 |
| 채널별 별도 quota | 없음 | 없음 |
| 일반적인 수명 | HTTP 세션과 함께 유지 | 요청 인증 방식과 `Prefer: persistent-auth` 사용 여부에 따라 달라짐 |

REST API의 세션 수명 동작은 최대 세션 수 집계와 별개의 문제이다.

* 기존 Bearer 토큰에 대응하는 Engine 세션이 있으면 REST 필터는 해당 세션을 재사용할 수 있다.
* `Prefer: persistent-auth`를 사용하면 REST API 세션과 TTL을 유지한다.
* Bearer 인증이 아니고 persistent authentication도 요청하지 않은 경우, 인증된 요청 처리 후 REST 관리 필터가 Engine 세션을 로그아웃시킨다. 이 세션은 생성 시점에는 공통 제한 계산에 포함되지만 요청 후 제거되므로 장기적인 동시 세션 점유는 하지 않는다.

## 4. 보안 및 운영 영향

1. **WebAdmin과 REST API가 서로의 세션 여유분을 사용한다.** REST 자동화가 지속형 세션을 많이 만들면 사용자의 WebAdmin 로그인이 거부될 수 있고, 반대 상황도 가능하다.
2. **사용자별 통합 제한이다.** 동일 사용자가 여러 IP, 브라우저 또는 API 클라이언트를 사용해도 별도 할당량이 없다.
3. **`appScope`는 권한/용도 표시이지 quota 분리 기준이 아니다.** scope 값만 변경해도 별도 세션 한도가 생기지 않는다.
4. **기본값 `-1`은 무제한이다.** 실제 제한이 필요하면 `ENGINE_MAX_USER_SESSIONS`를 양의 값으로 설정하고 Engine 재기동 및 로그인 시험을 수행해야 한다.
5. **채널별 제한이 보안 요구사항이라면 추가 구현이 필요하다.** 현재 동작을 WebAdmin `N`, REST API `M`처럼 별도 관리한다고 보안기능 확인서에 기재하면 안 된다.

## 5. 채널별 분리 관리가 필요한 경우의 권고 설계

별도 관리 요구가 있다면 기존 통합 제한의 의미를 임의로 변경하지 말고 다음 항목을 명시적으로 추가해야 한다.

1. 세션에 `WEBADMIN` 또는 `REST_API`와 같은 검증된 채널 분류값을 저장한다.
2. 신뢰할 수 없는 요청 파라미터가 아니라 서버 측 인증 진입점과 승인된 `appScope`를 이용해 채널을 결정한다.
3. `getNumUserSessions(user, channel)` 형태로 사용자와 채널을 함께 집계한다.
4. `ENGINE_MAX_WEBADMIN_USER_SESSIONS` 및 `ENGINE_MAX_REST_API_USER_SESSIONS`처럼 독립된 설정과 감사 로그를 제공한다.
5. Bearer 토큰 재사용, persistent REST 세션, 비지속 REST 요청, WebAdmin 재로그인 및 경합 상황을 단위·통합 시험한다.
6. 기존 `ENGINE_MAX_USER_SESSIONS`와의 하위 호환 정책(전체 상한으로 유지할지, 폐기할지)을 별도로 정의한다.

## 6. 확인 시험 시나리오

| 번호 | 절차 | 현재 구현의 기대 결과 |
|---|---|---|
| SM-01 | 제한을 2로 설정하고 동일 사용자로 WebAdmin 2회 로그인 후 REST 신규 세션 요청 | REST 세션 생성 거부 |
| SM-02 | 제한을 2로 설정하고 지속형 REST 세션 1개 생성 후 WebAdmin 1회 로그인 | 두 세션 모두 허용 |
| SM-03 | SM-02 상태에서 WebAdmin 또는 REST 신규 세션 생성 | 접속 방식과 무관하게 거부 |
| SM-04 | 유효한 Bearer 토큰으로 기존 Engine 세션 재사용 | 신규 세션이 생성되지 않으면 집계 수 증가 없음 |
| SM-05 | 비지속 REST 인증 요청 완료 후 활성 세션 조회 | 요청용 세션이 로그아웃되어 장기 점유하지 않음 |
| SM-06 | 한 세션 종료 후 다른 채널에서 신규 접속 | 통합 여유분이 생겨 신규 세션 허용 |

## 7. 보안기능 확인서 기재 문안

> 제품은 WebAdmin과 REST API의 인증 진입 경로 및 세션 유지 방식을 구분하지만, 사용자 동시 세션 상한은 채널별로 분리하지 않는다. 두 채널에서 생성된 Engine 세션은 동일한 사용자 식별자(`DbUser.id`) 기준으로 합산되며, 합산된 유효 세션 수가 `ENGINE_MAX_USER_SESSIONS`에 도달하면 접속 채널과 관계없이 신규 세션 생성을 거부한다. REST API의 비지속 인증 세션은 요청 처리 후 로그아웃되며, persistent authentication 또는 재사용 가능한 Bearer 세션은 유효한 동안 통합 세션 수에 포함된다.
