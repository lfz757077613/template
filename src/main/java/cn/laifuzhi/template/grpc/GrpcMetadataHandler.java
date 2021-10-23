package cn.laifuzhi.template.grpc;

import cn.laifuzhi.template.grpc.proto.MetadataHandlerGrpc;
import cn.laifuzhi.template.grpc.proto.MetadataResp;
import cn.laifuzhi.template.grpc.proto.QueryBrokerReq;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GrpcMetadataHandler extends MetadataHandlerGrpc.MetadataHandlerImplBase {
    @Override
    public void queryBroker(QueryBrokerReq request, StreamObserver<MetadataResp> responseObserver) {

    }
}
