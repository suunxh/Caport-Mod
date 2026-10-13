#!/usr/bin/env bash
# Run after a successful build. Authentication is supplied by GitHub Actions.
set -euo pipefail

: "${GH_REPO:?Set GH_REPO to owner/repository}"
: "${BUILD_COMMIT:?Set BUILD_COMMIT to the built commit SHA}"
: "${BUILD_NUMBER:?Set BUILD_NUMBER to the workflow run number}"
: "${BUILD_ATTEMPT:?Set BUILD_ATTEMPT to the workflow run attempt}"
: "${RELEASE_PLATFORM:?Set RELEASE_PLATFORM to forge-1.8.9 or fabric-26.3}"
case "$RELEASE_PLATFORM" in
  forge-1.8.9) platform_title="Forge 1.8.9"; latest=true ;;
  fabric-26.3) platform_title="Fabric 26.3"; latest=false ;;
  *) printf 'Unknown release platform.\n' >&2; exit 1 ;;
esac
jar_directory=${JAR_DIRECTORY:-build/libs}

mapfile -t jars < <(find "$jar_directory" -maxdepth 1 -type f -name "caport-${RELEASE_PLATFORM}-*.jar" \
  ! -name '*-sources.jar' ! -name '*-dev.jar' | sort)
if [ "${#jars[@]}" -ne 1 ]; then
  printf 'Expected one installable JAR, found %s.\n' "${#jars[@]}" >&2
  exit 1
fi
jar=${jars[0]}
filename=$(basename "$jar")
version=${filename#caport-${RELEASE_PLATFORM}-}
version=${version%.jar}

# Run numbers prevent distinct builds from overwriting the same version release.
# A retry of a run gets its own tag as well. Version tags use their explicit name.
if [ "${BUILD_REF_TYPE:-branch}" = tag ]; then
  tag="${BUILD_REF_NAME:?Missing version tag}-${RELEASE_PLATFORM}"
else
  tag="build-${BUILD_NUMBER}-${BUILD_ATTEMPT}-${RELEASE_PLATFORM}"
fi

release_dir=$(mktemp -d)
trap 'rm -rf "$release_dir"' EXIT
checksum="$release_dir/${filename}.sha256"
(cd "$jar_directory" && sha256sum "$filename") > "$checksum"

# Include only changes since the most recent published release in this commit's
# history. The workflow checks out full history and tags. A newer release from
# another branch/run must not hide changes in the commit being published.
published_tags=$(gh release list --repo "$GH_REPO" --exclude-drafts --limit 100 \
  --json tagName --jq '.[].tagName')
previous_tag=""
while IFS= read -r candidate; do
  case "$candidate" in
    *-forge-1.8.9|*-fabric-26.3) [[ "$candidate" == *"-${RELEASE_PLATFORM}" ]] || continue ;;
  esac
  if [ -n "$candidate" ] && git merge-base --is-ancestor "refs/tags/$candidate" "$BUILD_COMMIT" 2>/dev/null; then
    previous_tag=$candidate
    break
  fi
done <<< "$published_tags"

if [ -n "$previous_tag" ]; then
  git log --reverse --no-merges --format='- %s (%h)' \
    "refs/tags/${previous_tag}..${BUILD_COMMIT}" > "$release_dir/notes.md"
else
  git log --reverse --no-merges --format='- %s (%h)' "$BUILD_COMMIT" > "$release_dir/notes.md"
fi
if [ ! -s "$release_dir/notes.md" ]; then
  printf '%s\n' "- Rebuilt commit ${BUILD_COMMIT:0:7}; no changes since the previous release." > "$release_dir/notes.md"
fi

# A failure to find/read/create a release is fatal. Never overwrite existing
# release assets or create a release after a failed build.
gh release create "$tag" "$jar" "$checksum" \
  --repo "$GH_REPO" \
  --target "$BUILD_COMMIT" \
  --title "caport ${version} — ${platform_title} (${tag})" \
  --notes-file "$release_dir/notes.md" \
  --latest="$latest"
