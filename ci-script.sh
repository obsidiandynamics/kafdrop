#!/bin/bash

if [ -z $REG_USER ]; then echo "REG_USER not set"; exit 1; fi
if [ -z $REG_PASS ]; then echo "REG_PASS not set"; exit 1; fi
if [ -z $GITHUB_USER ]; then echo "GITHUB_USER not set"; exit 1; fi
if [ -z $GITHUB_PASS ]; then echo "GITHUB_PASS not set"; exit 1; fi

set -e
docker login -u $REG_USER -p $REG_PASS registry.hub.docker.com

set -x
app_ver=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo Kafdrop version $app_ver
mvn clean integration-test package assembly:single docker:build
docker push obsidiandynamics/kafdrop:$app_ver

repo_url=https://api.github.com/repos/obsidiandynamics/kafdrop
if [[ ! $app_ver =~ "-SNAPSHOT" ]]; then
  echo "Release version"
  docker push obsidiandynamics/kafdrop:latest
  get_release_tag=$(curl -s -o /dev/null -w "%{http_code}" $repo_url/tags/$app_ver)
  echo "Release tag $get_release_tag"
  if [ $get_release_tag == "404" ]; then
    echo "Publishing release"
    release_json="{ \
      \"tag_name\": \"$app_ver\", \
      \"target_commitish\": \"master\", \
      \"name\": \"$app_ver\", \
      \"body\": \"Release $app_ver\", \
      \"draft\": false, \
      \"prerelease\": false \
    }"
    set +x
    curl -u $GITHUB_USER:$GITHUB_PASS -X POST $repo_url/releases -d "$release_json"
    set -x
  else
    echo "Release already exists; skipping"
  fi
else
  echo "Snapshot version"
fi