package discard;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.netty.util.CharsetUtil;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@ChannelHandler.Sharable
public class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<Object> {

    private final StringBuilder buf = new StringBuilder();
    private FullHttpResponse response;
    Logger logger = LoggerFactory.getLogger("HttpHelloWorldServer");

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("done");
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            logger.info("Channel id: " + ctx.channel().id());
            Thread.sleep(5000);

            HttpRequest request = (HttpRequest) msg;
            buf.setLength(0);
            buf.append("VERSION: ").append(request.getProtocolVersion()).append("\r\n");
            buf.append("HOSTNAME: ").append(HttpHeaders.getHost(request, "unknown")).append("\r\n");
            buf.append("REQUEST_URI: ").append(request.getUri()).append("\r\n\r\n");

            HttpHeaders headers = request.headers();
            if (!headers.isEmpty()) {
                for (Map.Entry<String, String> h : headers) {
                    String key = h.getKey();
                    String value = h.getValue();
                    buf.append("HEADER: ").append(key).append("=").append(value).append("\r\n");
                }
            }

            response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));
            response.headers().set(CONTENT_TYPE, "text/plain");
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

            // Encode the cookie.
            String cookieString = request.headers().get(COOKIE);
            if (cookieString != null) {
                Set<Cookie> cookies = CookieDecoder.decode(cookieString);
                if (!cookies.isEmpty()) {
                    for (io.netty.handler.codec.http.cookie.Cookie cookie: cookies) {
                        response.headers().add(SET_COOKIE, ServerCookieEncoder.encode((io.netty.handler.codec.http.Cookie) cookie));
                    }
                }
            } else {
                response.headers().add(SET_COOKIE, ServerCookieEncoder.encode("key1", "value1"));
                response.headers().add(SET_COOKIE, ServerCookieEncoder.encode("key2", "value2"));
            }

            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    private void createExitCall() throws IOException {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet request = new HttpGet("http://localhost:8080/continueBT");
        httpClient.execute(request);
        System.err.println("Created an Apache ExitCall");
    }
}