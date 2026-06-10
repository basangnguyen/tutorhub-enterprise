import com.mycompany.tutorhub_enterprise.models.Packet;
import com.mycompany.tutorhub_enterprise.models.auth.AuthProtocol;
import com.mycompany.tutorhub_enterprise.models.auth.AuthRequest;
import com.mycompany.tutorhub_enterprise.models.auth.AuthResponse;
import com.mycompany.tutorhub_enterprise.utils.SerializationUtils;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class TestAuthRemote {
    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "wss://hocba299-3-tutorhub-core.hf.space";
        boolean legacy = args.length > 1 && "legacy".equalsIgnoreCase(args[1]);
        CountDownLatch done = new CountDownLatch(1);
        AuthRequest request = AuthRequest.login("probe@example.com", "wrong-password");

        WebSocketClient ws = new WebSocketClient(new URI(url)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("OPEN " + url);
                try {
                    Packet packet = legacy
                            ? new Packet("LOGIN", "probe@example.com|wrong-password")
                            : new Packet(AuthProtocol.LOGIN, request);
                    System.out.println("SEND " + packet.action);
                    send(SerializationUtils.serialize(packet));
                } catch (Exception e) {
                    e.printStackTrace();
                    done.countDown();
                }
            }

            @Override
            public void onMessage(String message) {
                System.out.println("TEXT " + message);
            }

            @Override
            public void onMessage(ByteBuffer message) {
                try {
                    byte[] bytes = new byte[message.remaining()];
                    message.get(bytes);
                    Packet packet = (Packet) SerializationUtils.deserialize(bytes);
                    System.out.println("PACKET action=" + packet.action + " success=" + packet.success + " message=" + packet.message);
                    System.out.println("DATA class=" + (packet.data == null ? "null" : packet.data.getClass().getName()));
                    if (packet.data instanceof AuthResponse response) {
                        System.out.println("AUTH_RESPONSE requestId=" + response.getRequestId()
                                + " success=" + response.isSuccess()
                                + " message=" + response.getMessage());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("CLOSE code=" + code + " reason=" + reason + " remote=" + remote);
                done.countDown();
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("ERROR " + ex.getMessage());
                done.countDown();
            }
        };

        if (!ws.connectBlocking(15, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Could not open websocket");
        }
        if (!done.await(20, TimeUnit.SECONDS)) {
            System.out.println("TIMEOUT waiting for AUTH_RESPONSE");
        }
        ws.close();
    }
}
