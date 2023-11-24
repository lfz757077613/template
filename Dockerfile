FROM xxx:base

USER admin
WORKDIR /home/admin

COPY target/ops-assembly.tgz /home/admin/ops-assembly.tgz

RUN tar -zxvf /home/admin/ops-assembly.tgz -C /home/admin

ENTRYPOINT ["/home/admin/ops/appctl.sh", "start_k8s"]
