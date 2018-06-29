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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.nats.client.ConnectionListener.Events;
import io.nats.client.NatsServerProtocolMock.ExitAt;
import io.nats.client.NatsServerProtocolMock.Progress;

public class ConnectTests {
    @Test
    public void testDefaultConnection() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer(Options.DEFAULT_PORT, false)) {
            Connection nc = Nats.connect();
            try {
                assertTrue("Connected Status", Connection.Status.CONNECTED == nc.getStatus());
            } finally {
                nc.close();
                assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
            }
        }
    }

    @Test
    public void testConnection() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer(false)) {
            Connection nc = Nats.connect(ts.getURI());
            try {
                assertTrue("Connected Status", Connection.Status.CONNECTED == nc.getStatus());
            } finally {
                nc.close();
                assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
            }
        }
    }

    @Test
    public void testConnectionWithOptions() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer(false)) {
            Options options = new Options.Builder().server(ts.getURI()).build();
            Connection nc = Nats.connect(options);
            try {
                assertTrue("Connected Status", Connection.Status.CONNECTED == nc.getStatus());
            } finally {
                nc.close();
                assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
            }
        }
    }

    @Test
    public void testFullFakeConnect() throws IOException, InterruptedException {
        try (NatsServerProtocolMock ts = new NatsServerProtocolMock(ExitAt.NO_EXIT)) {
            Connection nc = Nats.connect(ts.getURI());
            try {
                assertTrue("Connected Status", Connection.Status.CONNECTED == nc.getStatus());
            } finally {
                nc.close();
                assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
            }
            assertTrue("Progress", Progress.SENT_PONG == ts.getProgress());
        }
    }

    @Test
    public void testConnectExitBeforeInfo() throws IOException, InterruptedException {
        try (NatsServerProtocolMock ts = new NatsServerProtocolMock(ExitAt.EXIT_BEFORE_INFO)) {
            Options opt = new Options.Builder().server(ts.getURI()).noReconnect().build();
            Connection nc = Nats.connect(opt);
            try {
                assertTrue("Connected Status", Connection.Status.DISCONNECTED == nc.getStatus());
                assertEquals(-1, nc.getMaxPayload()); // No info to set it from
            } finally {
                nc.close();
                assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
            }
            assertTrue("Progress", Progress.CLIENT_CONNECTED == ts.getProgress());
        }
    }

    @Test
    public void testConnectExitAfterInfo() throws IOException, InterruptedException {
        try (NatsServerProtocolMock ts = new NatsServerProtocolMock(ExitAt.EXIT_AFTER_INFO)) {
            Options opt = new Options.Builder().server(ts.getURI()).noReconnect().build();
            Connection nc = Nats.connect(opt);
            try {
                assertTrue("Connected Status", Connection.Status.DISCONNECTED == nc.getStatus());
            } finally {
                nc.close();
                assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
            }
            assertTrue("Progress", Progress.SENT_INFO == ts.getProgress());
        }
    }

    @Test
    public void testConnectExitAfterConnect() throws IOException, InterruptedException {
        try (NatsServerProtocolMock ts = new NatsServerProtocolMock(ExitAt.EXIT_AFTER_CONNECT)) {
            Options opt = new Options.Builder().server(ts.getURI()).noReconnect().build();
            Connection nc = Nats.connect(opt);
            try {
                assertTrue("Connected Status", Connection.Status.DISCONNECTED == nc.getStatus());
            } finally {
                nc.close();
                assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
            }
            assertTrue("Progress", Progress.GOT_CONNECT == ts.getProgress());
        }
    }

    @Test
    public void testConnectExitAfterPing() throws IOException, InterruptedException {
        try (NatsServerProtocolMock ts = new NatsServerProtocolMock(ExitAt.EXIT_AFTER_PING)) {
            Options opt = new Options.Builder().server(ts.getURI()).noReconnect().build();
            Connection nc = Nats.connect(opt);
            try {
                assertTrue("Connected Status", Connection.Status.DISCONNECTED == nc.getStatus());
            } finally {
                nc.close();
                assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
            }
            assertTrue("Progress", Progress.GOT_PING == ts.getProgress());
        }
    }

    @Test
    public void testConnectionFailureWithFallback() throws IOException, InterruptedException {
        
        try (NatsTestServer ts = new NatsTestServer(false)) {
            try (NatsServerProtocolMock fake = new NatsServerProtocolMock(ExitAt.EXIT_AFTER_PING)) {
                Options options = new Options.Builder().server(fake.getURI()).server(ts.getURI()).build();
                Connection nc = Nats.connect(options);
                try {
                    assertEquals("Connected Status", Connection.Status.CONNECTED, nc.getStatus());
                } finally {
                    nc.close();
                    assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
                }
                assertEquals("Progress", Progress.GOT_PING, fake.getProgress());
            }
        }
    }

    @Test
    public void testConnectWithConfig() throws IOException, InterruptedException {
        try (NatsTestServer ts = new NatsTestServer("src/test/resources/simple.conf", false)) {
            Connection nc = Nats.connect(ts.getURI());
            try {
                assertTrue("Connected Status", Connection.Status.CONNECTED == nc.getStatus());
                assertEquals("Parsed port", 16222, ts.getPort());
            } finally {
                nc.close();
                assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
            }
        }
    }

    @Test
    public void testFailWithMissingLineFeedAfterInfo() throws IOException, InterruptedException {
        String badInfo = "{\"server_id\":\"test\"}\rmore stuff";
        try (NatsServerProtocolMock ts = new NatsServerProtocolMock(null, badInfo)) {
            Options options = new Options.Builder().server(ts.getURI()).reconnectWait(Duration.ofDays(1)).build();
            Connection nc = Nats.connect(options);
            try {
                assertEquals("Connected Status", Connection.Status.DISCONNECTED, nc.getStatus());
            } finally {
                nc.close();
                assertEquals("Closed Status", Connection.Status.CLOSED, nc.getStatus());
            }
        }
    }

    @Test
    public void testFailWithStuffAfterInitialInfo() throws IOException, InterruptedException {
        String badInfo = "{\"server_id\":\"test\"}\r\nmore stuff";
        try (NatsServerProtocolMock ts = new NatsServerProtocolMock(null, badInfo)) {
            Options options = new Options.Builder().server(ts.getURI()).reconnectWait(Duration.ofDays(1)).build();
            Connection nc = Nats.connect(options);
            try {
                assertEquals("Connected Status", Connection.Status.DISCONNECTED, nc.getStatus());
            } finally {
                nc.close();
                assertEquals("Closed Status", Connection.Status.CLOSED, nc.getStatus());
            }
        }
    }

    @Test
    public void testFailWrongInitialInfoOP() throws IOException, InterruptedException {
        String badInfo = "PING {\"server_id\":\"test\"}\r\n"; // wrong op code
        try (NatsServerProtocolMock ts = new NatsServerProtocolMock(null, badInfo)) {
            ts.useCustomInfoAsFullInfo();
            Options options = new Options.Builder().server(ts.getURI()).reconnectWait(Duration.ofDays(1)).build();
            Connection nc = Nats.connect(options);
            try {
                assertEquals("Connected Status", Connection.Status.DISCONNECTED, nc.getStatus());
            } finally {
                nc.close();
                assertEquals("Closed Status", Connection.Status.CLOSED, nc.getStatus());
            }
        }
    }

    @Test
    public void testIncompleteInitialInfo() throws IOException, InterruptedException {
        String badInfo = "{\"server_id\"\r\n";
        try (NatsServerProtocolMock ts = new NatsServerProtocolMock(null, badInfo)) {
            Options options = new Options.Builder().server(ts.getURI()).reconnectWait(Duration.ofDays(1)).build();
            Connection nc = Nats.connect(options);
            try {
                assertEquals("Connected Status", Connection.Status.DISCONNECTED, nc.getStatus());
            } finally {
                nc.close();
                assertEquals("Closed Status", Connection.Status.CLOSED, nc.getStatus());
            }
        }
    }
    
    @Test
    public void testAsyncConnection() throws IOException, InterruptedException {
        TestHandler handler = new TestHandler();
        Connection nc = null;

        try (NatsTestServer ts = new NatsTestServer(false)) {
            Options options = new Options.Builder().
                                        server(ts.getURI()).
                                        connectionListener(handler).
                                        build();
           handler.prepForStatusChange(Events.CONNECTED);

            Nats.connectAsynchronously(options, false);

            handler.waitForStatusChange(1, TimeUnit.SECONDS);

            try {
                nc = handler.getConnection();
                assertNotNull(nc);
                assertTrue("Connected Status", Connection.Status.CONNECTED == nc.getStatus());
            } finally {
                if (nc != null) {
                    nc.close();
                }
                assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
            }
        }
    }
    
    @Test
    public void testAsyncConnectionWithReconnect() throws IOException, InterruptedException {
        TestHandler handler = new TestHandler();
        Connection nc = null;
        Options options = new Options.Builder().
                                server(Options.DEFAULT_URL).
                                maxReconnects(-1).
                                reconnectWait(Duration.ofMillis(100)).
                                connectionListener(handler).
                                build();
        
        try {
            handler.prepForStatusChange(Events.CONNECTED);

            Nats.connectAsynchronously(options, true);

            // No server at this point, let it fail and try to start over
            try {
                Thread.sleep(100);
            } catch (Exception exp) {

            }

            nc = handler.getConnection();
            assertNotNull(nc);

            try (NatsTestServer ts = new NatsTestServer(Options.DEFAULT_PORT, false)) {
                handler.waitForStatusChange(1, TimeUnit.SECONDS);
                assertTrue("Connected Status", Connection.Status.CONNECTED == nc.getStatus());
            }
        } finally {
            if (nc != null) {
                nc.close();
            }
            assertTrue("Closed Status", Connection.Status.CLOSED == nc.getStatus());
        }
    }
}