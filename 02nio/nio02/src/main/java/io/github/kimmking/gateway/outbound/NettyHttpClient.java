package io.github.kimmking.gateway.outbound;

import io.github.kimmking.gateway.outbound.netty4.NettyHttpClientInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.concurrent.FutureListener;

import java.net.URI;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NettyHttpClient {

    private final Bootstrap b;
    private final EventLoopGroup workerGroup;

    public NettyHttpClient(){
        workerGroup = new NioEventLoopGroup();
         b = new Bootstrap()
                 .group(workerGroup)
                 .channel(NioSocketChannel.class)
                 .option(ChannelOption.SO_KEEPALIVE, true);
    }

    public void connect(final Consumer<Object> httpResponseConsumer, final HttpHeaders headers, final String backendUrl) {
        Matcher matcher = Pattern.compile("http://(.+):([0-9]+)").matcher(backendUrl);
        String host;
        int port;
        if (matcher.find()) {
            host = matcher.group(1);
            port = Integer.parseInt(matcher.group(2));
        } else {
            throw new IllegalArgumentException("illegal backendUrl");
        }
        try {
            // Start the client.
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    // 客户端接收到的是httpResponse响应，所以要使用HttpResponseDecoder进行解码
                    ch.pipeline().addLast(new HttpResponseDecoder());
//                     客户端发送的是httprequest，所以要使用HttpRequestEncoder进行编码
                    ch.pipeline().addLast(new HttpRequestEncoder())
                            .addLast(new NettyHttpClientInboundHandler(httpResponseConsumer));
//                    ch.pipeline().addLast(new HttpClientOutboundHandler(ctx, inbound));
                }
            });
            ChannelFuture f = b.connect(host, port).sync();
            String url = "http://" + host + ":" + port + "/api/hello";
            URI uri = new URI(url);
            DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                    HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString(),
                    Unpooled.buffer(0), headers, new DefaultHttpHeaders(true));
            // 构建http请求
            request.headers().set(HttpHeaderNames.HOST, host);
            request.headers().set(HttpHeaderNames.CONNECTION,
                    HttpHeaderNames.CONNECTION);
            request.headers().set(HttpHeaderNames.CONTENT_LENGTH,
                    request.content().readableBytes());
            f.channel().write(request);
            f.channel().flush();
            f.channel().closeFuture().sync();
        }catch (Exception e){
            throw new RuntimeException(e.getCause());
        } finally{
            workerGroup.shutdownGracefully();
        }
    }

}