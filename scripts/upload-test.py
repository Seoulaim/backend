#!/usr/bin/env python3
"""presigned PUT URL 로 파일을 올려 서명이 맞는지 확인하는 로컬 검증용 스크립트.

사용법: python3 scripts/upload-test.py <uploadUrl> <파일경로> <contentType>
presigned 요청 때 보낸 sizeBytes·contentType 과 파일 크기·인자 값이 같아야 200 이 나온다.
"""

import os
import sys
import urllib.error
import urllib.request


def main() -> int:
    if len(sys.argv) != 4:
        print("usage: upload-test.py <uploadUrl> <filePath> <contentType>", file=sys.stderr)
        return 2
    upload_url, file_path, content_type = sys.argv[1:]

    size = os.path.getsize(file_path)
    with open(file_path, "rb") as file:
        body = file.read()

    request = urllib.request.Request(
        upload_url,
        data=body,
        method="PUT",
        headers={"Content-Type": content_type, "Content-Length": str(size)},
    )
    try:
        with urllib.request.urlopen(request) as response:
            print(f"{response.status} ({size} bytes, {content_type})")
            return 0
    except urllib.error.HTTPError as error:
        print(f"{error.code} ({size} bytes, {content_type})")
        print(error.read().decode("utf-8", errors="replace"))
        return 1


if __name__ == "__main__":
    sys.exit(main())
