# docker run -itd --name mysql-test -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.0.26
# docker run --name some-redis -p6379:6379 -d redis:6.2.4 redis-server --appendonly yes
# docker run -itd -p 12379:2379 --name etcd -e ALLOW_NONE_AUTHENTICATION=yes bitnami/etcd:3.5.0
# docker run -itd -p80:80 -p443:443 axizdkr/tengine:2.3.2

# nginx默认和客户端保持长连，和upstream的服务器通过http1.0请求，每次请求都新建连接
# tengine和upstream保持长连接配置需要指定proxy_http_version为1.1(默认1.0)，删除Connection请求头(默认close)
# tengine2.3.3 configure 时需要加参数--add-module=./modules/ngx_http_upstream_check_module/ (tengine官方文档有误，这种方式报错./configure --with-http_upstream_check_module)
# 文档说默认开启http_check模块，实际还是需要制指定开启。否则会报nginx: emerg unknown directive "check" (https://github.com/alibaba/tengine/issues/1394)
# 这样的话通过http1.1默认就是keep-alive了(即使没有Connection请求头)，抓包可以确定请求会复用连接，Connection也可配置成keep-alive
# upstream中设置keepalive 数字代表最大空闲连接数
# tengine和客户端、upstream都是长连接之后，收到同一个客户端的同一个链接的请求会负载均衡转发到upstream
# 所以通过tengine的http_check模块可以做到http服务无损发布，实际上这里的tengine起到了注册中心的作用(先摘流量，再等待正在处理的请求处理完毕)
http {   
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
        server docker.for.mac.host.internal:8080;
        check interval=1000 rise=1 fall=1 timeout=1000 type=http;
        check_http_send "GET /template/healthCheck HTTP/1.0\r\n\r\n";
        check_http_expect_alive http_2xx;
        keepalive 16;
    }
}
