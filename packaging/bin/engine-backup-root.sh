#!/bin/sh

set -eu

DEST_DIR="${1:-}"

if [ "$#" -ne 1 ]; then
    echo "FAIL: 저장 위치 인자는 하나만 허용됩니다." 1>&2
    exit 1
fi

if [ "$(id -u)" -ne 0 ]; then
    echo "FAIL: 엔진 백업은 root 권한으로 실행해야 합니다." 1>&2
    exit 1
fi

if [ -z "${DEST_DIR}" ]; then
    echo "FAIL: 저장 위치가 필요합니다." 1>&2
    exit 1
fi

case "${DEST_DIR}" in
    /*) ;;
    *)
        echo "FAIL: 저장 위치는 절대 경로여야 합니다." 1>&2
        exit 1
        ;;
esac

if [ "${DEST_DIR}" = "/" ]; then
    echo "FAIL: 루트 디렉터리는 저장 위치로 사용할 수 없습니다." 1>&2
    exit 1
fi

umask 077
mkdir -p -- "${DEST_DIR}"
resolved_dir="$(readlink -f -- "${DEST_DIR}")"
[ "${resolved_dir}" != "/" ] || {
    echo "FAIL: 루트 디렉터리는 저장 위치로 사용할 수 없습니다." 1>&2
    exit 1
}

work_dir="$(mktemp -d "${resolved_dir%/}/.engine-backup.XXXXXX")"
trap 'rm -rf -- "${work_dir}"' EXIT HUP INT TERM

/usr/bin/engine-backup \
    --mode=backup \
    --file="${work_dir}/engine_backup.tar.gz" \
    --log="${work_dir}/engine_backup.log"

mv -f -- "${work_dir}/engine_backup.tar.gz" "${resolved_dir}/engine_backup.tar.gz"
mv -f -- "${work_dir}/engine_backup.log" "${resolved_dir}/engine_backup.log"
trap - EXIT HUP INT TERM
rmdir -- "${work_dir}"
