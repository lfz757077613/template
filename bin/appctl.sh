#!/bin/sh

usage ()
{
    echo "Usage: $0 {start|stop|online|offline|restart}"
    exit 1
}

export APPNAME=${project.build.finalName}
export STATUS_PORT=8080
export APP_HOME=${HOME}/${APPNAME}
export JAVA=java
export LOGPATH=${HOME}/logs

mkdir -p LOGPATH

start ()
{
    SERVICE_OPTS="-server -jar -Xms8g -Xmx8g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -XX:+AlwaysPreTouch"
    SERVICE_OPTS="${SERVICE_OPTS} -XX:+UseG1GC -XX:+DisableExplicitGC"
    SERVICE_OPTS="${SERVICE_OPTS} -Xloggc:${LOGPATH}/gc_%p.log -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCApplicationStoppedTime -XX:+PrintAdaptiveSizePolicy"
    SERVICE_OPTS="${SERVICE_OPTS} -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=30m"
    SERVICE_OPTS="${SERVICE_OPTS} -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOGPATH}/java.hprof"
    SERVICE_OPTS="${SERVICE_OPTS} -XX:-OmitStackTraceInFastThrow"
    online
    nohup ${JAVA} ${SERVICE_OPTS} ${APP_HOME}/${APPNAME}.jar > ${LOGPATH}/nohup_stdout.log 2>&1 &
    for e in $(seq 90); do
        echo "check http://127.0.0.1:${STATUS_PORT}/healthCheck ${e}"
        sleep 1
        status_code=`/usr/bin/curl -L -o /dev/null --connect-timeout 1 -s -w %{http_code}  "http://127.0.0.1:${STATUS_PORT}/healthCheck"`
        if [ ${status_code} == 200 ];then
            echo "check http://127.0.0.1:${STATUS_PORT}/healthCheck success"
            return 0
        fi
    done
    echo "check http://127.0.0.1:${STATUS_PORT}/healthCheck fail"
    return 1 # status.taobao check failed
}

stop()
{
    offline
    for e in $(seq 20); do
        echo "wait offline ${e} ..."
        sleep 1
    done

    STR=`ps -ef | grep java | grep "$APPNAME"`
    [ -z "$STR" ] && return 0
    kill `ps -ef | grep java | grep "$APPNAME" | awk '{print $2}'`
    while true
    do
        STR=`ps -ef | grep java | grep "$APPNAME"`
        [ -z "$STR" ] && break || sleep 1
        echo "wait kill ..."
    done
}

online()
{
    touch -m $APP_HOME/healthCheck
}

offline()
{
    rm -f $APP_HOME/healthCheck
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
