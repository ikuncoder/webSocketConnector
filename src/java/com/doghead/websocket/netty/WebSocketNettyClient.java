package com.doghead.websocket.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;
import java.net.URI;
import java.net.URISyntaxException;


public class WebSocketNettyClient {

    public static void main(String[] args) throws Exception {
        connect();
    }


    private static void connect() throws URISyntaxException {
        EventLoopGroup group = new NioEventLoopGroup(1);
        final ClientHandler handler = new ClientHandler();
        String url = "";//your websocket url,for example，wss://xxxxx.com/websocket/**
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
        final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        try {
            SslContext sslContext = SslContextBuilder.forClient()// 配置支持的协议版本
                    .build();
            URI websocketURI = uri;
            WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(websocketURI, WebSocketVersion.V13, (String) null, true, new DefaultHttpHeaders());
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(sslContext.newHandler(ch.alloc(), host, 443));
                            // 添加一个http的编解码器
                            pipeline.addLast(new HttpClientCodec());
                            // 添加一个用于支持大数据流的支持
                            pipeline.addLast(new ChunkedWriteHandler());
                            // 添加一个聚合器，这个聚合器主要是将HttpMessage聚合成FullHttpRequest/Response
                            pipeline.addLast(new HttpObjectAggregator(1024 * 64));
                            pipeline.addLast(handler);
                        }
                    });

            HttpHeaders httpHeaders = new DefaultHttpHeaders();
            //进行握手
            final Channel channel = bootstrap.connect(websocketURI.getHost(), 443).sync().channel();
            handler.setHandshaker(handshaker);
            handshaker.handshake(channel);
            //阻塞等待是否握手成功
            handler.handshakeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static class ClientHandler extends SimpleChannelInboundHandler<Object> {

        private WebSocketClientHandshaker handshaker;
        ChannelPromise handshakeFuture;

        private Long startTime;

        /**
         * 当客户端主动链接服务端的链接后，调用此方法
         *
         * @param channelHandlerContext ChannelHandlerContext
         */
        @Override
        public void channelActive(ChannelHandlerContext channelHandlerContext) {
            System.out.println("客户端Active .....");
            handlerAdded(channelHandlerContext);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            System.out.println("\n\t⌜⎓⎓⎓⎓⎓⎓exception⎓⎓⎓⎓⎓⎓⎓⎓⎓\n" +
                    cause.getMessage());
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("channelInactive");
            super.channelInactive(ctx);
        }

        public void setHandshaker(WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }

        public void handlerAdded(ChannelHandlerContext ctx) {
            this.handshakeFuture = ctx.newPromise();
        }

        public ChannelFuture handshakeFuture() {
            return this.handshakeFuture;
        }

        protected void channelRead0(ChannelHandlerContext ctx, Object o) throws Exception {

            // 握手协议返回，设置结束握手
            if (!this.handshaker.isHandshakeComplete()) {
                FullHttpResponse response = (FullHttpResponse) o;
                this.handshaker.finishHandshake(ctx.channel(), response);
                this.handshakeFuture.setSuccess();
                System.out.println("---------握手成功------------");
                //do something after handshake success;
                return;
            } else if (o instanceof TextWebSocketFrame) {
                //do something after receive text message;
            } else if (o instanceof CloseWebSocketFrame) {
                System.out.println("连接关闭");
            }
        }
    }
}
