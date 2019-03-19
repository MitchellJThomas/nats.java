// Copyright 2015-2018 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client;

import java.util.List;
import java.util.Properties;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;

import javax.net.ssl.SSLContext;

import io.nats.client.impl.DataPort;
import io.nats.client.impl.SSLUtils;
import io.nats.client.impl.SocketDataPort;

/**
 * The Options class specifies the connection options for a new NATs connection, including the default options.
 * Options are created using a {@link Options.Builder Builder}.
 * This class, and the builder associated with it, is basically a long list of parameters. The documentation attempts
 * to clarify the value of each parameter in place on the builder and here, but it may be easier to read the documentation
 * starting with the {@link Options.Builder Builder}, since it has a simple list of methods that configure the connection.
 */
public class Options {
    // NOTE TO DEVS
    // To add an option, you have to:
    // * add property
    // * add field in builder
    // * add field in options
    // * add a chainable method in builder
    // * add update build
    // * update constructor that takes properties
    // * optional default in statics

    /**
     * Default server URL.
     *
     * <p>
     * This property is defined as {@value #DEFAULT_URL}
     */
    public static final String DEFAULT_URL = "nats://localhost:4222";

    /**
     * Default server port.
     *
     * <p>
     * This property is defined as {@value #DEFAULT_PORT}
     */
    public static final int DEFAULT_PORT = 4222;

    /**
     * Default maximum number of reconnect attempts, see {@link #getMaxReconnect() getMaxReconnect()}.
     *
     * <p>
     * This property is defined as {@value #DEFAULT_MAX_RECONNECT}
     */
    public static final int DEFAULT_MAX_RECONNECT = 60;

    /**
     * Default wait time before attempting reconnection to the same server, see {@link #getReconnectWait() getReconnectWait()}.
     *
     * <p>
     * This property is defined as 2 seconds.
     */
    public static final Duration DEFAULT_RECONNECT_WAIT = Duration.ofSeconds(2);

    /**
     * Default connection timeout, see {@link #getConnectionTimeout() getConnectionTimeout()}.
     *
     * <p>
     * This property is defined as 2 seconds.}
     */
    public static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(2);

    /**
     * Default server ping interval. The client will send a ping to the server on this interval to insure liveness.
     * The server may send pings to the client as well, these are handled automatically by the library
     * , see {@link #getPingInterval() getPingInterval()}.
     * 
     * <p>A value of {@code <=0} means disabled.
     *
     * <p>This property is defined as 2 minutes.
     */

    public static final Duration DEFAULT_PING_INTERVAL = Duration.ofMinutes(2);

    /**
     * Default interval to clean up cancelled/timed out requests.
     * A timer is used to clean up futures that were handed out but never completed
     * via a message, {@link #getRequestCleanupInterval() getRequestCleanupInterval()}.
     *
     * <p>This property is defined as 5 seconds.
     */
    public static final Duration DEFAULT_REQUEST_CLEANUP_INTERVAL = Duration.ofSeconds(5);

    /**
     * Default maximum number of pings have not received a response allowed by the 
     * client, {@link #getMaxPingsOut() getMaxPingsOut()}.
     *
     * <p>This property is defined as {@value #DEFAULT_MAX_PINGS_OUT}
     */
    public static final int DEFAULT_MAX_PINGS_OUT = 2;

    /**
     * Default SSL protocol used to create an SSLContext if the {@link #PROP_SECURE
     * secure property} is used.
     * <p>This property is defined as {@value #DEFAULT_SSL_PROTOCOL}
     */
    public static final String DEFAULT_SSL_PROTOCOL = "TLSv1.2";

    /**
     * Default of pending message buffer that is used for buffering messages that
     * are published during a disconnect/reconnect, {@link #getReconnectBufferSize() getReconnectBufferSize()}.
     *
     * <p>This property is defined as {@value #DEFAULT_RECONNECT_BUF_SIZE} bytes, 8 *
     * 1024 * 1024.
     */
    public static final int DEFAULT_RECONNECT_BUF_SIZE = 8_388_608;

    /**
     * The default length, {@value #DEFAULT_MAX_CONTROL_LINE} bytes, the client will allow in an
     *  outgoing protocol control line, {@link #getMaxControlLine() getMaxControlLine()}.
     * 
     * <p>This value is configurable on the server, and should be set here to match.</p>
     */
    public static final int DEFAULT_MAX_CONTROL_LINE = 1024;

    /**
     * Default dataport class, which will use a TCP socket, {@link #getDataPortType() getDataPortType()}.
     * 
     * <p><em>This option is currently provided only for testing, and experimentation, the default 
     * should be used in almost all cases.</em>
     */
    public static final String DEFAULT_DATA_PORT_TYPE = SocketDataPort.class.getCanonicalName();

    /**
     * Default size for buffers in the connection, not as available as other settings, 
     * this is primarily changed for testing, {@link #getBufferSize() getBufferSize()}.
     */
    public static final int DEFAULT_BUFFER_SIZE = 64 * 1024;

    
    /**
     * Default prefix used for inboxes, you can change this to manage authorization of subjects.
     * See {@link #getInboxPrefix() getInboxPrefix()}, the . is required but will be added if missing.
     */
    public static final String DEFAULT_INBOX_PREFIX = "_INBOX.";

    static final String PFX = "io.nats.client.";

    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_CONNECTION_CB}, see
     * {@link Builder#connectionListener(ConnectionListener) connectionListener}.
     */
    public static final String PROP_CONNECTION_CB = PFX + "callback.connection";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_DATA_PORT_TYPE}, see
     * {@link Builder#dataPortType(String) dataPortType}.
     */
    public static final String PROP_DATA_PORT_TYPE = PFX + "dataport.type";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_ERROR_LISTENER}, see
     * {@link Builder#errorListener(ErrorListener) errorListener}.
     */
    public static final String PROP_ERROR_LISTENER = PFX + "callback.error";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_MAX_PINGS}, see {@link Builder#maxPingsOut(int) maxPingsOut}.
     */
    public static final String PROP_MAX_PINGS = PFX + "maxpings";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_PING_INTERVAL}, see {@link Builder#pingInterval(Duration)
     * pingInterval}.
     */
    public static final String PROP_PING_INTERVAL = PFX + "pinginterval";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_CLEANUP_INTERVAL}, see {@link Builder#requestCleanupInterval(Duration)
     * requestCleanupInterval}.
     */
    public static final String PROP_CLEANUP_INTERVAL = PFX + "cleanupinterval";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_CONNECTION_TIMEOUT}, see
     * {@link Builder#connectionTimeout(Duration) connectionTimeout}.
     */
    public static final String PROP_CONNECTION_TIMEOUT = PFX + "timeout";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_RECONNECT_BUF_SIZE}, see
     * {@link Builder#reconnectBufferSize(long) reconnectBufferSize}.
     */
    public static final String PROP_RECONNECT_BUF_SIZE = PFX + "reconnect.buffer.size";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_RECONNECT_WAIT}, see {@link Builder#reconnectWait(Duration)
     * reconnectWait}.
     */
    public static final String PROP_RECONNECT_WAIT = PFX + "reconnect.wait";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_MAX_RECONNECT}, see {@link Builder#maxReconnects(int)
     * maxReconnects}.
     */
    public static final String PROP_MAX_RECONNECT = PFX + "reconnect.max";

    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_PEDANTIC}, see {@link Builder#pedantic() pedantic}.
     */
    public static final String PROP_PEDANTIC = PFX + "pedantic";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_VERBOSE}, see {@link Builder#verbose() verbose}.
     */
    public static final String PROP_VERBOSE = PFX + "verbose";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_NO_ECHO}, see {@link Builder#noEcho() noEcho}.
     */
    public static final String PROP_NO_ECHO = PFX + "noecho";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_CONNECTION_NAME}, see {@link Builder#connectionName(String)
     * connectionName}.
     */
    public static final String PROP_CONNECTION_NAME = PFX + "name";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_NORANDOMIZE}, see {@link Builder#noRandomize() noRandomize}.
     */
    public static final String PROP_NORANDOMIZE = PFX + "norandomize";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_SERVERS}, 
     * see {@link Builder#servers(String[]) servers}. The value can be a comma-separated list of server URLs.
     */
    public static final String PROP_SERVERS = PFX + "servers";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_PASSWORD}, see {@link Builder#userInfo(String, String)
     * userInfo}.
     */
    public static final String PROP_PASSWORD = PFX + "password";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_USERNAME}, see {@link Builder#userInfo(String, String)
     * userInfo}.
     */
    public static final String PROP_USERNAME = PFX + "username";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_TOKEN}, see {@link Builder#token(String) token}.
     */
    public static final String PROP_TOKEN = PFX + "token";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_URL}, see {@link Builder#server(String) server}.
     */
    public static final String PROP_URL = PFX + "url";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_SECURE},
     *  see {@link Builder#sslContext(SSLContext) sslContext}.
     * 
     * This property is a boolean flag, but it tells the options parser to use the
     * default SSL context. Set the default context before creating the options.
     */
    public static final String PROP_SECURE = PFX + "secure";

    /**
     * Property used to configure a builder from a Properties object. 
     * {@value #PROP_OPENTLS}, see {@link Builder#sslContext(SSLContext) sslContext}.
     * 
     * This property is a boolean flag, but it tells the options parser to use the
     * an SSL context that takes any server TLS certificate and does not provide
     * its own. The server must have tls_verify turned OFF for this option to work.
     */
    public static final String PROP_OPENTLS = PFX + "opentls";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_USE_OLD_REQUEST_STYLE}, see {@link Builder#oldRequestStyle()
     * oldRequestStyle}.
     */
    public static final String PROP_USE_OLD_REQUEST_STYLE = "use.old.request.style";
    /**
     * Property used to configure a builder from a Properties object. {@value #PROP_MAX_CONTROL_LINE}, see {@link Builder#maxControlLine(int)
     * maxControlLine}.
     */
    public static final String PROP_MAX_CONTROL_LINE = "max.control.line";

    /**
     * This property is used to enable support for UTF8 subjects. See {@link Builder#supportUTF8Subjects() supportUTF8Subjcts()}
     */
    public static final String PROP_UTF8_SUBJECTS = "allow.utf8.subjects";

    /**
     * Property used to set the inbox prefix
     */
    public static final String PROP_INBOX_PREFIX = "inbox.prefix";

    /**
     * Protocol key {@value #OPTION_VERBOSE}, see {@link Builder#verbose() verbose}.
     */
    static final String OPTION_VERBOSE = "verbose";

    /**
     * Protocol key {@value #OPTION_PEDANTIC}, see {@link Builder#pedantic()
     * pedantic}.
     */
    static final String OPTION_PEDANTIC = "pedantic";

    /**
     * Protocol key {@value #OPTION_TLS_REQUIRED}, see
     * {@link Builder#sslContext(SSLContext) sslContext}.
     */
    static final String OPTION_TLS_REQUIRED = "tls_required";

    /**
     * Protocol key {@value #OPTION_AUTH_TOKEN}, see {@link Builder#token(String)
     * token}.
     */
    static final String OPTION_AUTH_TOKEN = "auth_token";

    /**
     * Protocol key {@value #OPTION_USER}, see
     * {@link Builder#userInfo(String, String) userInfo}.
     */
    static final String OPTION_USER = "user";

    /**
     * Protocol key {@value #OPTION_PASSWORD}, see
     * {@link Builder#userInfo(String, String) userInfo}.
     */
    static final String OPTION_PASSWORD = "pass";

    /**
     * Protocol key {@value #OPTION_NAME}, see {@link Builder#connectionName(String)
     * connectionName}.
     */
    static final String OPTION_NAME = "name";

    /**
     * Protocol key {@value #OPTION_LANG}, will be set to "Java".
     */
    static final String OPTION_LANG = "lang";

    /**
     * Protocol key {@value #OPTION_VERSION}, will be set to
     * {@link Nats#CLIENT_VERSION CLIENT_VERSION}.
     */
    static final String OPTION_VERSION = "version";

    /**
     * Protocol key {@value #OPTION_PROTOCOL}, will be set to 1.
     */
    static final String OPTION_PROTOCOL = "protocol";

    /**
     * Echo key {@value #OPTION_ECHO}, determines if the server should echo to the client.
     */
    static final String OPTION_ECHO = "echo";

    /**
     * NKey key {@value #OPTION_NKEY}, the public key being used for sign-in.
     */
    static final String OPTION_NKEY = "nkey";

    /**
     * SIG key {@value #OPTION_SIG}, the signature of the nonce sent by the server.
     */
    static final String OPTION_SIG = "sig";

    /**
     * JWT key {@value #OPTION_SIG}, the user JWT to send to the server.
     */
    static final String OPTION_JWT = "jwt";

    private List<URI> servers;
    private final boolean noRandomize;
    private final String connectionName;
    private final boolean verbose;
    private final boolean pedantic;
    private final SSLContext sslContext;
    private final int maxReconnect;
    private final int maxControlLine;
    private final Duration reconnectWait;
    private final Duration connectionTimeout;
    private final Duration pingInterval;
    private final Duration requestCleanupInterval;
    private final int maxPingsOut;
    private final long reconnectBufferSize;
    private final String username;
    private final String password;
    private final String token;
    private final String inboxPrefix;
    private final boolean useOldRequestStyle;
    private final int bufferSize;
    private final boolean noEcho;
    private final boolean utf8Support;

    private final AuthHandler authHandler;

    private final ErrorListener errorListener;
    private final ConnectionListener connectionListener;
    private final String dataPortType;

    private final boolean trackAdvancedStats;

    /**
     * Options are created using a Builder. The builder supports chaining and will
     * create a default set of options if no methods are calls. The builder can also
     * be created from a properties object using the property names defined with the
     * prefix PROP_ in this class.
     * 
     * <p>{@code new Options.Builder().build()} is equivalent to calling {@link Nats#connect() Nats.connect()}.
     * 
     * <p>A common usage for testing might be {@code new Options.Builder().server(myserverurl).noReconnect.build()}
     */
    public static class Builder {

        private ArrayList<URI> servers = new ArrayList<URI>();
        private boolean noRandomize = false;
        private String connectionName = null; // Useful for debugging -> "test: " + NatsTestServer.currentPort();
        private boolean verbose = false;
        private boolean pedantic = false;
        private SSLContext sslContext = null;
        private int maxControlLine = DEFAULT_MAX_CONTROL_LINE;
        private int maxReconnect = DEFAULT_MAX_RECONNECT;
        private Duration reconnectWait = DEFAULT_RECONNECT_WAIT;
        private Duration connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        private Duration pingInterval = DEFAULT_PING_INTERVAL;
        private Duration requestCleanupInterval = DEFAULT_REQUEST_CLEANUP_INTERVAL;
        private int maxPingsOut = DEFAULT_MAX_PINGS_OUT;
        private long reconnectBufferSize = DEFAULT_RECONNECT_BUF_SIZE;
        private String username = null;
        private String password = null;
        private String token = null;
        private boolean useOldRequestStyle = false;
        private int bufferSize = DEFAULT_BUFFER_SIZE;
        private boolean trackAdvancedStats = false;
        private boolean noEcho = false;
        private boolean utf8Support = false;
        private String inboxPrefix = DEFAULT_INBOX_PREFIX;

        private AuthHandler authHandler;

        private ErrorListener errorListener = null;
        private ConnectionListener connectionListener = null;
        private String dataPortType = DEFAULT_DATA_PORT_TYPE;

        /**
         * Constructs a new Builder with the default values.
         * 
         * <p>One tiny clarification is that the builder doesn't have a server url. When {@link #build() build()}
         * is called on a default builder it will add the {@link Options#DEFAULT_URL
         * default url} to its list of servers before creating the options object.
         */
        public Builder() {
        }

        /**
         * Constructs a new {@code Builder} from a {@link Properties} object.
         * 
         * <p>If {@link Options#PROP_SECURE PROP_SECURE} is set, the builder will
         * try to to get the default context{@link SSLContext#getDefault() getDefault()}.
         * 
         * If a context can't be found, no context is set and an IllegalArgumentException is thrown.
         * 
         * <p>Methods called on the builder after construction can override the properties.
         *
         * @param props the {@link Properties} object
         */
        public Builder(Properties props) throws IllegalArgumentException {
            if (props == null) {
                throw new IllegalArgumentException("Properties cannot be null");
            }

            if (props.containsKey(PROP_URL)) {
                this.server(props.getProperty(PROP_URL, DEFAULT_URL));
            }

            if (props.containsKey(PROP_USERNAME)) {
                this.username = props.getProperty(PROP_USERNAME, null);
            }

            if (props.containsKey(PROP_PASSWORD)) {
                this.password = props.getProperty(PROP_PASSWORD, null);
            }

            if (props.containsKey(PROP_TOKEN)) {
                this.token = props.getProperty(PROP_TOKEN, null);
            }

            if (props.containsKey(PROP_SERVERS)) {
                String str = props.getProperty(PROP_SERVERS);
                if (str.isEmpty()) {
                    throw new IllegalArgumentException(PROP_SERVERS + " cannot be empty");
                } else {
                    String[] servers = str.trim().split(",\\s*");
                    this.servers(servers);
                }
            }

            if (props.containsKey(PROP_NORANDOMIZE)) {
                this.noRandomize = Boolean.parseBoolean(props.getProperty(PROP_NORANDOMIZE));
            }

            if (props.containsKey(PROP_SECURE)) {
                boolean secure = Boolean.parseBoolean(props.getProperty(PROP_SECURE));

                if (secure) {
                    try {
                        this.sslContext = SSLContext.getDefault();
                    } catch (NoSuchAlgorithmException e) {
                        this.sslContext = null;
                        throw new IllegalArgumentException("Unable to retrieve default SSL context");
                    }
                }
            }

            if (props.containsKey(PROP_OPENTLS)) {
                boolean tls = Boolean.parseBoolean(props.getProperty(PROP_OPENTLS));

                if (tls) {
                    try {
                        this.sslContext = SSLUtils.createOpenTLSContext();
                    } catch (Exception e) {
                        this.sslContext = null;
                        throw new IllegalArgumentException("Unable to create open SSL context");
                    }
                }
            }

            if (props.containsKey(PROP_CONNECTION_NAME)) {
                this.connectionName = props.getProperty(PROP_CONNECTION_NAME, null);
            }

            if (props.containsKey(PROP_VERBOSE)) {
                this.verbose = Boolean.parseBoolean(props.getProperty(PROP_VERBOSE));
            }

            if (props.containsKey(PROP_NO_ECHO)) {
                this.noEcho = Boolean.parseBoolean(props.getProperty(PROP_NO_ECHO));
            }

            if (props.containsKey(PROP_UTF8_SUBJECTS)) {
                this.utf8Support = Boolean.parseBoolean(props.getProperty(PROP_UTF8_SUBJECTS));
            }

            if (props.containsKey(PROP_PEDANTIC)) {
                this.pedantic = Boolean.parseBoolean(props.getProperty(PROP_PEDANTIC));
            }

            if (props.containsKey(PROP_MAX_RECONNECT)) {
                this.maxReconnect = Integer
                        .parseInt(props.getProperty(PROP_MAX_RECONNECT, Integer.toString(DEFAULT_MAX_RECONNECT)));
            }

            if (props.containsKey(PROP_RECONNECT_WAIT)) {
                int ms = Integer.parseInt(props.getProperty(PROP_RECONNECT_WAIT, "-1"));
                this.reconnectWait = (ms < 0) ? DEFAULT_RECONNECT_WAIT : Duration.ofMillis(ms);
            }

            if (props.containsKey(PROP_RECONNECT_BUF_SIZE)) {
                this.reconnectBufferSize = Long.parseLong(
                        props.getProperty(PROP_RECONNECT_BUF_SIZE, Long.toString(DEFAULT_RECONNECT_BUF_SIZE)));
            }

            if (props.containsKey(PROP_CONNECTION_TIMEOUT)) {
                int ms = Integer.parseInt(props.getProperty(PROP_CONNECTION_TIMEOUT, "-1"));
                this.connectionTimeout = (ms < 0) ? DEFAULT_CONNECTION_TIMEOUT : Duration.ofMillis(ms);
            }

            if (props.containsKey(PROP_MAX_CONTROL_LINE)) {
                int bytes = Integer.parseInt(props.getProperty(PROP_MAX_CONTROL_LINE, "-1"));
                this.maxControlLine = (bytes < 0) ? DEFAULT_MAX_CONTROL_LINE : bytes;
            }

            if (props.containsKey(PROP_PING_INTERVAL)) {
                int ms = Integer.parseInt(props.getProperty(PROP_PING_INTERVAL, "-1"));
                this.pingInterval = (ms < 0) ? DEFAULT_PING_INTERVAL : Duration.ofMillis(ms);
            }

            if (props.containsKey(PROP_CLEANUP_INTERVAL)) {
                int ms = Integer.parseInt(props.getProperty(PROP_CLEANUP_INTERVAL, "-1"));
                this.requestCleanupInterval = (ms < 0) ? DEFAULT_REQUEST_CLEANUP_INTERVAL : Duration.ofMillis(ms);
            }

            if (props.containsKey(PROP_MAX_PINGS)) {
                this.maxPingsOut = Integer
                        .parseInt(props.getProperty(PROP_MAX_PINGS, Integer.toString(DEFAULT_MAX_PINGS_OUT)));
            }

            if (props.containsKey(PROP_USE_OLD_REQUEST_STYLE)) {
                this.useOldRequestStyle = Boolean.parseBoolean(props.getProperty(PROP_USE_OLD_REQUEST_STYLE));
            }

            if (props.containsKey(PROP_ERROR_LISTENER)) {
                Object instance = createInstanceOf(props.getProperty(PROP_ERROR_LISTENER));
                this.errorListener = (ErrorListener) instance;
            }

            if (props.containsKey(PROP_CONNECTION_CB)) {
                Object instance = createInstanceOf(props.getProperty(PROP_CONNECTION_CB));
                this.connectionListener = (ConnectionListener) instance;
            }

            if (props.containsKey(PROP_DATA_PORT_TYPE)) {
                this.dataPortType = props.getProperty(PROP_DATA_PORT_TYPE);
            }

            if (props.containsKey(PROP_INBOX_PREFIX)) {
                this.inboxPrefix(props.getProperty(PROP_INBOX_PREFIX, DEFAULT_INBOX_PREFIX));
            }
        }

        static Object createInstanceOf(String className) {
            Object instance = null;
            try {
                Class<?> clazz = Class.forName(className);
                Constructor<?> constructor = clazz.getConstructor();
                instance = constructor.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            return instance;
        }

        /**
         * Add a server to the list of known servers.
         * 
         * @param serverURL the URL for the server to add
         * @throws IllegalArgumentException if the url is not formatted correctly.
         * @return the Builder for chaining
         */
        public Builder server(String serverURL) {
            return this.servers(serverURL.trim().split(","));
        }

        /**
         * Add an array of servers to the list of known servers.
         * 
         * @param servers A list of server URIs
         * @throws IllegalArgumentException if any url is not formatted correctly.
         * @return the Builder for chaining
         */
        public Builder servers(String[] servers) {
            for (String s : servers) {
                if (s != null && !s.isEmpty()) {
                    try {
                        this.servers.add(Options.parseURIForServer(s.trim()));
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException("Bad server URL: " + s, e);
                    }
                }
            }
            return this;
        }

        /**
         * Turn on the old request style that uses a new inbox and subscriber for each
         * request.
         * @return the Builder for chaining
         */
        public Builder oldRequestStyle() {
            this.useOldRequestStyle = true;
            return this;
        }

        /**
         * Turn off server pool randomization. By default the server will pick
         * servers from its list randomly on a reconnect. When set to noRandom the server
         * goes in the order they were configured or provided by gnatsd.
         * @return the Builder for chaining
         */
        public Builder noRandomize() {
            this.noRandomize = true;
            return this;
        }

        /**
         * Turn off echo. If supported by the gnatsd version you are connecting to this
         * flag will prevent the server from echoing messages back to the connection if it
         * has subscriptions on the subject being published to.
         * @return the Builder for chaining
         */
        public Builder noEcho() {
            this.noEcho = true;
            return this;
        }

        /**
         * The client protocol is not clear about the encoding for subject names. For 
         * performance reasons, the Java client defaults to ASCII. You can enable UTF8
         * with this method. The server, written in go, treats byte to string as UTF8 by default
         * and should allow UTF8 subjects, but make sure to test any clients when using them.
         * @return the Builder for chaining
         */
        public Builder supportUTF8Subjects() {
            this.utf8Support = true;
            return this;
        }

        /**
         * Set the connection's optional Name.
         * 
         * @param name the connections new name.
         * @return the Builder for chaining
         */
        public Builder connectionName(String name) {
            this.connectionName = name;
            return this;
        }

        /**
         * Set the connection's inbox prefix. All inboxes will start with this string.
         * 
         * @param prefix prefix to use.
         * @return the Builder for chaining
         */
        public Builder inboxPrefix(String prefix) {
            this.inboxPrefix = prefix;

            if (!this.inboxPrefix.endsWith(".")) {
                this.inboxPrefix = this.inboxPrefix + ".";
            }
            return this;
        }

        /**
         * Turn on verbose mode with the server.
         * @return the Builder for chaining
         */
        public Builder verbose() {
            this.verbose = true;
            return this;
        }

        /**
         * Turn on pedantic mode for the server, in relation to this connection.
         * @return the Builder for chaining
         */
        public Builder pedantic() {
            this.pedantic = true;
            return this;
        }

        /**
         * Turn on advanced stats, primarily for test/benchmarks. These are visible if you
         * call toString on the {@link Statistics Statistics} object.
         * @return the Builder for chaining
         */
        public Builder turnOnAdvancedStats() {
            this.trackAdvancedStats = true;
            return this;
        }

        /**
         * Sets the options to use the default SSL Context, if it exists.
         * 
         * @throws NoSuchAlgorithmException If the default protocol is unavailable.
         * @throws IllegalArgumentException If there is no default SSL context.
         * @return the Builder for chaining
         */
        public Builder secure() throws NoSuchAlgorithmException, IllegalArgumentException {
            this.sslContext = SSLContext.getDefault();

            if(this.sslContext == null) {
                throw new IllegalArgumentException("No Default SSL Context");
            }
            return this;
        }

        /**
         * Set the SSL context to one that accepts any server certificate and has no client certificates.
         * 
         * @throws NoSuchAlgorithmException If the tls protocol is unavailable.
         * @return the Builder for chaining
         */
        public Builder opentls() throws NoSuchAlgorithmException {
            this.sslContext = SSLUtils.createOpenTLSContext();
            return this;
        }

        /**
         * Set the SSL context, requires that the server supports TLS connections and
         * the URI specifies TLS.
         * 
         * @param ctx the SSL Context to use for TLS connections
         * @return the Builder for chaining
         */
        public Builder sslContext(SSLContext ctx) {
            this.sslContext = ctx;
            return this;
        }

        /**
         * Equivalent to calling maxReconnects with 0, {@link #maxReconnects(int) maxReconnects}.
         * @return the Builder for chaining
         */
        public Builder noReconnect() {
            this.maxReconnect = 0;
            return this;
        }

        /**
         * Set the maximum number of reconnect attempts. Use 0 to turn off
         * auto-reconnect. Use -1 to turn on infinite reconnects.
         * 
         * <p>The reconnect count is incremented on a per-server basis, so if the server list contains 5 servers
         * but max reconnects is set to 3, only 3 of those servers will be tried.
         * 
         * <p>This library has a slight difference from some NATS clients, if you set the maxReconnects to zero
         * there will not be any reconnect attempts, regardless of the number of known servers.
         * 
         * <p>The reconnect state is entered when the connection is connected and loses
         * that connection. During the initial connection attempt, the client will cycle over
         * its server list one time, regardless of what maxReconnects is set to. The only exception
         * to this is the experimental async connect method {@link Nats#connectAsynchronously(Options, boolean) connectAsynchronously}.
         * 
         * @param max the maximum reconnect attempts
         * @return the Builder for chaining
         */
        public Builder maxReconnects(int max) {
            this.maxReconnect = max;
            return this;
        }

        /**
         * Set the time to wait between reconnect attempts to the same server. This setting is only used
         * by the client when the same server appears twice in the reconnect attempts, either because it is the
         * only known server or by random chance. Note, the randomization of the server list doesn't occur per
         * attempt, it is performed once at the start, so if there are 2 servers in the list you will never encounter
         * the reconnect wait.
         * 
         * @param time the time to wait
         * @return the Builder for chaining
         */
        public Builder reconnectWait(Duration time) {
            this.reconnectWait = time;
            return this;
        }

        /**
         * Set the maximum length of a control line sent by this connection. This value is also configured
         * in the server but the protocol doesn't currently forward that setting. Configure it here so that
         * the client can ensure that messages are valid before sending to the server.
         * 
         * @param bytes the max byte count
         * @return the Builder for chaining
         */
        public Builder maxControlLine(int bytes) {
            this.maxControlLine = bytes;
            return this;
        }

        /**
         * Set the timeout for connection attempts.
         * 
         * @param time the time to wait
         * @return the Builder for chaining
         */
        public Builder connectionTimeout(Duration time) {
            this.connectionTimeout = time;
            return this;
        }

        /**
         * Set the interval between attempts to pings the server. These pings are automated,
         * and capped by {@link #maxPingsOut(int) maxPingsOut()}.
         * 
         * @param time the time between client to server pings
         * @return the Builder for chaining
         */
        public Builder pingInterval(Duration time) {
            this.pingInterval = time;
            return this;
        }

        /**
         * Set the interval between cleaning passes on outstanding request futures that are cancelled or timeout
         * in the application code.
         * 
         * <p>The default value is probably reasonable, but this interval is useful in a very noisy network
         * situation where lots of requests are used.
         * 
         * @param time the cleaning interval
         * @return the Builder for chaining
         */
        public Builder requestCleanupInterval(Duration time) {
            this.requestCleanupInterval = time;
            return this;
        }

        /**
         * Set the maximum number of pings the client can have in flight.
         * 
         * @param max the max pings
         * @return the Builder for chaining
         */
        public Builder maxPingsOut(int max) {
            this.maxPingsOut = max;
            return this;
        }

        /**
         * Sets the initial size for buffers in the connection, primarily for testing.
         * @param size the size in bytes to make buffers for connections created with this options
         * @return the Builder for chaining
         */
        public Builder bufferSize(int size) {
            this.bufferSize = size;
            return this;
        }

        /**
         * Set the maximum number of bytes to buffer in the client when trying to
         * reconnect. When this value is exceeded the client will start to drop messages.
         * The count of dropped messages can be read from the {@link Statistics#getDroppedCount() Statistics}.
         * 
         * @param size the size in bytes
         * @return the Builder for chaining
         */
        public Builder reconnectBufferSize(long size) {
            this.reconnectBufferSize = size;
            return this;
        }

        /**
         * Set the username and password for basic authentication.
         * 
         * If the user and password are set in the server URL, they will override these values. However, in a clustering situation,
         * these values can be used as a fallback.
         * 
         * @param userName a non-empty user name
         * @param password the password, in plain text
         * @return the Builder for chaining
         */
        public Builder userInfo(String userName, String password) {
            this.username = userName;
            this.password = password;
            return this;
        }

        /**
         * Set the token for token-based authentication.
         * 
         * If a token is provided in a server URI it overrides this value.
         * 
         * @param token The token
         * @return the Builder for chaining
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Set the {@link AuthHandler AuthHandler} to sign the server nonce for authentication in 
         * nonce-mode.
         * 
         * @param handler The new AuthHandler for this connection.
         * @return the Builder for chaining
         */
        public Builder authHandler(AuthHandler handler) {
            this.authHandler = handler;
            return this;
        }

        /**
         * Set the {@link ErrorListener ErrorListener} to receive asynchronous error events related to this
         * connection.
         * 
         * @param listener The new ErrorListener for this connection.
         * @return the Builder for chaining
         */
        public Builder errorListener(ErrorListener listener) {
            this.errorListener = listener;
            return this;
        }

        /**
         * Set the {@link ConnectionListener ConnectionListener} to receive asynchronous notifications of disconnect
         * events.
         * 
         * @param listener The new ConnectionListener for this type of event.
         * @return the Builder for chaining
         */
        public Builder connectionListener(ConnectionListener listener) {
            this.connectionListener = listener;
            return this;
        }

        /**
         * The class to use for this connections data port. This is an advanced setting
         * and primarily useful for testing.
         * 
         * @param dataPortClassName a valid and accessible class name
         * @return the Builder for chaining
         */
        public Builder dataPortType(String dataPortClassName) {
            this.dataPortType = dataPortClassName;
            return this;
        }

        /**
         * Build an Options object from this Builder.
         * 
         * <p>If the Options builder was not provided with a server, a default one will be included
         * {@link Options#DEFAULT_URL}. If only a single server URI is included, the builder
         * will try a few things to make connecting easier:
         * <ul>
         * <li>If there is no user/password is set but the URI has them, {@code nats://user:password@server:port}, they will be used.
         * <li>If there is no token is set but the URI has one, {@code nats://token@server:port}, it will be used.
         * <li>If the URI is of the form tls:// and no SSL context was assigned, one is created, see {@link Options.Builder#secure() secure()}.
         * <li>If the URI is of the form opentls:// and no SSL context was assigned one will be created
         * that does not check the servers certificate for validity. This is not secure and only provided
         * for tests and development.
         * </ul>
         * 
         * @return the new options object
         * @throws IllegalStateException if there is a conflict in the options, like a token and a user/pass
         */
        public Options build() throws IllegalStateException {

            if (this.username != null && this.token != null) {
                throw new IllegalStateException("Options can't have token and username");
            }
            
            if (servers.size() == 0) {
                server(DEFAULT_URL);
            } else if (servers.size() == 1) { // Allow some URI based configs
                URI serverURI = servers.get(0);

                if ("tls".equals(serverURI.getScheme()) && this.sslContext == null)
                {
                    try {
                        this.sslContext = SSLContext.getDefault();
                    } catch (NoSuchAlgorithmException e) {
                        throw new IllegalStateException("Unable to create default SSL context", e);
                    }
                } else if ("opentls".equals(serverURI.getScheme()) && this.sslContext == null)
                {
                    this.sslContext = SSLUtils.createOpenTLSContext();
                }
            }
            return new Options(this);
        }
    }

    private Options(Builder b) {
        this.servers = b.servers;
        this.noRandomize = b.noRandomize;
        this.connectionName = b.connectionName;
        this.verbose = b.verbose;
        this.pedantic = b.pedantic;
        this.sslContext = b.sslContext;
        this.maxReconnect = b.maxReconnect;
        this.reconnectWait = b.reconnectWait;
        this.connectionTimeout = b.connectionTimeout;
        this.pingInterval = b.pingInterval;
        this.requestCleanupInterval = b.requestCleanupInterval;
        this.maxPingsOut = b.maxPingsOut;
        this.reconnectBufferSize = b.reconnectBufferSize;
        this.username = b.username;
        this.password = b.password;
        this.token = b.token;
        this.useOldRequestStyle = b.useOldRequestStyle;
        this.maxControlLine = b.maxControlLine;
        this.bufferSize = b.bufferSize;
        this.noEcho = b.noEcho;
        this.utf8Support = b.utf8Support;
        this.inboxPrefix = b.inboxPrefix;

        this.authHandler = b.authHandler;
        this.errorListener = b.errorListener;
        this.connectionListener = b.connectionListener;
        this.dataPortType = b.dataPortType;
        this.trackAdvancedStats = b.trackAdvancedStats;
    }

    /**
     * @return the error listener, or null, see {@link Builder#errorListener(ErrorListener) errorListener()} in the builder doc
     */
    public ErrorListener getErrorListener() {
        return this.errorListener;
    }

    /**
     * @return the connection listener, or null, see {@link Builder#connectionListener(ConnectionListener) connectionListener()} in the builder doc
     */
    public ConnectionListener getConnectionListener() {
        return this.connectionListener;
    }

    /**
     * @return the auth handler, or null, see {@link Builder#authHandler(AuthHandler) authHandler()} in the builder doc
     */
    public AuthHandler getAuthHandler() {
        return this.authHandler;
    }

    /**
     * @return the dataport type for connections created by this options object, see {@link Builder#dataPortType(String) dataPortType()} in the builder doc
     */
    public String getDataPortType() {
        return this.dataPortType;
    }

    /**
     * @return the data port described by these options
     */
    public DataPort buildDataPort() {
        return (DataPort) Options.Builder.createInstanceOf(dataPortType);
    }

    /**
     * @return the servers stored in this options, see {@link Builder#servers(String[]) servers()} in the builder doc
     */
    public Collection<URI> getServers() {
        return servers;
    }

    /**
     * @return should we turn off randomization for server connection attempts, see {@link Builder#noRandomize() noRandomize()} in the builder doc
     */
    public boolean isNoRandomize() {
        return noRandomize;
    }

    /**
     * @return whether or not utf8 subjects are supported, see {@link Builder#supportUTF8Subjects() supportUTF8Subjects()} in the builder doc.
     */
    public boolean supportUTF8Subjects() {
        return utf8Support;
    }

    /**
     * @return the connectionName, see {@link Builder#connectionName(String) connectionName()} in the builder doc
     */
    public String getConnectionName() {
        return connectionName;
    }

    /**
     * @return are we in verbose mode, see {@link Builder#verbose() verbose()} in the builder doc
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * @return is echo-ing disabled, see {@link Builder#noEcho() noEcho()} in the builder doc
     */
    public boolean isNoEcho() {
        return noEcho;
    }

    /**
     * @return are we using pedantic protocol, see {@link Builder#pedantic() pedantic()} in the builder doc
     */
    public boolean isPedantic() {
        return pedantic;
    }

    /**
     * @return should we track advanced stats, see {@link Builder#turnOnAdvancedStats() turnOnAdvancedStats()} in the builder doc
     */
    public boolean isTrackAdvancedStats() {
        return trackAdvancedStats;
    }

    /**
     * @return the maximum length of a control line, see {@link Builder#maxControlLine(int) maxControlLine()} in the builder doc
     */
    public int getMaxControlLine() {
        return maxControlLine;
    }

    /**
     * 
     * @return true if there is an sslContext for this Options, otherwise false, see {@link Builder#secure() secure()} in the builder doc
     */
    public boolean isTLSRequired() {
        return (this.sslContext != null);
    }

    /**
     * @return the sslContext, see {@link Builder#secure() secure()} in the builder doc
     */
    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * @return the maxReconnect attempts to make before failing, see {@link Builder#maxReconnects(int) maxReconnects()} in the builder doc
     */
    public int getMaxReconnect() {
        return maxReconnect;
    }

    /**
     * @return the reconnectWait, used between reconnect attempts to the same server, see {@link Builder#reconnectWait(Duration) reconnectWait()} in the builder doc
     */
    public Duration getReconnectWait() {
        return reconnectWait;
    }

    /**
     * @return the connectionTimeout, see {@link Builder#connectionTimeout(Duration) connectionTimeout()} in the builder doc
     */
    public Duration getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @return the pingInterval, see {@link Builder#pingInterval(Duration) pingInterval()} in the builder doc
     */
    public Duration getPingInterval() {
        return pingInterval;
    }

    /**
     * @return the request cleanup interval, see {@link Builder#requestCleanupInterval(Duration) requestCleanupInterval()} in the builder doc
     */
    public Duration getRequestCleanupInterval() {
        return requestCleanupInterval;
    }

    /**
     * @return the maxPingsOut to limit the number of pings on the wire, see {@link Builder#maxPingsOut(int) maxPingsOut()} in the builder doc
     */
    public int getMaxPingsOut() {
        return maxPingsOut;
    }

    /**
     * @return the reconnectBufferSize, to limit the amount of data held during
     *         reconnection attempts, see {@link Builder#reconnectBufferSize(long) reconnectBufferSize()} in the builder doc
     */
    public long getReconnectBufferSize() {
        return reconnectBufferSize;
    }

    /**
     * @return the default size for buffers in the connection code, see {@link Builder#bufferSize(int) bufferSize()} in the builder doc
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * @return the username to use for basic authentication, see {@link Builder#userInfo(String, String) userInfo()} in the builder doc
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password the password to use for basic authentication, see {@link Builder#userInfo(String, String) userInfo()} in the builder doc
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the token to be used for token-based authentication, see {@link Builder#token(String) token()} in the builder doc
     */
    public String getToken() {
        return token;
    }

    /**
     * @return the flag to turn on old style requests, see {@link Builder#oldRequestStyle() oldStyleRequest()} in the builder doc
     */
    public boolean isOldRequestStyle() {
        return useOldRequestStyle;
    }

    /**
     * @return the inbox prefix to use for requests, see {@link Builder#inboxPrefix(String) inboxPrefix()} in the builder doc
     */
    public String getInboxPrefix() {
        return inboxPrefix;
    }
    
    public URI createURIForServer(String serverURI) throws URISyntaxException {
        return Options.parseURIForServer(serverURI);
    }
    
    static URI parseURIForServer(String serverURI) throws URISyntaxException {
        String known[] = {"nats", "tls", "opentls"};
        List<String> knownProtocols = Arrays.asList(known);
        URI uri = null;

        try {
            uri = new URI(serverURI);

            if (uri.getHost() == null || uri.getHost().equals("") || uri.getScheme() == "" || uri.getScheme() == null) {
                // try nats:// - we don't allow bare URIs in options, only from info and then we don't use the protocol
                uri = new URI("nats://"+serverURI);
            }
        } catch (URISyntaxException e) {
            // try nats:// - we don't allow bare URIs in options, only from info and then we don't use the protocol
            uri = new URI("nats://"+serverURI);
        }

        if (!knownProtocols.contains(uri.getScheme())) {
            throw new URISyntaxException(serverURI, "unknown URI scheme ");
        }

        if (uri.getHost() != null && uri.getHost() != "") {
            if (uri.getPort() == -1) {
                uri = new URI(uri.getScheme(), 
                                uri.getUserInfo(), 
                                uri.getHost(),
                                4222,
                                uri.getPath(),
                                uri.getQuery(),
                                uri.getFragment());
            }
            return uri;
        }

        throw new URISyntaxException(serverURI, "unable to parse server URI");
    }

    /**
     * Create the options string sent with a connect message.
     * 
     * If includeAuth is true the auth information is included:
     * If the server URIs have auth info it is used. Otherwise the userInfo is used.
     * 
     * @param serverURI the current server uri
     * @param includeAuth tells the options to build a connection string that includes auth information
     * @param nonce if the client is supposed to sign the nonce for authentication
     * @return the options String, basically JSON
     */
    public String buildProtocolConnectOptionsString(String serverURI, boolean includeAuth, byte[] nonce) {
        StringBuilder connectString = new StringBuilder();
        connectString.append("{");

        appendOption(connectString, Options.OPTION_LANG, Nats.CLIENT_LANGUAGE, true, false);
        appendOption(connectString, Options.OPTION_VERSION, Nats.CLIENT_VERSION, true, true);

        if (this.connectionName != null) {
            appendOption(connectString, Options.OPTION_NAME, this.connectionName, true, true);
        }

        appendOption(connectString, Options.OPTION_PROTOCOL, "1", false, true);

        appendOption(connectString, Options.OPTION_VERBOSE, String.valueOf(this.isVerbose()), false, true);
        appendOption(connectString, Options.OPTION_PEDANTIC, String.valueOf(this.isPedantic()), false, true);
        appendOption(connectString, Options.OPTION_TLS_REQUIRED, String.valueOf(this.isTLSRequired()), false, true);
        appendOption(connectString, Options.OPTION_ECHO, String.valueOf(!this.isNoEcho()), false, true);

        if (includeAuth && nonce != null && this.getAuthHandler() != null) {
            char[] nkey = this.getAuthHandler().getID();
            byte[] sig = this.getAuthHandler().sign(nonce);
            char[] jwt = this.getAuthHandler().getJWT();

            if (sig == null) {
                sig = new byte[0];
            }

            if (jwt == null) {
                jwt = new char[0];
            }

            if (nkey == null) {
                nkey = new char[0];
            }

            String encodedSig = Base64.getUrlEncoder().withoutPadding().encodeToString(sig);

            appendOption(connectString, Options.OPTION_NKEY, new String(nkey), true, true); // public key to string is ok
            appendOption(connectString, Options.OPTION_SIG, encodedSig, true, true);
            appendOption(connectString, Options.OPTION_JWT, new String(jwt), true, true); // public JWT to string is ok
        } else if (includeAuth) {
            String uriUser = null;
            String uriPass = null;
            String uriToken = null;
            
            // Values from URI override options
            try {
                URI uri = this.createURIForServer(serverURI);
                String userInfo = uri.getUserInfo();

                if (userInfo != null) {
                    String[] info = userInfo.split(":");

                    if (info.length == 2) {
                        uriUser = info[0];
                        uriPass = info[1];
                    } else {
                        uriToken = userInfo;
                    }
                }
            } catch(URISyntaxException e) {
                uriUser = uriToken = uriPass = null;
            }

            if (uriUser != null) {
                appendOption(connectString, Options.OPTION_USER, uriUser, true, true);
            } else if (this.username != null) {
                appendOption(connectString, Options.OPTION_USER, this.username, true, true);
            }

            if (uriPass != null) {
                appendOption(connectString, Options.OPTION_PASSWORD, uriPass, true, true);
            } else if (this.password != null) {
                appendOption(connectString, Options.OPTION_PASSWORD, this.password, true, true);
            }

            if (uriToken != null) {
                appendOption(connectString, Options.OPTION_AUTH_TOKEN, uriToken, true, true);
            } else if (this.token != null) {
                appendOption(connectString, Options.OPTION_AUTH_TOKEN, this.token, true, true);
            }
        }

        connectString.append("}");
        return connectString.toString();
    }

    private void appendOption(StringBuilder builder, String key, String value, boolean quotes, boolean comma) {
        if (comma) {
            builder.append(",");
        }
        builder.append("\"");
        builder.append(key);
        builder.append("\"");
        builder.append(":");
        if (quotes) {
            builder.append("\"");
        }
        builder.append(value);
        if (quotes) {
            builder.append("\"");
        }
    }
}
