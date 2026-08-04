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

## 8. 한 PC에서 여러 브라우저로 접속하는 경우

### 8.1 검토 결론

세션은 PC 또는 IP 주소 단위가 아니라 **브라우저 쿠키 저장소의 HTTP 세션과 Engine 사용자 세션 단위**로 처리된다. Chrome, Firefox, Edge처럼 서로 다른 브라우저는 쿠키 저장소를 공유하지 않으므로, 동일 PC와 동일 IP에서 동일 사용자로 로그인해도 일반적으로 각각 별도의 HTTP 세션과 Engine 세션이 생성된다. 이렇게 생성된 Engine 세션들은 모두 동일 사용자의 통합 세션 수에 포함된다.

브라우저 종류 자체를 식별하여 한도를 분리하거나, 동일 IP라는 이유로 여러 브라우저를 하나의 세션으로 합치는 코드는 없다. `sourceIp`는 세션 정보와 감사·표시 목적으로 저장되지만 `getNumUserSessions()`의 집계 조건에는 포함되지 않는다.

### 8.2 브라우저 사용 형태별 처리

| 사용 형태 | 일반적인 세션 처리 | 동시 세션 수 영향 |
|---|---|---|
| 동일 브라우저·동일 프로필의 여러 탭 | 동일 쿠키와 HTTP 세션을 공유하므로 기존 Engine 세션을 사용 | 보통 1개 |
| 서로 다른 브라우저 | 각 브라우저의 쿠키 저장소가 분리되어 별도 로그인/세션 생성 | 브라우저별로 증가 |
| 동일 브라우저의 일반 창과 시크릿/사생활 보호 창 | 쿠키 저장소가 분리되어 별도 세션 생성 | 각각 증가 |
| 동일 브라우저의 서로 다른 사용자 프로필 | 프로필별 쿠키 저장소가 분리되어 별도 세션 생성 | 프로필별로 증가 |
| 브라우저 새로고침 | HTTP 세션 쿠키가 유효하면 기존 Engine 세션 재사용 | 증가하지 않음 |
| 쿠키 삭제 후 재로그인 | 기존 세션이 서버에서 아직 유효하면 기존 세션과 별도로 신규 세션 생성 가능 | 신규 세션만큼 증가 |
| 한 브라우저에서 정상 로그아웃 | 해당 HTTP 세션에 연결된 Engine 세션을 `LogoutSession`으로 종료하고 HTTP 세션 무효화 | 일반적으로 1개 감소 |
| 브라우저 창만 닫음 | 서버 로그아웃이 즉시 호출된다고 보장할 수 없음 | timeout/정리 전까지 유지 가능 |

표의 동작은 일반적인 브라우저 쿠키 정책을 전제로 한다. 브라우저 확장 기능, 관리 정책 또는 프록시가 쿠키를 공유·차단하는 경우에는 달라질 수 있으므로 보안 검증은 실제 운영 브라우저 정책으로 수행해야 한다.

### 8.3 소스 코드 기준 처리 흐름

1. WebAdmin 로그인 성공 시 `SsoPostLoginServlet`은 `request.getSession(true)`로 현재 브라우저 요청의 HTTP 세션을 얻고, 생성된 Engine session ID를 그 HTTP 세션 속성에 저장한다.
2. 후속 요청에서 `SessionValidationFilter`는 요청과 HTTP 세션에 저장된 Engine session ID가 유효한지 확인한다. 유효하지 않으면 요청 속성을 제거하거나 해당 HTTP 세션을 무효화한다.
3. 새 브라우저는 기존 브라우저의 HTTP 세션 쿠키를 보내지 않으므로 별도 로그인 흐름을 거쳐 새 Engine 세션을 만들게 된다.
4. `CreateUserSessionCommand`는 source IP나 브라우저 종류가 아닌 `DbUser.id` 기준 유효 세션 수를 확인하므로, 한 PC의 여러 브라우저 세션도 모두 합산한다.
5. 정상 로그아웃 시 `SsoLogoutServlet`은 현재 HTTP 세션에서 Engine session ID를 구해 `LogoutSession`을 실행한 뒤 현재 HTTP 세션을 무효화한다.

### 8.4 동일 PC 다중 브라우저 예시

`ENGINE_MAX_USER_SESSIONS=3`이고 사용자 A가 동일 PC에서 접속하는 경우를 예로 들면 다음과 같다.

1. Chrome 로그인: 사용자 A의 유효 세션 수는 1이다.
2. Firefox 로그인: 별도 쿠키를 사용하므로 유효 세션 수는 2이다.
3. Edge 로그인: 유효 세션 수는 3이다.
4. Chrome 시크릿 창에서 추가 로그인: 새 세션 생성이 시도되지만 사용자 A의 통합 한도 3에 도달했으므로 거부된다.
5. Firefox에서 정상 로그아웃: Firefox에 연결된 Engine 세션이 종료되어 유효 세션 수는 2가 된다.
6. Chrome 시크릿 창에서 다시 로그인: 여유분이 생겼으므로 새 세션이 허용된다.

### 8.5 추가 확인 시험

| 번호 | 절차 | 기대 결과 |
|---|---|---|
| MB-01 | 새 로그인을 강제하지 않고 동일 브라우저·동일 프로필에서 탭 3개로 WebAdmin 사용 | 동일 session cookie를 사용하고 Engine 세션 수는 1개 유지 |
| MB-02 | Chrome, Firefox, Edge에서 동일 사용자로 각각 로그인 | 유효 Engine 세션 수가 3개로 증가 |
| MB-03 | 동일 PC의 일반 창과 시크릿 창에서 동일 사용자 로그인 | 서로 다른 Engine 세션 2개 생성 |
| MB-04 | 제한 도달 후 동일 PC의 새 브라우저에서 로그인 | 동일 IP라도 신규 세션 생성 거부 |
| MB-05 | 브라우저 하나에서 로그아웃 후 다른 브라우저 상태 확인 | 로그아웃한 브라우저의 세션만 종료되고 다른 유효 세션은 유지 |
| MB-06 | 브라우저를 로그아웃 없이 종료하고 즉시 활성 세션 조회 | 서버 timeout 전까지 세션이 남아 있을 수 있음 |
| MB-07 | 쿠키 삭제 후 재로그인하고 기존 세션이 남아 있는지 조회 | 기존 서버 세션이 만료되지 않았다면 신규 세션과 함께 집계 |

### 8.6 제출 문안 보완

> 동일 PC에서 서로 다른 브라우저 또는 분리된 브라우저 프로필로 접속하는 경우, 제품은 PC나 source IP를 기준으로 세션을 병합하지 않는다. 각 브라우저 쿠키 저장소에서 성립한 HTTP 세션은 각각의 Engine 세션에 연결되며, 동일 사용자의 모든 유효 Engine 세션은 브라우저 종류 및 source IP와 관계없이 `ENGINE_MAX_USER_SESSIONS` 한도에 합산된다. 동일 브라우저 프로필의 여러 탭은 일반적으로 같은 HTTP 세션 쿠키를 공유하므로 하나의 Engine 세션을 재사용한다. 정상 로그아웃하지 않고 브라우저만 종료하면 서버 세션은 즉시 제거되지 않을 수 있으며 설정된 세션 timeout 또는 정리 절차가 적용될 때까지 한도를 점유할 수 있다.
