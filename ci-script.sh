#!/bin/bash

if [ "$TRAVIS_PULL_REQUEST" = "false" ]; then
  # only require these variables if building off the main repo
  if [ -z $REG_USER ]; then echo "REG_USER not set"; exit 1; fi
  if [ -z $REG_PASS ]; then echo "REG_PASS not set"; exit 1; fi
  if [ -z $GITHUB_USER ]; then echo "GITHUB_USER not set"; exit 1; fi
  if [ -z $GITHUB_PASS ]; then echo "GITHUB_PASS not set"; exit 1; fi
  if [ -z $BINTRAY_USER ]; then echo "BINTRAY_USER not set"; exit 1; fi
  if [ -z $BINTRAY_KEY ]; then echo "BINTRAY_KEY not set"; exit 1; fi
fi

DOCKER_PUSH_ENABLED=1
BINTRAY_UPLOAD_ENABLED=1
GITHUB_RELEASE_ENABLED=1

set -e
if [ "$TRAVIS_PULL_REQUEST" = "false" -a $DOCKER_PUSH_ENABLED = 1 ]; then
  echo "$REG_PASS" | docker login -u $REG_USER --password-stdin
fi

set -x
app_ver=$(mvn -B help:evaluate -Dexpression=project.version -q -DforceStdout)
echo Kafdrop version $app_ver
mvn -B clean integration-test package assembly:single docker:build

if [ "$TRAVIS_PULL_REQUEST" = "false" ]; then
  if [ $DOCKER_PUSH_ENABLED = 1 ]; then 
    docker push docker.io/obsidiandynamics/kafdrop:$app_ver
  fi

  repo_url=https://api.github.com/repos/obsidiandynamics/kafdrop
  if [[ ! $app_ver =~ "-SNAPSHOT" ]]; then
    echo "Release version"
    if [ $DOCKER_PUSH_ENABLED = 1 ]; then 
      docker push docker.io/obsidiandynamics/kafdrop:latest
    fi

    if [ $GITHUB_RELEASE_ENABLED = 1 ]; then
      set +x
      get_release_status=$(curl -u $GITHUB_USER:$GITHUB_PASS -s -o /dev/null -w "%{http_code}" $repo_url/releases/tags/$app_ver)
      set -x
      echo "Release tag query status code: $get_release_status"
      if [ $get_release_status == "404" ]; then
        echo "Publishing release"
        release_json="{ \
          \"tag_name\": \"$app_ver\", \
          \"target_commitish\": \"master\", \
          \"name\": \"$app_ver\", \
          \"body\": \"Download from [Bintray](https://bintray.com/obsidiandynamics/kafdrop/download_file?file_path=com%2Fobsidiandynamics%2Fkafdrop%2Fkafdrop%2F${app_ver}%2Fkafdrop-${app_ver}.jar)\", \
          \"draft\": false, \
          \"prerelease\": false \
        }"
        set +x
        curl -u $GITHUB_USER:$GITHUB_PASS -X POST $repo_url/releases -d "$release_json"
      elif [ $get_release_status == "200" ]; then
        echo "Release already exists; skipping"
      else
        echo "Unexpected error checking release status: $get_release_status"
        exit 1
      fi
    fi

    if [ $BINTRAY_UPLOAD_ENABLED = 1 ]; then
      set +x
      curl -X DELETE https://api.bintray.com/packages/obsidiandynamics/kafdrop/main/versions/$app_ver -u ${BINTRAY_USER}:${BINTRAY_KEY} || echo Skipped 'delete'
      cat settings.xml.template | sed "s/{{BINTRAY_USER}}/${BINTRAY_USER}/g" | sed "s/{{BINTRAY_KEY}}/${BINTRAY_KEY}/g" > settings.xml
      set -x
      mvn -B deploy -s settings.xml
    fi
  else
    echo "Snapshot version"
  fi
else
  echo "Pull request detected; reverting to a minimal build"
fi