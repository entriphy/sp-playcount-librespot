package xyz.gianlu.librespot.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import com.spotify.Authentication;
import com.spotify.Keyexchange;
import com.spotify.connectstate.Connect;
import com.spotify.explicit.ExplicitContentPubsub;
import com.spotify.explicit.ExplicitContentPubsub.UserAttributesUpdate;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import xyz.gianlu.librespot.AbsConfiguration;
import xyz.gianlu.librespot.Version;
import xyz.gianlu.librespot.common.NameThreadFactory;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.crypto.CipherPair;
import xyz.gianlu.librespot.crypto.DiffieHellman;
import xyz.gianlu.librespot.crypto.Packet;
import xyz.gianlu.librespot.mercury.MercuryClient;
import xyz.gianlu.librespot.mercury.SubListener;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Gianlu
 */
public final class Session implements Closeable, SubListener {
    private static final Logger LOGGER = Logger.getLogger(Session.class);
    private static final byte[] serverKey = new byte[]{
            (byte) 0xac, (byte) 0xe0, (byte) 0x46, (byte) 0x0b, (byte) 0xff, (byte) 0xc2, (byte) 0x30, (byte) 0xaf, (byte) 0xf4, (byte) 0x6b, (byte) 0xfe, (byte) 0xc3,
            (byte) 0xbf, (byte) 0xbf, (byte) 0x86, (byte) 0x3d, (byte) 0xa1, (byte) 0x91, (byte) 0xc6, (byte) 0xcc, (byte) 0x33, (byte) 0x6c, (byte) 0x93, (byte) 0xa1,
            (byte) 0x4f, (byte) 0xb3, (byte) 0xb0, (byte) 0x16, (byte) 0x12, (byte) 0xac, (byte) 0xac, (byte) 0x6a, (byte) 0xf1, (byte) 0x80, (byte) 0xe7, (byte) 0xf6,
            (byte) 0x14, (byte) 0xd9, (byte) 0x42, (byte) 0x9d, (byte) 0xbe, (byte) 0x2e, (byte) 0x34, (byte) 0x66, (byte) 0x43, (byte) 0xe3, (byte) 0x62, (byte) 0xd2,
            (byte) 0x32, (byte) 0x7a, (byte) 0x1a, (byte) 0x0d, (byte) 0x92, (byte) 0x3b, (byte) 0xae, (byte) 0xdd, (byte) 0x14, (byte) 0x02, (byte) 0xb1, (byte) 0x81,
            (byte) 0x55, (byte) 0x05, (byte) 0x61, (byte) 0x04, (byte) 0xd5, (byte) 0x2c, (byte) 0x96, (byte) 0xa4, (byte) 0x4c, (byte) 0x1e, (byte) 0xcc, (byte) 0x02,
            (byte) 0x4a, (byte) 0xd4, (byte) 0xb2, (byte) 0x0c, (byte) 0x00, (byte) 0x1f, (byte) 0x17, (byte) 0xed, (byte) 0xc2, (byte) 0x2f, (byte) 0xc4, (byte) 0x35,
            (byte) 0x21, (byte) 0xc8, (byte) 0xf0, (byte) 0xcb, (byte) 0xae, (byte) 0xd2, (byte) 0xad, (byte) 0xd7, (byte) 0x2b, (byte) 0x0f, (byte) 0x9d, (byte) 0xb3,
            (byte) 0xc5, (byte) 0x32, (byte) 0x1a, (byte) 0x2a, (byte) 0xfe, (byte) 0x59, (byte) 0xf3, (byte) 0x5a, (byte) 0x0d, (byte) 0xac, (byte) 0x68, (byte) 0xf1,
            (byte) 0xfa, (byte) 0x62, (byte) 0x1e, (byte) 0xfb, (byte) 0x2c, (byte) 0x8d, (byte) 0x0c, (byte) 0xb7, (byte) 0x39, (byte) 0x2d, (byte) 0x92, (byte) 0x47,
            (byte) 0xe3, (byte) 0xd7, (byte) 0x35, (byte) 0x1a, (byte) 0x6d, (byte) 0xbd, (byte) 0x24, (byte) 0xc2, (byte) 0xae, (byte) 0x25, (byte) 0x5b, (byte) 0x88,
            (byte) 0xff, (byte) 0xab, (byte) 0x73, (byte) 0x29, (byte) 0x8a, (byte) 0x0b, (byte) 0xcc, (byte) 0xcd, (byte) 0x0c, (byte) 0x58, (byte) 0x67, (byte) 0x31,
            (byte) 0x89, (byte) 0xe8, (byte) 0xbd, (byte) 0x34, (byte) 0x80, (byte) 0x78, (byte) 0x4a, (byte) 0x5f, (byte) 0xc9, (byte) 0x6b, (byte) 0x89, (byte) 0x9d,
            (byte) 0x95, (byte) 0x6b, (byte) 0xfc, (byte) 0x86, (byte) 0xd7, (byte) 0x4f, (byte) 0x33, (byte) 0xa6, (byte) 0x78, (byte) 0x17, (byte) 0x96, (byte) 0xc9,
            (byte) 0xc3, (byte) 0x2d, (byte) 0x0d, (byte) 0x32, (byte) 0xa5, (byte) 0xab, (byte) 0xcd, (byte) 0x05, (byte) 0x27, (byte) 0xe2, (byte) 0xf7, (byte) 0x10,
            (byte) 0xa3, (byte) 0x96, (byte) 0x13, (byte) 0xc4, (byte) 0x2f, (byte) 0x99, (byte) 0xc0, (byte) 0x27, (byte) 0xbf, (byte) 0xed, (byte) 0x04, (byte) 0x9c,
            (byte) 0x3c, (byte) 0x27, (byte) 0x58, (byte) 0x04, (byte) 0xb6, (byte) 0xb2, (byte) 0x19, (byte) 0xf9, (byte) 0xc1, (byte) 0x2f, (byte) 0x02, (byte) 0xe9,
            (byte) 0x48, (byte) 0x63, (byte) 0xec, (byte) 0xa1, (byte) 0xb6, (byte) 0x42, (byte) 0xa0, (byte) 0x9d, (byte) 0x48, (byte) 0x25, (byte) 0xf8, (byte) 0xb3,
            (byte) 0x9d, (byte) 0xd0, (byte) 0xe8, (byte) 0x6a, (byte) 0xf9, (byte) 0x48, (byte) 0x4d, (byte) 0xa1, (byte) 0xc2, (byte) 0xba, (byte) 0x86, (byte) 0x30,
            (byte) 0x42, (byte) 0xea, (byte) 0x9d, (byte) 0xb3, (byte) 0x08, (byte) 0x6c, (byte) 0x19, (byte) 0x0e, (byte) 0x48, (byte) 0xb3, (byte) 0x9d, (byte) 0x66,
            (byte) 0xeb, (byte) 0x00, (byte) 0x06, (byte) 0xa2, (byte) 0x5a, (byte) 0xee, (byte) 0xa1, (byte) 0x1b, (byte) 0x13, (byte) 0x87, (byte) 0x3c, (byte) 0xd7,
            (byte) 0x19, (byte) 0xe6, (byte) 0x55, (byte) 0xbd
    };
    private final DiffieHellman keys;
    private final Inner inner;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NameThreadFactory(r -> "session-scheduler-" + r.hashCode()));
    private final ExecutorService executorService = Executors.newCachedThreadPool(new NameThreadFactory(r -> "handle-packet-" + r.hashCode()));
    private final AtomicBoolean authLock = new AtomicBoolean(false);
    private final List<CloseListener> closeListeners = Collections.synchronizedList(new ArrayList<>());
    private final List<ReconnectionListener> reconnectionListeners = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, String> userAttributes = Collections.synchronizedMap(new HashMap<>());
    private ConnectionHolder conn;
    private volatile CipherPair cipherPair;
    private Receiver receiver;
    private Authentication.APWelcome apWelcome = null;
    private MercuryClient mercuryClient;
    private String countryCode = null;
    private volatile boolean closed = false;
    private volatile ScheduledFuture<?> scheduledReconnect = null;

    private Session(Inner inner, String addr) throws IOException {
        this.inner = inner;
        this.keys = new DiffieHellman(inner.random);
        this.conn = ConnectionHolder.create(addr);

        LOGGER.info(String.format("Created new session! {deviceId: %s, ap: %s} ", inner.deviceId, addr));
    }

    @NotNull
    static Session from(@NotNull Inner inner) throws IOException {
        ApResolver.fillPool();
        return new Session(inner, ApResolver.getRandomAccesspoint());
    }

    void connect() throws IOException, GeneralSecurityException, SpotifyAuthenticationException {
        Accumulator acc = new Accumulator();

        // Send ClientHello

        byte[] nonce = new byte[0x10];
        inner.random.nextBytes(nonce);

        Keyexchange.ClientHello clientHello = Keyexchange.ClientHello.newBuilder()
                .setBuildInfo(Version.standardBuildInfo())
                .addCryptosuitesSupported(Keyexchange.Cryptosuite.CRYPTO_SUITE_SHANNON)
                .setLoginCryptoHello(Keyexchange.LoginCryptoHelloUnion.newBuilder()
                        .setDiffieHellman(Keyexchange.LoginCryptoDiffieHellmanHello.newBuilder()
                                .setGc(ByteString.copyFrom(keys.publicKeyArray()))
                                .setServerKeysKnown(1)
                                .build())
                        .build())
                .setClientNonce(ByteString.copyFrom(nonce))
                .setPadding(ByteString.copyFrom(new byte[]{0x1e}))
                .build();

        byte[] clientHelloBytes = clientHello.toByteArray();
        int length = 2 + 4 + clientHelloBytes.length;
        conn.out.writeByte(0);
        conn.out.writeByte(4);
        conn.out.writeInt(length);
        conn.out.write(clientHelloBytes);
        conn.out.flush();

        acc.writeByte(0);
        acc.writeByte(4);
        acc.writeInt(length);
        acc.write(clientHelloBytes);


        // Read APResponseMessage

        length = conn.in.readInt();
        acc.writeInt(length);
        byte[] buffer = new byte[length - 4];
        conn.in.readFully(buffer);
        acc.write(buffer);
        acc.dump();

        Keyexchange.APResponseMessage apResponseMessage = Keyexchange.APResponseMessage.parseFrom(buffer);
        byte[] sharedKey = Utils.toByteArray(keys.computeSharedKey(apResponseMessage.getChallenge().getLoginCryptoChallenge().getDiffieHellman().getGs().toByteArray()));


        // Check gs_signature

        KeyFactory factory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = factory.generatePublic(new RSAPublicKeySpec(new BigInteger(1, serverKey), BigInteger.valueOf(65537)));

        Signature sig = Signature.getInstance("SHA1withRSA");
        sig.initVerify(publicKey);
        sig.update(apResponseMessage.getChallenge().getLoginCryptoChallenge().getDiffieHellman().getGs().toByteArray());
        if (!sig.verify(apResponseMessage.getChallenge().getLoginCryptoChallenge().getDiffieHellman().getGsSignature().toByteArray()))
            throw new GeneralSecurityException("Failed signature check!");


        // Solve challenge

        ByteArrayOutputStream data = new ByteArrayOutputStream(0x64);

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(sharedKey, "HmacSHA1"));
        for (int i = 1; i < 6; i++) {
            mac.update(acc.array());
            mac.update(new byte[]{(byte) i});
            data.write(mac.doFinal());
            mac.reset();
        }

        byte[] dataArray = data.toByteArray();
        mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(Arrays.copyOfRange(dataArray, 0, 0x14), "HmacSHA1"));
        mac.update(acc.array());

        byte[] challenge = mac.doFinal();
        Keyexchange.ClientResponsePlaintext clientResponsePlaintext = Keyexchange.ClientResponsePlaintext.newBuilder()
                .setLoginCryptoResponse(Keyexchange.LoginCryptoResponseUnion.newBuilder()
                        .setDiffieHellman(Keyexchange.LoginCryptoDiffieHellmanResponse.newBuilder()
                                .setHmac(ByteString.copyFrom(challenge)).build())
                        .build())
                .setPowResponse(Keyexchange.PoWResponseUnion.newBuilder().build())
                .setCryptoResponse(Keyexchange.CryptoResponseUnion.newBuilder().build())
                .build();

        byte[] clientResponsePlaintextBytes = clientResponsePlaintext.toByteArray();
        length = 4 + clientResponsePlaintextBytes.length;
        conn.out.writeInt(length);
        conn.out.write(clientResponsePlaintextBytes);
        conn.out.flush();

        try {
            byte[] scrap = new byte[4];
            conn.socket.setSoTimeout(300);
            int read = conn.in.read(scrap);
            if (read == scrap.length) {
                length = (scrap[0] << 24) | (scrap[1] << 16) | (scrap[2] << 8) | (scrap[3] & 0xFF);
                byte[] payload = new byte[length - 4];
                conn.in.readFully(payload);
                Keyexchange.APLoginFailed failed = Keyexchange.APResponseMessage.parseFrom(payload).getLoginFailed();
                throw new SpotifyAuthenticationException(failed);
            } else if (read > 0) {
                throw new IllegalStateException("Read unknown data!");
            }
        } catch (SocketTimeoutException ignored) {
        } finally {
            conn.socket.setSoTimeout(0);
        }

        synchronized (authLock) {
            // Init Shannon cipher
            cipherPair = new CipherPair(Arrays.copyOfRange(data.toByteArray(), 0x14, 0x34),
                    Arrays.copyOfRange(data.toByteArray(), 0x34, 0x54));

            authLock.set(true);
        }

        LOGGER.info("Connected successfully!");
    }

    /**
     * Authenticates with the server and creates all the necessary components.
     * All of them should be initialized inside the synchronized block and MUST NOT call any method on this {@link Session} object.
     */
    void authenticate(@NotNull Authentication.LoginCredentials credentials) throws IOException, GeneralSecurityException, SpotifyAuthenticationException, MercuryClient.MercuryException {
        authenticatePartial(credentials, false);

        synchronized (authLock) {
            mercuryClient = new MercuryClient(this);

            authLock.set(false);
            authLock.notifyAll();
        }



        LOGGER.info(String.format("Authenticated as %s!", apWelcome.getCanonicalUsername()));


        mercuryClient.interestedIn("spotify:user:attributes:update", this);
    }

    /**
     * Authenticates with the server. Does not create all the components unlike {@link Session#authenticate(Authentication.LoginCredentials)}.
     *
     * @param removeLock Whether {@link Session#authLock} should be released or not.
     *                   {@code false} for {@link Session#authenticate(Authentication.LoginCredentials)},
     *                   {@code true} for {@link Session#reconnect()}.
     */
    private void authenticatePartial(@NotNull Authentication.LoginCredentials credentials, boolean removeLock) throws IOException, GeneralSecurityException, SpotifyAuthenticationException {
        if (cipherPair == null) throw new IllegalStateException("Connection not established!");

        Authentication.ClientResponseEncrypted clientResponseEncrypted = Authentication.ClientResponseEncrypted.newBuilder()
                .setLoginCredentials(credentials)
                .setSystemInfo(Authentication.SystemInfo.newBuilder()
                        .setOs(Authentication.Os.OS_UNKNOWN)
                        .setCpuFamily(Authentication.CpuFamily.CPU_UNKNOWN)
                        .setSystemInformationString(Version.systemInfoString())
                        .setDeviceId(inner.deviceId)
                        .build())
                .setVersionString(Version.versionString())
                .build();

        sendUnchecked(Packet.Type.Login, clientResponseEncrypted.toByteArray());

        Packet packet = cipherPair.receiveEncoded(conn.in);
        if (packet.is(Packet.Type.APWelcome)) {
            apWelcome = Authentication.APWelcome.parseFrom(packet.payload);

            receiver = new Receiver();
            new Thread(receiver, "session-packet-receiver").start();

            byte[] bytes0x0f = new byte[20];
            random().nextBytes(bytes0x0f);
            sendUnchecked(Packet.Type.Unknown_0x0f, bytes0x0f);

            ByteBuffer preferredLocale = ByteBuffer.allocate(18 + 5);
            preferredLocale.put((byte) 0x0).put((byte) 0x0).put((byte) 0x10).put((byte) 0x0).put((byte) 0x02);
            preferredLocale.put("preferred-locale".getBytes());
            preferredLocale.put(inner.configuration.preferredLocale().getBytes());
            sendUnchecked(Packet.Type.PreferredLocale, preferredLocale.array());

            if (removeLock) {
                synchronized (authLock) {
                    authLock.set(false);
                    authLock.notifyAll();
                }
            }

            if (conf().storeCredentials()) {
                ByteString reusable = apWelcome.getReusableAuthCredentials();
                Authentication.AuthenticationType reusableType = apWelcome.getReusableAuthCredentialsType();

                JsonObject obj = new JsonObject();
                obj.addProperty("username", apWelcome.getCanonicalUsername());
                obj.addProperty("credentials", Utils.toBase64(reusable));
                obj.addProperty("type", reusableType.name());

                File storeFile = conf().credentialsFile();
                if (storeFile == null) throw new IllegalArgumentException();
                try (FileOutputStream out = new FileOutputStream(storeFile)) {
                    out.write(obj.toString().getBytes());
                }
            }
        } else if (packet.is(Packet.Type.AuthFailure)) {
            throw new SpotifyAuthenticationException(Keyexchange.APLoginFailed.parseFrom(packet.payload));
        } else {
            throw new IllegalStateException("Unknown CMD 0x" + Integer.toHexString(packet.cmd));
        }
    }

    @Override
    public void close() throws IOException {
        if (receiver != null) {
            receiver.stop();
            receiver = null;
        }

        if (mercuryClient != null) {
            mercuryClient.close();
            mercuryClient = null;
        }

        executorService.shutdown();
        conn.socket.close();

        synchronized (authLock) {
            apWelcome = null;
            cipherPair = null;
            closed = true;
        }

        synchronized (closeListeners) {
            Iterator<CloseListener> i = closeListeners.iterator();
            while (i.hasNext()) {
                i.next().onClosed();
                i.remove();
            }
        }

        LOGGER.info(String.format("Closed session. {deviceId: %s, ap: %s} ", inner.deviceId, conn.socket.getInetAddress()));
    }

    private void sendUnchecked(Packet.Type cmd, byte[] payload) throws IOException {
        cipherPair.sendEncoded(conn.out, cmd.val, payload);
    }

    private void waitAuthLock() {
        if (closed) throw new IllegalStateException("Session is closed!");

        synchronized (authLock) {
            if (cipherPair == null || authLock.get()) {
                try {
                    authLock.wait();
                } catch (InterruptedException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
    }

    public void send(Packet.Type cmd, byte[] payload) throws IOException {
        if (closed) throw new IllegalStateException("Session is closed!");

        synchronized (authLock) {
            if (cipherPair == null || authLock.get()) {
                try {
                    authLock.wait();
                } catch (InterruptedException ex) {
                    throw new IllegalStateException(ex);
                }
            }

            sendUnchecked(cmd, payload);
        }
    }

    @NotNull
    public MercuryClient mercury() {
        waitAuthLock();
        if (mercuryClient == null) throw new IllegalStateException("Session isn't authenticated!");
        return mercuryClient;
    }

    @NotNull
    public Authentication.APWelcome apWelcome() {
        waitAuthLock();
        if (apWelcome == null) throw new IllegalStateException("Session isn't authenticated!");
        return apWelcome;
    }

    @NotNull
    ExecutorService executor() {
        return executorService;
    }

    @NotNull
    public Random random() {
        return inner.random;
    }

    private void reconnect() {
        synchronized (reconnectionListeners) {
            reconnectionListeners.forEach(ReconnectionListener::onConnectionDropped);
        }

        try {
            if (conn != null) {
                conn.socket.close();
                receiver.stop();
            }

            conn = ConnectionHolder.create(ApResolver.getRandomAccesspoint());
            connect();
            authenticatePartial(Authentication.LoginCredentials.newBuilder()
                    .setUsername(apWelcome.getCanonicalUsername())
                    .setTyp(apWelcome.getReusableAuthCredentialsType())
                    .setAuthData(apWelcome.getReusableAuthCredentials())
                    .build(), true);

            LOGGER.info(String.format("Re-authenticated as %s!", apWelcome.getCanonicalUsername()));

            synchronized (reconnectionListeners) {
                reconnectionListeners.forEach(ReconnectionListener::onConnectionEstablished);
            }
        } catch (IOException | GeneralSecurityException | SpotifyAuthenticationException ex) {
            conn = null;
            LOGGER.error("Failed reconnecting, retrying in 10 seconds...", ex);
            scheduler.schedule(this::reconnect, 10, TimeUnit.SECONDS);
        }
    }

    @NotNull
    public AbsConfiguration conf() {
        return inner.configuration;
    }

    private void parseProductInfo(@NotNull InputStream in) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = dBuilder.parse(in);

        Node products = doc.getElementsByTagName("products").item(0);
        if (products == null) return;

        Node product = products.getChildNodes().item(0);
        if (product == null) return;

        NodeList properties = product.getChildNodes();
        for (int i = 0; i < properties.getLength(); i++) {
            Node node = properties.item(i);
            userAttributes.put(node.getNodeName(), node.getTextContent());
        }

        LOGGER.trace("Parsed product info: " + userAttributes);
    }

    @Override
    public void event(@NotNull MercuryClient.Response resp) {
        if (resp.uri.equals("spotify:user:attributes:update")) {
            UserAttributesUpdate attributesUpdate;
            try {
                attributesUpdate = UserAttributesUpdate.parseFrom(resp.payload.stream());
            } catch (IOException ex) {
                LOGGER.warn("Failed parsing user attributes update.", ex);
                return;
            }

            for (ExplicitContentPubsub.KeyValuePair pair : attributesUpdate.getPairsList()) {
                userAttributes.put(pair.getKey(), pair.getValue());
                LOGGER.trace(String.format("Updated user attribute: %s -> %s", pair.getKey(), pair.getValue()));
            }
        }
    }

    public interface ReconnectionListener {
        void onConnectionDropped();

        void onConnectionEstablished();
    }

    public interface CloseListener {
        void onClosed();
    }

    static class Inner {
        final Connect.DeviceType deviceType;
        final String deviceName;
        final SecureRandom random;
        final String deviceId;
        final AbsConfiguration configuration;

        private Inner(Connect.DeviceType deviceType, String deviceName, AbsConfiguration configuration) {
            this.deviceType = deviceType;
            this.deviceName = deviceName;
            this.configuration = configuration;
            this.random = new SecureRandom();

            String configuredDeviceId = configuration.deviceId();
            this.deviceId = (configuredDeviceId == null || configuredDeviceId.isEmpty()) ?
                    Utils.randomHexString(random, 40).toLowerCase() : configuredDeviceId;
        }

        @NotNull
        static Inner from(@NotNull AbsConfiguration configuration) {
            String deviceName = configuration.deviceName();
            if (deviceName == null || deviceName.isEmpty())
                throw new IllegalArgumentException("Device name required: " + deviceName);

            Connect.DeviceType deviceType = configuration.deviceType();
            if (deviceType == null)
                throw new IllegalArgumentException("Device type required!");

            return new Inner(deviceType, deviceName, configuration);
        }
    }

    /**
     * Builder for setting up a {@link Session} object.
     */
    public static class Builder {
        private final Inner inner;
        private final AbsConfiguration authConf;
        private Authentication.LoginCredentials loginCredentials = null;
        private String[] args;

        public Builder(@NotNull AbsConfiguration configuration, @Nullable String[] args) {
            this.inner = Inner.from(configuration);
            this.authConf = configuration;
            this.args = args;
        }

        @NotNull
        public Builder userPass(@NotNull String username, @NotNull String password) {
            loginCredentials = Authentication.LoginCredentials.newBuilder()
                    .setUsername(username)
                    .setTyp(Authentication.AuthenticationType.AUTHENTICATION_USER_PASS)
                    .setAuthData(ByteString.copyFromUtf8(password))
                    .build();
            return this;
        }

        /**
         * Creates a connected and fully authenticated {@link Session} object.
         */
        @NotNull
        public Session create() throws IOException, GeneralSecurityException, SpotifyAuthenticationException, MercuryClient.MercuryException {
            if (authConf.storeCredentials()) {
                File storeFile = authConf.credentialsFile();
                if (storeFile != null && storeFile.exists()) {
                    JsonObject obj = JsonParser.parseReader(new FileReader(storeFile)).getAsJsonObject();
                    loginCredentials = Authentication.LoginCredentials.newBuilder()
                            .setTyp(Authentication.AuthenticationType.valueOf(obj.get("type").getAsString()))
                            .setUsername(obj.get("username").getAsString())
                            .setAuthData(Utils.fromBase64(obj.get("credentials").getAsString()))
                            .build();
                }
            }

            if (loginCredentials == null) {
                String username;
                String password;
                if (System.getenv("SPOTIFY_USERNAME") != null && System.getenv("SPOTIFY_PASSWORD") != null) {
                    username = System.getenv("SPOTIFY_USERNAME");
                    password = System.getenv("SPOTIFY_PASSWORD");
                } else {
                    if (args.length < 2) throw new IllegalArgumentException("Missing username and password!");
                    username = args[0];
                    password = args[1];
                }

                if (username == null) throw new IllegalArgumentException("Missing authUsername!");
                if (password == null) throw new IllegalArgumentException("Missing authPassword!");
                userPass(username, password);
            }

            Session session = Session.from(inner);
            session.connect();
            session.authenticate(loginCredentials);
            return session;
        }
    }

    public static class SpotifyAuthenticationException extends Exception {
        private SpotifyAuthenticationException(Keyexchange.APLoginFailed loginFailed) {
            super(loginFailed.getErrorCode().name());
        }
    }

    private static class Accumulator extends DataOutputStream {
        private byte[] bytes;

        Accumulator() {
            super(new ByteArrayOutputStream());
        }

        void dump() throws IOException {
            bytes = ((ByteArrayOutputStream) this.out).toByteArray();
            this.close();
        }

        @NotNull
        byte[] array() {
            return bytes;
        }
    }

    private static class ConnectionHolder {
        final Socket socket;
        final DataInputStream in;
        final DataOutputStream out;

        private ConnectionHolder(@NotNull Socket socket) throws IOException {
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
        }

        @NotNull
        static ConnectionHolder create(@NotNull String addr) throws IOException {
            int colon = addr.indexOf(':');
            String apAddr = addr.substring(0, colon);
            int apPort = Integer.parseInt(addr.substring(colon + 1));
            return new ConnectionHolder(new Socket(apAddr, apPort));
        }
    }

    private class Receiver implements Runnable {
        private volatile boolean shouldStop = false;

        private Receiver() {
        }

        void stop() {
            shouldStop = true;
        }

        @Override
        public void run() {
            while (!shouldStop) {
                Packet packet;
                Packet.Type cmd;
                try {
                    packet = cipherPair.receiveEncoded(conn.in);
                    cmd = Packet.Type.parse(packet.cmd);
                    if (cmd == null) {
                        LOGGER.info(String.format("Skipping unknown command {cmd: 0x%s, payload: %s}", Integer.toHexString(packet.cmd), Utils.bytesToHex(packet.payload)));
                        continue;
                    }
                } catch (IOException | GeneralSecurityException ex) {
                    if (!shouldStop) {
                        LOGGER.fatal("Failed reading packet!", ex);
                        reconnect();
                    }

                    return;
                }

                if (shouldStop) return;

                switch (cmd) {
                    case Ping:
                        if (scheduledReconnect != null) scheduledReconnect.cancel(true);
                        scheduledReconnect = scheduler.schedule(() -> {
                            LOGGER.warn("Socket timed out. Reconnecting...");
                            reconnect();
                        }, 2 * 60 + 5, TimeUnit.SECONDS);

                        try {
                            send(Packet.Type.Pong, packet.payload);
                        } catch (IOException ex) {
                            LOGGER.fatal("Failed sending Pong!", ex);
                        }
                        break;
                    case PongAck:
                        // Silent
                        break;
                    case CountryCode:
                        countryCode = new String(packet.payload);
                        LOGGER.info("Received CountryCode: " + countryCode);
                        break;
                    case LicenseVersion:
                        ByteBuffer licenseVersion = ByteBuffer.wrap(packet.payload);
                        short id = licenseVersion.getShort();
                        if (id != 0) {
                            byte[] buffer = new byte[licenseVersion.get()];
                            licenseVersion.get(buffer);
                            LOGGER.info(String.format("Received LicenseVersion: %d, %s", id, new String(buffer)));
                        } else {
                            LOGGER.info(String.format("Received LicenseVersion: %d", id));
                        }
                        break;
                    case Unknown_0x10:
                        LOGGER.debug("Received 0x10: " + Utils.bytesToHex(packet.payload));
                        break;
                    case MercurySub:
                    case MercuryUnsub:
                    case MercuryEvent:
                    case MercuryReq:
                        mercury().dispatch(packet);
                        break;
                    case AesKey:
                    case ChannelError:
                    case ProductInfo:
                        try {
                            parseProductInfo(new ByteArrayInputStream(packet.payload));
                        } catch (IOException | ParserConfigurationException | SAXException ex) {
                            LOGGER.warn("Failed parsing prodcut info!", ex);
                        }
                        break;
                    default:
                        LOGGER.info("Skipping " + cmd.name());
                        break;
                }
            }
        }
    }
}
