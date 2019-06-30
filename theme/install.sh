#!/bin/bash
set -e

cd $(dirname $0)

which npm > /dev/null
if [ $? -ne 0 ]; then
  echo "ERROR: npm not installed"
  exit 1
fi

which grunt > /dev/null || npm install -g grunt-cli

cd ..
mkdir -p target
cd target
if [ -d bootswatch ]; then
  cd bootswatch
  git pull
else
  git clone https://github.com/thomaspark/bootswatch.git
  cd bootswatch
fi

mkdir -p dist/kafdrop
cp ../../theme/*.scss dist/kafdrop

npm install
grunt swatch:kafdrop
theme_target_dir=src/main/resources/static/css
cp dist/kafdrop/bootstrap.min.css ../../$theme_target_dir

echo "Theme installed into $theme_target_dir"