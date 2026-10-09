#!/usr/bin/env bash
# Run after a successful build. Authentication is supplied by GitHub Actions.
set -euo pipefail

: "${GH_REPO:?Set GH_REPO to owner/repository}"
: "${BUILD_COMMIT:?Set BUILD_COMMIT to the built commit SHA}"
: "${BUILD_NUMBER:?Set BUILD_NUMBER to the workflow run number}"
: "${BUILD_ATTEMPT:?Set BUILD_ATTEMPT to the workflow run attempt}"

mapfile -t jars < <(find build/libs -maxdepth 1 -type f -name '*.jar' \
  ! -name '*-sources.jar' ! -name '*-dev.jar' | sort)
if [ "${#jars[@]}" -ne 1 ]; then
  printf 'Expected one installable JAR, found %s.\n' "${#jars[@]}" >&2
  exit 1
fi
jar=${jars[0]}
filename=$(basename "$jar")
version=${filename#caport-}
version=${version%.jar}

# Run numbers prevent distinct builds from overwriting the same version release.
# A retry of a run gets its own tag as well. Version tags use their explicit name.
if [ "${BUILD_REF_TYPE:-branch}" = tag ]; then
  tag=${BUILD_REF_NAME:?Missing version tag}
else
  tag="build-${BUILD_NUMBER}-${BUILD_ATTEMPT}"
fi

release_dir=$(mktemp -d)
trap 'rm -rf "$release_dir"' EXIT
checksum="$release_dir/${filename}.sha256"
(cd build/libs && sha256sum "$filename") > "$checksum"
cat > "$release_dir/notes.md" <<EOF
Download **${filename}** under Assets and copy it into your Minecraft instance's mods folder.
Remove the old copy when updating.

Requires Minecraft Java 1.8.9, Forge 11.15.1.2318, and Java 8.

- **G:** teleport to the block you aim at.
- **H:** teleport to the nearest suitable pressure plate ahead, within 64 blocks.
- Manual mode: press Enter to send or Escape to cancel.

Commands:

- \`/caport status\` — show the current mode.
- \`/caport manual\` — restore manual confirmation.
- \`/caport direct\` — enable direct mode in singleplayer.
- \`/caport direct authorized\` — enable direct mode on the current private server.
- \`/caport direct hypixel-risk\` — enable direct mode on the current Hypixel host, including Housing.

The server must allow you to use /tp. **Use at your own risk.**
EOF

# A failure to find/read/create a release is fatal. Never overwrite existing
# release assets or create a release after a failed build.
gh release create "$tag" "$jar" "$checksum" \
  --repo "$GH_REPO" \
  --target "$BUILD_COMMIT" \
  --title "caport ${version} (${tag})" \
  --notes-file "$release_dir/notes.md" \
  --latest
