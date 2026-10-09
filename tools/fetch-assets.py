#!/usr/bin/env python3
"""Fill ForgeGradle 2.1's asset cache via HTTPS, preserving Mojang's SHA-1/size checks.

Run setupDecompWorkspace getAssetIndex -x getAssets first to obtain the official version
metadata and asset index. No Minecraft assets are stored in the repository.
"""
import argparse
import concurrent.futures
import hashlib
import json
import os
from pathlib import Path
import urllib.request


def digest(path):
    with path.open('rb') as stream:
        return hashlib.sha1(stream.read()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--gradle-user-home', type=Path,
                        default=Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle'))))
    args = parser.parse_args()
    cache = args.gradle_user_home / 'caches/minecraft'
    version = json.loads((cache / 'versionJsons/1.8.9.json').read_text())
    metadata = version['assetIndex']
    index = cache / 'assets/indexes' / (metadata['id'] + '.json')
    if digest(index) != metadata['sha1']:
        raise RuntimeError('Asset index checksum mismatch; regenerate it with ForgeGradle')
    entries = json.loads(index.read_text())['objects']
    unique = {entry['hash']: entry['size'] for entry in entries.values()}

    def fetch(item):
        expected, size = item
        destination = cache / 'assets/objects' / expected[:2] / expected
        if destination.is_file() and destination.stat().st_size == size and digest(destination) == expected:
            return
        destination.parent.mkdir(parents=True, exist_ok=True)
        # urllib uses system CA trust and standard HTTPS_PROXY automatically.
        url = 'https://resources.download.minecraft.net/' + expected[:2] + '/' + expected
        with urllib.request.urlopen(url, timeout=60) as response:
            content = response.read(size + 1)
        if len(content) != size or hashlib.sha1(content).hexdigest() != expected:
            raise RuntimeError('Asset checksum/size mismatch: ' + expected)
        temporary = destination.with_suffix('.https-part')
        temporary.write_bytes(content)
        temporary.replace(destination)

    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        list(pool.map(fetch, unique.items()))
    print('Verified {} unique Minecraft assets via HTTPS.'.format(len(unique)))


if __name__ == '__main__':
    main()
