# docker run -itd --name mysql-test -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.0.26
# docker run --name some-redis -p6379:6379 -d redis:6.2.4 redis-server --appendonly yes
# docker run -itd -p 12379:2379 --name etcd -e ALLOW_NONE_AUTHENTICATION=yes bitnami/etcd:3.5.0
# docker run -itd -p80:80 -p443:443 axizdkr/tengine:2.3.2

# nginx默认和客户端保持长连，和upstream的服务器通过http1.0请求，每次请求都新建连接
# tengine和upstream保持长连接配置需要指定proxy_http_version为1.1(默认1.0)，删除Connection请求头(默认close) https://nginx.org/en/docs/http/ngx_http_upstream_module.html#keepalive
# tengine2.3.3 configure 时需要加参数--add-module=./modules/ngx_http_upstream_check_module/ (tengine官方文档有误，这种方式报错./configure --with-http_upstream_check_module)
# 文档说默认开启http_check模块，实际还是需要制指定开启。否则会报nginx: emerg unknown directive "check" (https://github.com/alibaba/tengine/issues/1394)
# 这样的话通过http1.1默认就是keep-alive了(即使没有Connection请求头)，抓包可以确定请求会复用连接，Connection也可配置成keep-alive
# tengine和客户端、upstream都是长连接之后，收到同一个客户端的同一个链接的请求会负载均衡转发到upstream
# 所以通过tengine的http_check模块可以做到http服务无损发布，实际上这里的tengine起到了注册中心的作用(先摘流量，再等待正在处理的请求处理完毕)
# https://tengine.taobao.org/document_cn/ngx_log_pipe_cn.html 异步打印日志及自动回滚功能
error_log  "pipe:rollback logs/error_log interval=60m baknum=5 maxsize=2048M" info;
# 与cpu数量相同的worker进程
worker_processes  auto;
# 优雅关机超时
worker_shutdown_timeout 1m;
# nginx进程最大打开文件句柄数
worker_rlimit_nofile        100000;
events {
    # 单个进程允许的最大连接数，包括前端和后端的全部链接，不能超过worker_rlimit_nofile
    worker_connections  20480;
    # 不用指定，nginx会选择效率最高的https://nginx.org/en/docs/ngx_core_module.html#use
    # use epoll;
    # 处理新连接是否有惊群效应，之前默认on，现在默认off，访问量小用on，访问量大用off https://blog.huoding.com/2013/08/24/281
    accept_mutex off;
}
http {   
    # 零拷贝，对静态资源有性能提升，sendfile、tcp_nopush、tcp_nodelay详解：https://imququ.com/post/my-nginx-conf-for-wpo.html
    sendfile        on;
    # 默认off，
    tcp_nopush      on;
    # 默认on
    tcp_nodelay     on;
	log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';
	access_log  "pipe:rollback logs/access_log interval=1h baknum=5 maxsize=2G"  main;
	# 默认长连接发送1000次请求就断开
	keepalive_requests 1000
	# 默认长连接保持1小时
	keepalive_time 1h 
	# 默认长连接空闲75秒
	keepalive_timeout 75s 
	# 默认向前端发送超时60秒，不代表完整http请求，代表两个连续的写操作超时(一个http请求拆成多个tcp包)
	send_timeout 60s；
	# 默认读取前端请求头60秒超时
	client_header_timeout 60s;
	# 默认读取前端请求体60秒超时，不代表完整http请求，代表两个连续的读操作超时(一个http响应拆成多个tcp包)
	client_body_timeout 60s
	# 默认前端请求体1m，0代表无限
	client_max_body_size 1m；
	# 默认变量哈希表的最大1024
    variables_hash_max_size     1024;
    # 默认变量哈希表的桶大小64
    variables_hash_bucket_size  64;
    # 默认服务名哈希表大小512
    server_names_hash_max_size 4096
    # 默认服务名哈希表的桶大小32|64|128
    server_names_hash_bucket_size 4096;
	# 默认连接超时60秒
	proxy_connect_timeout 60s
	# 默认向后端服务器发送超时60秒，不代表完整http请求，代表两个连续的写操作超时(一个http请求拆成多个tcp包)
	proxy_send_timeout 60s
	# 默认后端服务器响应超时60秒，不代表完整http请求，代表两个连续的读操作超时(一个http响应拆成多个tcp包)
	proxy_read_timeout 60s
	# 默认转发不重试
	proxy_next_upstream_tries 0;
	# 默认转发不限制时间
	proxy_next_upstream_timeout 0;
	proxy_redirect          off;
	# host和http_host区别：https://segmentfault.com/a/1190000019422246
	proxy_set_header        Host $http_host;
	proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header        X-Real-IP $remote_addr;
    server {
        listen       80;
        server_name  localhost;
        location / {
            proxy_pass   http://self;
            proxy_http_version 1.1;
            # 删除请求头
            proxy_set_header Connection "";
        }
        location /status {
            stub_status on;
            access_log off;
        }
        # 查看upstream check状态
        location /check_status {
            check_status;
            access_log off;
        }
    }
    upstream self {
        # mac docker desktop独有的宿主机域名
        # max_fails默认1，fail_timeout默认10秒，代表默认有一次失败就屏蔽10秒
        server docker.for.mac.host.internal:8080 max_fails=1 fail_timeout=10s; 
        check interval=1000 rise=1 fall=1 timeout=1000 type=http;
        check_http_send "GET /template/healthCheck HTTP/1.0\r\n\r\n";
        check_http_expect_alive http_2xx;
        # 最大空闲连接数 https://nginx.org/en/docs/http/ngx_http_upstream_module.html#keepalive
        keepalive 16;
        # 默认长连接发送1000次请求就断开
        keepalive_requests 1000
        # 默认长连接保持1小时
        keepalive_time 1h 
        # 默认长连接空闲75秒，要比tomcat keep-alive-timeout设置的小
        keepalive_timeout 20s 
    }
}
