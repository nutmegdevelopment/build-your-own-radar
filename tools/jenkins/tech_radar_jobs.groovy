job("service/architecture/tech-radar-build-and-deploy") {

    displayName('nm-tech-radar-build-and-deploy')
    description("<img src=\"buildTimeGraph/png\"/>")
    logRotator(-1, -1, 1, 10)
    wrappers {
        timestamps()
        colorizeOutput()
        preBuildCleanup()
    }

    label('k8s-docker')

    parameters {
        stringParam('BRANCH', 'master', 'Git branch')
        stringParam('DEPLOY_VERSION','0.0.1','Deployment version')
        stringParam('DEPLOY_ENVIRONMENT','dev','Deployment environment')
        stringParam('MARATHON_URL','${MARATHON_DEV}','Marathon URL')
        stringParam('APP_ID','','Application ID')
        stringParam('INSTANCES','1','Application intances')
        stringParam('PROPERTIES','','Service properties')
        stringParam('APP_NAME', 'tech-radar', 'Application name')
        stringParam('DOCKER_REPO', 'build-your-own-radar', 'Docker repo name')
        stringParam('DOCKER_REGISTRY', 'registry.nutmeg.co.uk:8443', 'Docker registry address')
    }

    wrappers {
        deliveryPipelineVersion('\${DEPLOY_VERSION}', true)
    }

    scm {
        git {
            branch('${BRANCH}')
            remote {
                credentials('cc1d4123-c18e-403b-b790-1e072cd0583d')
                url('git@github.com:nutmegdevelopment/build-your-own-radar.git')
            }
            extensions {
                cleanBeforeCheckout()
            }
        }
    }

    triggers {
      githubPush()
    }

    steps {
      shell("""
        git tag -a -f -m "Release \${DEPLOY_VERSION}" \${DEPLOY_VERSION}

        echo "Build the docker image(s)."
        docker build -f Dockerfile -t \${DOCKER_REGISTRY}/\${DOCKER_REPO}:\${DEPLOY_VERSION} .
        docker tag \${DOCKER_REGISTRY}/\${DOCKER_REPO}:\${DEPLOY_VERSION} \${DOCKER_REGISTRY}/\${DOCKER_REPO}:latest

        echo "Push the docker images."
        docker push \${DOCKER_REGISTRY}/\${DOCKER_REPO}:\${DEPLOY_VERSION}
        docker push \${DOCKER_REGISTRY}/\${DOCKER_REPO}:latest

        marathon-config-generator -config-file=tools/marathon/marathon.yml \
          -var=APP_NAME=\${APP_NAME} \
          -var=VERSION=\${DEPLOY_VERSION} | marathon-client -d -m \${MARATHON_URL} -u marathon -p NutmegMarathon -f -
        """)
    }
}
