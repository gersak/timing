#!/bin/bash
# Generate API documentation with Codox

set -e

cd "$(dirname "$0")/.."

echo "Generating docs..."
rm -rf docs
clj -X:codox

# GitHub Pages needs this to skip Jekyll processing
touch docs/.nojekyll

# Copy 404 page
cp doc/404.html docs/

echo "Done. Docs generated in docs/"
