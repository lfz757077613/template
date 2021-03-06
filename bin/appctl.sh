#!/bin/sh

usage ()
{
    echo "Usage: $0 {start|stop|online|offline|restart}"
    exit 1
}

export APP_NAME=template
export SERVER_PORT=8080
export APP_HOME=${HOME}/${APP_NAME}
export JAVA=java
export LOG_PATH=${HOME}/logs
export HEALTH_CHECK_PATH=healthCheck
export HEALTH_CHECK_FILE=healthCheck.tmp

mkdir -p ${LOG_PATH}

start ()
{
  # java -XX:+PrintFlagsInitial 可以看jvm默认参数
    SERVICE_OPTS="-server -jar -Xms8g -Xmx8g -XX:MaxDirectMemorySize=10g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -XX:+AlwaysPreTouch -XX:-UseBiasedLocking"
    SERVICE_OPTS="${SERVICE_OPTS} -XX:+UseG1GC -XX:+DisableExplicitGC -Xloggc:${LOG_PATH}/gc_%p_%t.log"
    SERVICE_OPTS="${SERVICE_OPTS} -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCApplicationStoppedTime -XX:+PrintAdaptiveSizePolicy"
    SERVICE_OPTS="${SERVICE_OPTS} -XX:+PrintReferenceGC -XX:+PrintHeapAtGC -XX:+PrintTenuringDistribution"
    SERVICE_OPTS="${SERVICE_OPTS} -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=30m"
    SERVICE_OPTS="${SERVICE_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_PATH}/java.hprof"
    SERVICE_OPTS="${SERVICE_OPTS} -XX:-OmitStackTraceInFastThrow"
    online
    nohup ${JAVA} ${SERVICE_OPTS} ${APP_HOME}/${APP_NAME}.jar > ${LOG_PATH}/nohup_stdout.log 2>&1 &
    for e in $(seq 90); do
        echo "check http://127.0.0.1:${SERVER_PORT}/${HEALTH_CHECK_PATH} ${e}"
        sleep 1
        status_code=`/usr/bin/curl -L -o /dev/null --connect-timeout 1 -s -w %{http_code} "http://127.0.0.1:${SERVER_PORT}/${HEALTH_CHECK_PATH}"`
        if [ ${status_code} == 200 ];then
            echo "check http://127.0.0.1:${SERVER_PORT}/${HEALTH_CHECK_PATH} success"
            return 0
        fi
    done
    echo "check http://127.0.0.1:${SERVER_PORT}/${HEALTH_CHECK_PATH} fail"
    return 1 # status.taobao check failed
}

stop()
{
    offline
    for e in $(seq 20); do
        echo "wait offline ${e} ..."
        sleep 1
    done

    STR=`ps -ef | grep java | grep "$APP_NAME"`
    [ -z "$STR" ] && return 0
    kill `echo "$STR" | awk '{print $2}'`
    while true
    do
        STR=`ps -ef | grep java | grep "$APP_NAME"`
        [ -z "$STR" ] && break || sleep 1
        echo "wait kill ..."
    done
}

online()
{
    touch -m $APP_HOME/${HEALTH_CHECK_FILE}
}

offline()
{
    rm -f $APP_HOME/${HEALTH_CHECK_FILE}
}

case $1 in

    start)
        start
    ;;

    stop)
        stop
    ;;

    restart)
        stop
        start
    ;;

    online)
        online
    ;;

    offline)
        offline
    ;;

    *)
        usage
    ;;

esac
