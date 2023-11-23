package cn.laifuzhi.template;

import io.netty.util.internal.PlatformDependent;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

/**
 * 直接使用maven骨架生成jmh工程，无需自己写main方法
 * 如果要在idea中直接运行就需要自己像下面那样自己引入依赖写main方法
 * 生成之后的pom中根据需要修改jmh版本号，使用的java版本和插件版本等
 * mvn archetype:generate \
 *   -DinteractiveMode=false \
 *   -DarchetypeGroupId=org.openjdk.jmh \
 *   -DarchetypeArtifactId=jmh-java-benchmark-archetype \
 *   -DarchetypeVersion=1.37 \
 *   -DgroupId=com.laifuzhi \
 *   -DartifactId=benchmark \
 *   -Dversion=1.0
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 1, time = 10)
@Threads(16)
@State(Scope.Benchmark)
public class BenchMarkTest {
    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(BenchMarkTest.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    private FileChannel fileChannel;
    private MappedByteBuffer mappedByteBuffer;

    @Setup
    public void setup() throws IOException {
        RandomAccessFile r = new RandomAccessFile("xxx", "rw");
        fileChannel = r.getChannel();
        mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, 1024 * 1024 * 1024);
    }

    @TearDown
    public void tearDown() throws IOException {
        PlatformDependent.freeDirectBuffer(mappedByteBuffer);
        fileChannel.close();
    }

    @Benchmark
    public void test(Blackhole blackhole) {
        blackhole.consume(mappedByteBuffer.slice().put((byte) 0));
        Blackhole.consumeCPU(10);
    }
}
