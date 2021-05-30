package cn.laifuzhi.template.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import cn.laifuzhi.template.grpc.QueryBrokerReq;
import cn.laifuzhi.template.grpc.MetadataResp;
import io.grpc.stub.StreamObserver;
import cn.laifuzhi.template.grpc.MetadataHandlerGrpc;

@Slf4j
@Component
public class GrpcMetadataHandler extends MetadataHandlerGrpc.MetadataHandlerImplBase {
    @Override
    public void queryBroker(QueryBrokerReq request, StreamObserver<MetadataResp> responseObserver) {

    }
}
