#!/bin/bash
set -e

# =============================================================================
# ⚙️ CẤU HÌNH – chỉ cần chỉnh 2 dòng này
# =============================================================================
IMAGE="trannguyenhan/tuensso"
VERSION="1.1.0"
# =============================================================================

BUILD_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
GIT_COMMIT=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

echo ""
echo "╔══════════════════════════════════════════════╗"
echo "║       TuenSSO – Docker Build                ║"
echo "╚══════════════════════════════════════════════╝"
echo ""
echo "  Image   : ${IMAGE}"
echo "  Version : ${VERSION}"
echo "  Commit  : ${GIT_COMMIT}"
echo "  Date    : ${BUILD_DATE}"
echo ""

# ── Build ─────────────────────────────────────────────────────────────────────
echo "▶ Building image..."
docker build \
  --build-arg BUILD_DATE="${BUILD_DATE}" \
  --build-arg GIT_COMMIT="${GIT_COMMIT}" \
  --build-arg VERSION="${VERSION}" \
  -t "${IMAGE}:${VERSION}" \
  -t "${IMAGE}:latest" \
  .

echo ""
echo "✓ Build thành công: ${IMAGE}:${VERSION}"
echo ""

# ── Push ──────────────────────────────────────────────────────────────────────
read -p "▶ Push lên Docker Hub? [y/N] " CONFIRM
if [[ "${CONFIRM}" =~ ^[Yy]$ ]]; then
  echo "  Pushing ${IMAGE}:${VERSION}..."
  docker push "${IMAGE}:${VERSION}"

  echo "  Pushing ${IMAGE}:latest..."
  docker push "${IMAGE}:latest"

  echo ""
  echo "✓ Push thành công!"
  echo "  https://hub.docker.com/r/${IMAGE}/tags"
else
  echo "  Push bị bỏ qua. Chạy thủ công:"
  echo "    docker push ${IMAGE}:${VERSION}"
  echo "    docker push ${IMAGE}:latest"
fi

echo ""
