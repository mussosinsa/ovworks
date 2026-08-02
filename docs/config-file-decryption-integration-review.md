# 주요 설정 파일 복호화 연동 검토서

## 1. 검토 대상과 결론

| 대상 | 주요 소비자 | 현재 패치 사용 판정 |
|---|---|---|
| `10-setup-database.conf` | Engine launcher, `engine-setup`, `engine-backup` | **조건부 사용 가능** |
| `10-setup-dwh-database.conf` | Engine/DWH setup 및 backup | **조건부 사용 가능** |
| `internal.properties` | AAA JDBC extension, setup 및 backup | **runtime 0600 파일 방식 적용 완료** |

`encryptor.py --decrypt <파일>`과 `-d <파일>` 호출 규격은 기존 setup·backup 코드와 호환된다. 또한 Python·Java runtime reader와 AAA runtime 파일 처리가 적용되어 세 파일을 암호문으로 보관할 수 있다. 다만 동일 credential 배포와 통합시험 없이 일괄 암호화를 활성화하면 fail-closed 정책에 따라 Engine 또는 AAA 기동이 중단된다.

### 1.1 요청 경로별 실제 소스 판정

| 요청 경로 | setup에서 복호화 | backup에서 복호화 | Engine/AAA 정상 기동 시 복호화 |
|---|:---:|:---:|:---:|
| `/etc/ovirt-engine/engine.conf.d/10-setup-database.conf` | 예 | 예 | **예** |
| `/etc/ovirt-engine/engine.conf.d/10-setup-dwh-database.conf` | 예 | 예 | **예** |
| `/etc/ovirt-engine/aaa/internal.properties` | 예 | 예 | **예(runtime 0600 파일)** |

setup 경로는 `packaging/setup/ovirt_engine_setup/engine/constants.py`의 세 상수와 `db/connection.py::_setup()` 호출로 확인된다. backup 경로는 `packaging/bin/engine-backup.sh.in::my_load_config()`가 세 경로를 명시적으로 `decrypt_config_if_allowed()`에 전달한다. 정상 Engine 기동에는 다음 runtime reader가 적용되었다.

- Python `ConfigFile._openFile()`은 magic을 확인하고 GCM을 메모리 복호화한다.
- Java `ShellLikeConfd.loadProperties()`는 공용 `EncryptedConfigFile.open()`을 사용한다.
- `ExtensionsManager`는 암호화된 `internal.properties`를 0600 runtime 파일로 materialize하고 datasource 경로를 교체한다.
- `Configuration.loadFile()`도 공용 reader를 사용한다.

따라서 세 요청 경로 모두 읽을 때 자동 복호화된다. 단, Engine systemd unit에 동일 passphrase credential이 공급되지 않으면 fail closed로 기동이 중단된다.

## 2. 호출 경로별 검토

### 2.1 `engine-setup`

`packaging/setup/plugins/ovirt-engine-common/ovirt-engine/db/connection.py`는 세 basename만 허용하고 다음 순서로 처리한다.

1. 원본 바이트를 메모리에 보관한다.
2. `/usr/share/ovirt-engine/encryptor/encryptor.py --decrypt <경로>`를 호출한다.
3. 실패하면 원본을 복원하고 `-d <경로>`를 재시도한다.
4. Engine DB 설정은 복호화 후 `ENGINE_DB_PASSWORD` 존재 여부도 검사한다.

CLI 인자는 호환되지만 다음 주의사항이 있다.

- setup 프로세스에는 systemd의 `CREDENTIALS_DIRECTORY`가 자동 제공되지 않는다. `OVIRT_ENCRYPTOR_PASSPHRASE` 또는 권한 0600 `secret_file`을 별도로 제공해야 한다.
- setup은 파일을 제자리 복호화한 후 자동으로 `OVENC001`로 되돌리는 종료 hook이 없다. setup 완료 후 `encryptor.py --encrypt` 또는 승인된 재암호화 절차가 필요하다.
- setup 도중 설정이 갱신될 수 있으므로 과거 암호문을 단순 복원하면 변경사항이 유실된다. **최종 평문을 새 GCM 파일로 재암호화**해야 한다.

따라서 setup 연동은 “복호화 성공”만으로 적합 판정하지 않고 작업 후 재암호화와 자체 복호화 검증까지 확인해야 한다.

### 2.2 `engine-backup`

`packaging/bin/engine-backup.sh.in`은 세 basename만 복호화한다. 각 파일을 권한 0600 임시파일에 `cp -a`로 백업하고, `--decrypt` 실패 시 `-d`를 시도한다. `cleanup()`이 성공·실패 종료 모두에서 `restore_decrypted_configs()`를 호출하여 암호문 원본을 복원한다.

다음 조건에서는 현재 패치를 사용할 수 있다.

- passphrase가 환경변수 또는 0600 비밀 파일 설정으로 backup 프로세스에 공급된다.
- 프로세스가 파일 소유자·권한을 복원할 권한을 가진다.
- 비정상 강제 종료(`SIGKILL`, 전원 장애) 후 평문 잔류 탐지·복구 절차가 있다.

systemd credential을 사용하려면 backup을 실행하는 systemd unit에도 `LoadCredentialEncrypted=`를 선언해야 한다. 대화형 shell에서 실행하는 `engine-backup`은 systemd credential 디렉터리를 자동으로 상속하지 않는다.

`encrypt_conf_files.py` 자체가 `LoadCredentialEncrypted=`를 해석하는 것은 아니다. 해당 지시자는 스크립트를 실행하는 systemd unit의 `[Service]`에 다음과 같이 한 줄로 선언한다.

```ini
[Service]
LoadCredentialEncrypted=ovirt-encryptor-passphrase:/etc/credstore.encrypted/ovirt-encryptor-passphrase
```

systemd가 실행 시 복호화된 credential을 `$CREDENTIALS_DIRECTORY/ovirt-encryptor-passphrase`에 제공하며, 스크립트는 이 경로를 환경변수와 0600 secret file보다 먼저 읽는다. `LoadCredentialEncrypted=` 다음 줄에 값만 들여쓰는 표기는 유효한 연속 행이 아니므로 사용하지 않는다.

### 2.3 Engine 서비스의 DB 설정 읽기

`packaging/services/ovirt-engine/ovirt-engine.py.in`은 시작 시 Python `ConfigFile`을 사용한다. `ConfigFile._openFile()`은 `OVENC001` magic을 확인하고 승인된 파일을 Python encryptor로 메모리 복호화한다. Java Engine/SSO reader인 `ShellLikeConfd`도 공용 `EncryptedConfigFile.open()`을 사용하므로 두 DB 설정을 암호문 상태에서 읽는다.

두 reader 모두 header/version/tag 또는 credential 오류 시 plaintext fallback 없이 실패한다. 운영 적용 전 `ovirt-engine.service`에 credential을 제공하고 Python↔Java 고정 vector 및 Engine/SSO 재기동을 시험한다.

### 2.4 `internal.properties` 읽기

AAA JDBC extension 설정은 `config.datasource.file=/etc/ovirt-engine/aaa/internal.properties` 형태로 Java extension에 전달된다. `ExtensionsManager.loadImpl()`은 이 경로가 암호화됐는지 확인하고 공용 reader로 복호화한 권한 0600 runtime 파일을 만든 뒤 property를 runtime 경로로 교체한다.

외부 AAA JDBC extension은 기존 properties 파일 API를 그대로 사용한다. runtime 파일은 systemd가 mode 0700으로 생성한 `$RUNTIME_DIRECTORY` 아래에 mode 0600으로 생성되고 JVM 종료 시 삭제 대상으로 등록된다. 운영 시험에서는 프로세스 소유권, 정상·오류 종료 시 삭제 및 `admin@internal` 로그인을 확인한다.

## 3. 암호화 도구 자체 호환성 검토

| 항목 | 결과 |
|---|---|
| 기존 `--decrypt FILE`, `-d FILE` | 호환 |
| 같은 입력·출력 경로 | 임시파일, `fsync()`, `os.replace()`로 지원 |
| 복호화 출력 권한 | 0600 강제, 기존 uid/gid 유지 |
| 중복 GCM 암호화 | `OVENC001` magic으로 거부·일괄 처리에서는 skip |
| 변조·잘못된 키·손상 | GCM tag 오류로 출력 전 실패 |
| legacy CBC | 명시적 `legacy_cbc` 설정에서만 읽기 지원 |
| CBC 재암호화 | legacy를 먼저 복호화한 뒤 GCM으로 마이그레이션하여 이중 암호화 방지 |
| CBC 완전 차단 | `--deny-legacy-cbc` 지원 |

레거시 CBC에는 인증 태그가 없으므로 잘못된 키가 우연히 올바른 PKCS#7 padding을 만들 가능성을 제거할 수 없다. 마이그레이션 대상의 평문 구조와 필수 키를 추가 검증한 후 GCM 전환을 완료하고 `--deny-legacy-cbc`를 활성화해야 한다.

## 4. 적용 전 필수 시험

1. 세 파일 각각에 시험용 사본을 만들고 `OVENC001` 암호화·복호화 round trip을 확인한다.
2. 암호문 본문, wrapped DEK, header를 각각 변조하여 모두 출력 없이 실패하는지 확인한다.
3. 잘못된 systemd credential, 환경변수 및 secret file로 실패하는지 확인한다.
4. `engine-setup` 정상·오류 종료 후 세 파일이 GCM 암호문인지 확인한다.
5. `engine-backup` 정상 종료, 일반 오류, TERM 및 KILL 조건에서 평문 잔류 여부를 확인한다.
6. 암호화된 DB 설정으로 Engine 재기동을 시험한다. 사전 복호화 또는 launcher 개선 없이는 실패가 예상된다.
7. 암호화된 `internal.properties`로 `admin@internal` 인증을 시험한다. AAA 연동 개선 없이는 실패가 예상된다.
8. 적용 중단·롤백 시 파일 소유자, 그룹, 권한, SELinux context 및 서비스 동작을 확인한다.

## 5. 최종 판정

- **암호화 도구 자체:** 적합 — GCM 인증, 원자적 교체, 권한 및 경로 통제가 구현되어 있다.
- **`engine-backup` 복호화:** 조건부 적합 — key 공급과 강제 종료 후 복구 절차가 필요하다.
- **`engine-setup` 복호화:** 조건부 적합 — 완료 후 변경된 파일 재암호화 절차가 필요하다.
- **Engine DB 설정 직접 읽기:** 적합 — Python과 Java reader에 fail-closed GCM 복호화가 적용되었다.
- **AAA `internal.properties` 직접 읽기:** 조건부 적합 — ExtensionsManager가 0600 runtime 파일을 제공하며 credential 배포·통합시험이 필요하다.

세 파일 전체에 대한 자동 암호화는 위의 Engine 및 AAA 읽기 경로가 개선되기 전에는 운영 환경에서 활성화하지 않는다.

이를 코드 수준에서도 강제하기 위해 `encrypt_conf_files.py`는 기본 실행을 거부하며, 운영자가 runtime 복호화 배포·시험을 완료했다는 의미의 `--acknowledge-runtime-decryption`을 명시한 경우에만 일괄 암호화를 수행한다. 이 옵션은 복호화 기능을 추가하지 않으며 잘못된 운영 활성화를 방지하는 안전장치이다.

## 6. 지정된 설정 reader에 보안 패치 적용 가능성

### 6.1 Python `ovirt_engine/configfile.py`

대상 소스는 저장소의 `packaging/pythonlib/ovirt_engine/configfile.py`이며 설치 후 `/usr/lib/python3.6/site-packages/ovirt_engine/configfile.py`에 해당한다. `ConfigFile.__init__()`은 기본 파일과 `<vars>.d/*.conf`를 순서대로 `loadFile()`에 전달하고, `loadFile()`은 `open(file, 'r')`로 즉시 UTF-8 호환 text line을 파싱한다.

| 검토 항목 | 판정 |
|---|---|
| `10-setup-database.conf` 메모리 복호화 적용 | **가능·권장** |
| `10-setup-dwh-database.conf` 메모리 복호화 적용 | **가능·권장** |
| `internal.properties` 적용 | 직접 호출 경로는 아니며 ExtensionsManager runtime 처리로 해결 |

적용 방식은 `loadFile()`에서 binary prefix를 먼저 읽고 `OVENC001`이면 승인된 경로·basename을 검증한 뒤 `encryptor.decrypt_bytes()` 결과를 `io.StringIO`로 파싱하는 것이다. 원본 파일을 제자리 복호화하거나 평문 임시파일을 만들 필요가 없다. 다음 보안 조건이 필요하다.

1. magic이 `OVENC001`인데 복호화 실패하면 plaintext fallback 없이 기동을 중단한다.
2. 복호화 대상은 두 DB basename과 승인된 oVirt 경로로 제한한다.
3. systemd credential을 Engine launcher 프로세스에 전달한다.
4. 복호화 byte buffer와 passphrase 참조를 파싱 직후 해제한다.
5. Python 3.6과 설치된 `python3-cryptography` 버전에서 AESGCM/PBKDF2 호환 시험을 수행한다.

따라서 DB `.conf`를 읽는 첫 번째 적용 지점으로 적합하다. 단, 동일 설정을 Java가 다시 읽는 경로도 있으므로 이것만 패치해서는 충분하지 않다.

### 6.2 Java `ShellLikeConfd.java`

`backend/manager/modules/uutils/.../ShellLikeConfd.java`는 Engine과 SSO의 local config reader이다. `loadConfig()`이 `.conf` 목록을 만든 뒤 private `loadProperties(File)`가 `FileInputStream`과 UTF-8 `BufferedReader`로 직접 읽는다.

| 검토 항목 | 판정 |
|---|---|
| 두 DB `.conf` 메모리 복호화 적용 | **가능·필수** |
| `internal.properties` 적용 | 이 class의 대상은 아니며 ExtensionsManager runtime 처리로 해결 |

`loadProperties()`의 `FileInputStream` 대신 공용 `EncryptedConfigInputStream.open(file)`을 사용하면 적용 가능하다. 공용 Java reader는 Python과 정확히 같은 big-endian header, PBKDF2-HMAC-SHA-256 600,000회, AES-256-GCM AAD 및 두 단계 tag 검증을 구현해야 한다. Python과 Java에 각각 독립된 포맷 구현을 만들면 drift 위험이 있으므로 다음을 요구한다.

- 동일한 고정 test vector를 Python·Java 양쪽에서 검증
- header version과 PBKDF2 횟수가 다르면 fail closed
- Java `PBKDF2WithHmacSHA256`의 passphrase encoding을 Python UTF-8과 일치시킴
- `CREDENTIALS_DIRECTORY` credential 우선순위를 Python과 동일하게 유지
- `SENSITIVE_KEYS` 마스킹 이전에 평문 값을 로그로 출력하지 않음

Python launcher와 Java `ShellLikeConfd`를 함께 패치해야 Engine/SSO의 DB 설정 읽기가 일관된다.

### 6.3 Java `ExtensionsManager.java`

`ExtensionsManager.load(File)`은 extension descriptor(`*-authn.properties`, `*-authz.properties` 등)를 `Properties.load()`로 읽고 extension을 생성한다. 이 파일은 `internal.properties` 자체가 아니라 다음과 같은 **참조 경로**를 담는다.

```properties
config.datasource.file=/etc/ovirt-engine/aaa/internal.properties
```

`ExtensionsManager.loadImpl()`은 `config.datasource.file`을 확인하고 대상이 `OVENC001`이면 공용 reader로 복호화하여 권한 0600 runtime 파일을 만든 뒤 property 경로를 교체한다. 따라서 외부 AAA JDBC extension은 기존 파일 API를 그대로 사용하면서 복호화된 설정을 정상적으로 읽는다.

이 class에서 datasource를 미리 제자리 복호화하거나 임시파일 경로로 바꾸는 것도 권장하지 않는다. extension별 속성 의미를 manager가 알아야 하고, 임시파일 수명·재로드·오류 cleanup 문제가 생기기 때문이다. Manager에는 descriptor 암호화가 별도 요구사항일 때만 공용 reader를 적용한다.

### 6.4 Java `Configuration.java`

`extensions-manager`의 `Configuration.loadFile(File)`은 공용 `EncryptedConfigFile.open()`으로 전달받은 plaintext 또는 `OVENC001` properties를 읽어 `Root` configuration을 만든다.

따라서 다음과 같이 판정한다.

- 일반 extension-manager configuration 암호화: **적용 가능**
- 현재 `internal.properties` 자동 복호화: **ExtensionsManager runtime 경로 교체로 보장**

단순히 `Configuration.loadFile()`을 패치한 뒤 `internal.properties`가 보호됐다고 판정해서는 안 된다. 실제 AAA JDBC extension 소스 또는 runtime tracing으로 datasource open 지점을 찾아 그 지점에 공용 reader 또는 credential provider를 적용해야 한다.

### 6.5 권장 패치 순서

1. 암호 포맷의 단일 specification과 Python↔Java 고정 test vector를 추가한다.
2. `configfile.py`에 승인된 DB `.conf`의 메모리 복호화를 추가한다.
3. uutils에 공용 Java GCM reader를 만들고 `ShellLikeConfd.loadProperties()`에 적용한다.
4. `ovirt-engine.service`에 encrypted credential을 제공하고 잘못된 key에서 fail closed인지 확인한다.
5. AAA JDBC extension의 실제 datasource open 지점을 확인해 `internal.properties`를 메모리 복호화하거나 password credential provider로 대체한다.
6. 필요한 경우에만 `ExtensionsManager`와 `Configuration`의 extension descriptor reader를 공용 reader로 교체한다.
7. Engine, SSO, `admin@internal`, setup, backup 및 extension reload 통합시험 후 일괄 암호화 acknowledgement를 허용한다.

### 6.6 종합 결론

| 소스 | 보안 패치 적용 가능성 | 해결되는 파일 |
|---|---|---|
| Python `configfile.py` | 가능·권장 | 두 DB `.conf` |
| Java `ShellLikeConfd.java` | 가능·필수 | 두 DB `.conf` |
| Java `ExtensionsManager.java` | 적용 완료 | `internal.properties` runtime 파일 제공 |
| Java `Configuration.java` | 적용 완료 | 암호화 properties 일반 읽기 |

결론적으로 두 DB 접속암호 파일에는 Python과 Java 메모리 reader가 적용되었다. `internal.properties`는 ExtensionsManager가 권한 0600 runtime 파일로 materialize하여 외부 AAA JDBC extension에 전달한다. 운영 전에는 systemd credential과 Engine/SSO/AAA 통합시험을 완료해야 한다.
