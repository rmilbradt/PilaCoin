package br.ufsm.csi.seguranca.pilacoin;

import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;

import java.io.IOException;

public class PilaDHTClient {

    final private PeerDHT peer;

    public static void main(String[] args) {

    }

    private PilaDHTClient() throws IOException {
        peer = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(0)).ports(4000).start()).start();
    }

}
