#!/bin/zsh

set -e

if [[ -z "$1" ]]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 1.0.0"
  exit 1
fi

VERSION="$1"
POM="pom.xml"
MAKEFILE="makefile"

echo "🔖 Releasing version $VERSION..."

# Update version in pom.xml
sed -i '' \
  "/<groupId>dev.femrek<\/groupId>/{
    n
    /<artifactId>openapi-dart-network-codegen<\/artifactId>/{
      n
      s|<version>.*</version>|<version>$VERSION</version>|
    }
  }" "$POM"

echo "✅ Updated $POM to version $VERSION"

# Update CUSTOM_CODEGEN_JAR in Makefile to point to the new jar filename
if [[ -f "$MAKEFILE" ]]; then
  sed -i '' "s|^CUSTOM_CODEGEN_JAR := .*|CUSTOM_CODEGEN_JAR := ./target/openapi-dart-network-codegen-$VERSION.jar|" "$MAKEFILE"
  echo "✅ Updated $MAKEFILE to point to openapi-dart-network-codegen-$VERSION.jar"
else
  echo "⚠️ $MAKEFILE not found; skipping makefile update"
fi

# Commit
git add "$POM" "$MAKEFILE"
git commit -m "chore(version): $VERSION"

echo "✅ Created commit 'chore(version): $VERSION'"

# Tag
git tag "v$VERSION"

echo "✅ Created tag v$VERSION"

# Push
git push origin main
git push origin tag "v$VERSION"

echo "🚀 Released v$VERSION successfully!"
