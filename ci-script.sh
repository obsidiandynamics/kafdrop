#!/bin/bash

if [ -z $REG_USER ]; then echo "REG_USER not set"; exit 1; fi
if [ -z $REG_PASS ]; then echo "REG_PASS not set"; exit 1; fi
if [ -z $GITHUB_USER ]; then echo "GITHUB_USER not set"; exit 1; fi
if [ -z $GITHUB_PASS ]; then echo "GITHUB_PASS not set"; exit 1; fi
if [ -z $BINTRAY_USER ]; then echo "BINTRAY_USER not set"; exit 1; fi
if [ -z $BINTRAY_KEY ]; then echo "BINTRAY_KEY not set"; exit 1; fi

DOCKER_PUSH_ENABLED=1
BINTRAY_UPLOAD_ENABLED=1
GITHUB_RELEASE_ENABLED=1

set -e
if [ $DOCKER_PUSH_ENABLED = 1 ]; then
  echo "$REG_PASS" | docker login -u $REG_USER --password-stdin
fi

set -x
app_ver=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo Kafdrop version $app_ver
mvn clean integration-test package assembly:single docker:build
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
    get_release_tag=$(curl -s -o /dev/null -w "%{http_code}" $repo_url/releases/tag/$app_ver)
    echo "Release tag $get_release_tag"
    if [ $get_release_tag == "404" ]; then
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
      set -x
    elif [ $get_release_tag == "200" ]; then
      echo "Release already exists; skipping"
    else
      echo "Unexpected error checking release status: $get_release_tag"
      exit 1
    fi
  fi

  if [ $BINTRAY_UPLOAD_ENABLED = 1 ]; then
    set +x
    curl -X DELETE https://api.bintray.com/packages/obsidiandynamics/kafdrop/main/versions/$app_ver -u ${BINTRAY_USER}:${BINTRAY_KEY} || echo Skipped 'delete'
    cat settings.xml.template | sed "s/{{BINTRAY_USER}}/${BINTRAY_USER}/g" | sed "s/{{BINTRAY_KEY}}/${BINTRAY_KEY}/g" > settings.xml
    set -x
    mvn deploy -s settings.xml
  fi
else
  echo "Snapshot version"
fi