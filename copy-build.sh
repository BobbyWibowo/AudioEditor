#!/usr/bin/env bash

TARGET=audioeditor_jar.zip

cd out/artifacts/audioeditor_jar
if [ -f "$TARGET" ]; then
  rm -f "$TARGET"
fi
zip -r "$TARGET" .
mv "$TARGET" ../../../

