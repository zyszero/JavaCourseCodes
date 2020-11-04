package io.github.kimmking.gateway.outbound.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.function.Consumer;

public class NettyHttpClientInboundHandler extends SimpleChannelInboundHandler {
    private final Consumer<Object> httpResponseConsumer;


    public NettyHttpClientInboundHandler(Consumer<Object> httpResponseConsumer) {
        this.httpResponseConsumer = httpResponseConsumer;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) {
        httpResponseConsumer.accept(msg);
    }

}