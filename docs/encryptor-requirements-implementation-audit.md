# 설정 파일 암호화 보안 요구사항 구현 점검표

## 1. 판정 기준

- **완료:** 현재 소스에 구현과 시험 근거가 있다.
- **부분 완료:** 기능은 있으나 운영 연동 또는 기존 형식 검증이 추가로 필요하다.
- **미구현:** 현재 소스에 요구 동작이 없다.

## 2. 암호화 도구 18개 요구사항

| 번호 | 요구사항 | 판정 | 소스 근거 및 검토 의견 |
|:---:|---|:---:|---|
| 1 | AES-256-CBC에서 AES-256-GCM 전환 | **완료** | `encrypt_bytes()`가 본문과 DEK를 각각 `AESGCM.encrypt()`로 처리한다. 신규 CBC 암호화 경로는 없다. |
| 2 | 변조·잘못된 키·손상 탐지 | **완료** | 두 GCM tag의 `InvalidTag`를 fail closed 오류로 변환하고 출력 전 중단한다. |
| 3 | `OVENC001` magic과 version | **완료** | 8바이트 magic과 version 1이 header에 포함되고 읽을 때 검증된다. |
| 4 | 파일별 중복 암호화 방지 | **완료** | `is_encrypted()`가 magic을 검사하며 단일 변환은 거부하고 bulk는 skip한다. |
| 5 | DEK에 AES-GCM과 PBKDF2 600,000회 | **완료** | PBKDF2-HMAC-SHA-256 600,000회로 256비트 KEK를 유도하고 AES-GCM으로 256비트 DEK를 래핑한다. |
| 6 | MAC 주소 passphrase 제거 | **완료** | key source에 hardware identifier가 없고 systemd/env/secret file만 사용한다. |
| 7 | systemd credential 기본 | **부분 완료** | `$CREDENTIALS_DIRECTORY/ovirt-encryptor-passphrase`를 최우선으로 읽는다. 다만 실제 실행 unit에는 관리자가 `LoadCredentialEncrypted=`를 배포해야 한다. |
| 8 | 환경변수와 0600 secret file | **완료** | `OVIRT_ENCRYPTOR_PASSPHRASE`와 `--secret-file`/`secret_file`을 지원하며 group/other 권한을 거부한다. |
| 9 | config·암호화 파일 원자적 교체 | **완료** | 같은 디렉터리 임시파일, file/directory `fsync()`, `os.replace()`를 사용한다. config가 없으면 0600으로 원자 생성한다. |
| 10 | 기존 owner·mode 보존 | **완료** | 암호화와 기존 config 교체 시 uid/gid/mode를 보존한다. 복호화는 요구사항 14에 따라 mode만 0600으로 강화한다. |
| 11 | config symlink·group/other write 차단 | **완료** | `_load_crypto_config()`가 `lstat()` 기반 regular file 및 쓰기 권한 검사를 수행한다. |
| 12 | 허용 oVirt 경로 밖 `watch_path` 차단 | **완료** | `/etc/ovirt-engine`, `/etc/ovirt-engine-dwh` 이외 resolved path를 거부한다. |
| 13 | 대상 디렉터리 symlink 순회 차단 | **완료** | `os.walk(..., followlinks=False)`와 symlink directory filtering을 함께 사용한다. |
| 14 | 복호화 출력 0600 | **완료** | `transform_file(..., decrypt=True)`는 output mode를 0600으로 강제한다. |
| 15 | 동일 경로·덮어쓰기 검사 | **완료** | resolved source/output을 비교하고 별도 output이 존재하면 `--overwrite` 없이는 거부한다. 제자리 변환은 원자 교체로 지원한다. |
| 16 | 암호화 직후 자체 복호화 | **완료** | 생성한 GCM envelope를 즉시 복호화해 원문과 byte 단위로 비교한 뒤에만 기록한다. |
| 17 | 기존 CBC 복호화 호환 | **부분 완료** | migration-only AES-256-CBC reader와 CBC→GCM 경로가 있다. 실제 운영 CBC의 binary/Base64·IV 형식과 고정 fixture 대조가 필요하다. |
| 18 | `--deny-legacy-cbc` | **완료** | magic이 없는 입력을 즉시 거부하며 test가 차단 동작을 검증한다. |

## 3. DB·AAA 설정 reader 적용 요구사항

| 대상 | 판정 | 현재 상태 |
|---|:---:|---|
| Python `ovirt_engine/configfile.py` | **완료** | `OVENC001`를 탐지해 Python encryptor로 메모리 복호화한 뒤 `StringIO`로 파싱한다. |
| Java `ShellLikeConfd.java` | **완료** | 공용 `EncryptedConfigFile.open()`으로 평문 또는 GCM 내용을 동일하게 읽는다. |
| Java `ExtensionsManager.java` | **완료** | descriptor를 공용 reader로 읽고 암호화된 `config.datasource.file`은 권한 제한 runtime 파일로 materialize한다. |
| Java `Configuration.java` | **완료** | `EncryptedConfigFile.open()`을 사용해 properties를 메모리 복호화한다. |
| `engine-setup` 복호화 | **부분 완료** | CLI 복호화는 호출하지만 setup 종료 후 변경 파일 자동 재암호화가 없다. |
| `engine-backup` 복호화 | **부분 완료** | cleanup에서 원본 암호문을 복원하지만 SIGKILL·전원 장애 시 평문 잔류 대응이 필요하다. |

## 4. 최종 결론

암호화 도구에 대한 18개 요구사항은 16개 완료, 2개 부분 완료로 판정한다. 부분 완료 항목은 systemd unit credential 배포와 실제 legacy CBC fixture 호환이다. setup/backup lifecycle은 별도의 runtime 연동 항목으로 관리한다.

DB와 AAA가 서비스 기동 중 암호문을 읽는 runtime reader가 적용되었다. 운영 활성화 전에는 다음을 완료하고 `encrypt_conf_files.py --acknowledge-runtime-decryption`을 사용한다.

1. Python↔Java 고정 test vector 검증
2. `ovirt-engine.service`에 systemd encrypted credential 배포
3. AAA runtime 임시파일의 0600 권한과 종료 시 삭제 확인
4. Engine/SSO/AAA/setup/backup 통합시험

즉, **파일 암호화 도구와 DB·AAA runtime reader는 구현됐으며 credential 배포와 통합시험 후 활성화할 수 있다.**
