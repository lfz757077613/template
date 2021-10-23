package cn.laifuzhi.template.RocketHttp.reactive;

import cn.laifuzhi.template.RocketHttp.model.RocketResponse;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

import static cn.laifuzhi.template.RocketHttp.RocketClient.FUTURE;

@Slf4j
@AllArgsConstructor
public final class RocketConnectListener implements ChannelFutureListener {
    private final DefaultFullHttpRequest httpRequest;

    /* channel对应的eventloop线程关闭了的话，addListener会抛出RejectedExecutionException异常
     * ERROR io.netty.util.concurrent.DefaultPromise.rejectedExecution - Failed to submit a listener notification task. Event loop shut down?
     * java.util.concurrent.RejectedExecutionException: event executor terminated
	 *     at io.netty.util.concurrent.SingleThreadEventExecutor.reject(SingleThreadEventExecutor.java:926)
	 *     at io.netty.util.concurrent.SingleThreadEventExecutor.offerTask(SingleThreadEventExecutor.java:353)
	 *     at io.netty.util.concurrent.SingleThreadEventExecutor.addTask(SingleThreadEventExecutor.java:346)
	 *     at io.netty.util.concurrent.SingleThreadEventExecutor.execute(SingleThreadEventExecutor.java:828)
	 *     at io.netty.util.concurrent.SingleThreadEventExecutor.execute(SingleThreadEventExecutor.java:818)
	 *     at io.netty.util.concurrent.DefaultPromise.safeExecute(DefaultPromise.java:842)
	 *     at io.netty.util.concurrent.DefaultPromise.notifyListeners(DefaultPromise.java:499)
	 *     at io.netty.util.concurrent.DefaultPromise.addListener(DefaultPromise.java:184)
	 *     at io.netty.channel.DefaultChannelPromise.addListener(DefaultChannelPromise.java:95)
	 *     at io.netty.channel.DefaultChannelPromise.addListener(DefaultChannelPromise.java:30)
	 *
     * channel关闭了继续调用writeAndFlush什么都不会发生，future结果是StacklessClosedChannelException异常
     * 但是因为channel已经关闭，pipeline已经删除，即时addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)，也不会调用自定义的exceptionCaught方法
     * 会调用到DefaultChannelPipeline的末端exceptionCaught自带方法，记录日志
     * WARN io.netty.channel.DefaultChannelPipeline - An exceptionCaught() event was fired, and it reached at the tail of the pipeline. It usually means the last handler in the pipeline did not handle the exception.
     *     io.netty.channel.StacklessClosedChannelException: null
	 *     at io.netty.channel.AbstractChannel$AbstractUnsafe.write(Object, ChannelPromise)(Unknown Source)
     */
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        CompletableFuture<RocketResponse> result = future.channel().attr(FUTURE).get();
        try {
            if (!future.isSuccess()) {
                result.completeExceptionally(future.cause());
                return;
            }
            future.channel().writeAndFlush(httpRequest).addListener(new RocketWriteListener());
        } catch (Throwable e) {
            result.completeExceptionally(e);
        }
    }
}
