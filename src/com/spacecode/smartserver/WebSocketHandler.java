package com.spacecode.smartserver;

import com.spacecode.sdk.network.communication.MessageHandler;
import com.spacecode.smartserver.command.ClientCommandException;
import com.spacecode.smartserver.command.ClientCommandRegister;
import com.spacecode.smartserver.helper.SmartLogger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.util.logging.Level;

import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
public class WebSocketHandler extends SimpleChannelInboundHandler<Object>
{
    private WebSocketServerHandshaker _handshaker;
    private static final ClientCommandRegister COMMAND_REGISTER = new ClientCommandRegister();

    @Override
    public void channelActive(final ChannelHandlerContext ctx)
    {
        SmartServer.addClientChannel(ctx.channel(), ctx.handler());
        SmartLogger.getLogger().info("Connection from " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg)
    {
        if (msg instanceof FullHttpRequest)
        {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        }

        else if (msg instanceof WebSocketFrame)
        {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
    {
        ctx.flush();
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req)
    {
        // Handle a bad request.
        if (!req.getDecoderResult().isSuccess())
        {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
            return;
        }

        // Allow only GET methods.
        if (req.getMethod() != GET)
        {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, FORBIDDEN));
            return;
        }

        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://" + req.headers().get(HOST), null, false);
        _handshaker = wsFactory.newHandshaker(req);

        if (_handshaker == null)
        {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }

        else
        {
            _handshaker.handshake(ctx.channel(), req);
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame)
    {
        if (frame instanceof CloseWebSocketFrame)
        {
            _handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }

        if (!(frame instanceof TextWebSocketFrame))
        {
            SmartLogger.getLogger().severe("Invalid WebSocketFrame not handled: "+frame.getClass());
            return;
        }

        String request = ((TextWebSocketFrame) frame).text();

        if(request.trim().isEmpty())
        {
            return;
        }

        String[] parameters = request.split(Character.toString(MessageHandler.DELIMITER));

        SmartLogger.getLogger().info(ctx.channel().remoteAddress().toString()+" - "+parameters[0]);

        try
        {
            COMMAND_REGISTER.execute(ctx, parameters);
        } catch (ClientCommandException cce)
        {
            SmartLogger.getLogger().log(Level.SEVERE, "ClientCommand exception occurred.", cce);
        }
    }

    private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res)
    {
        if (res.getStatus().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpHeaders.setContentLength(res, res.content().readableBytes());
        }

        // Send the response and close the connection if necessary.
        ChannelFuture f = ctx.channel().writeAndFlush(res);

        if (!HttpHeaders.isKeepAlive(req) || res.getStatus().code() != 200)
        {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
    {
        cause.printStackTrace();
        ctx.close();
    }

    private static class WebSocketServerIndexPage {

        private static final String NEWLINE = "\r\n";

        public static ByteBuf getContent(String webSocketLocation) {
            return Unpooled.copiedBuffer(
                    "<html><head><title>Web Socket Test</title></head>" + NEWLINE +
                            "<body>" + NEWLINE +
                            "<script type=\"text/javascript\">" + NEWLINE +
                            "var socket;" + NEWLINE +
                            "if (!window.WebSocket) {" + NEWLINE +
                            "  window.WebSocket = window.MozWebSocket;" + NEWLINE +
                            '}' + NEWLINE +
                            "if (window.WebSocket) {" + NEWLINE +
                            "  socket = new WebSocket(\"" + webSocketLocation + "\");" + NEWLINE +
                            "  socket.onmessage = function(event) {" + NEWLINE +
                            "    var ta = document.getElementById('responseText');" + NEWLINE +
                            "    ta.value = ta.value + '\\n' + event.data" + NEWLINE +
                            "  };" + NEWLINE +
                            "  socket.onopen = function(event) {" + NEWLINE +
                            "    var ta = document.getElementById('responseText');" + NEWLINE +
                            "    ta.value = \"Web Socket opened!\";" + NEWLINE +
                            "  };" + NEWLINE +
                            "  socket.onclose = function(event) {" + NEWLINE +
                            "    var ta = document.getElementById('responseText');" + NEWLINE +
                            "    ta.value = ta.value + \"Web Socket closed\"; " + NEWLINE +
                            "  };" + NEWLINE +
                            "} else {" + NEWLINE +
                            "  alert(\"Your browser does not support Web Socket.\");" + NEWLINE +
                            '}' + NEWLINE +
                            NEWLINE +
                            "function send(message) {" + NEWLINE +
                            "  if (!window.WebSocket) { return; }" + NEWLINE +
                            "  if (socket.readyState == WebSocket.OPEN) {" + NEWLINE +
                            "    socket.send(message);" + NEWLINE +
                            "  } else {" + NEWLINE +
                            "    alert(\"The socket is not open.\");" + NEWLINE +
                            "  }" + NEWLINE +
                            '}' + NEWLINE +
                            "</script>" + NEWLINE +
                            "<form onsubmit=\"return false;\">" + NEWLINE +
                            "<input type=\"text\" name=\"message\" value=\"Hello, World!\"/>" +
                            "<input type=\"button\" value=\"Send Web Socket Data\"" + NEWLINE +
                            "       onclick=\"send(this.form.message.value)\" />" + NEWLINE +
                            "<h3>Output</h3>" + NEWLINE +
                            "<textarea id=\"responseText\" style=\"width:500px;height:300px;\"></textarea>" + NEWLINE +
                            "</form>" + NEWLINE +
                            "</body>" + NEWLINE +
                            "</html>" + NEWLINE, CharsetUtil.US_ASCII);
        }

        private WebSocketServerIndexPage() {
            // Unused
        }
    }
}
