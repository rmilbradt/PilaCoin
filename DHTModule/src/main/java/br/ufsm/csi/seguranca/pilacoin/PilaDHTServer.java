package br.ufsm.csi.seguranca.pilacoin;

import br.ufsm.csi.seguranca.pila.model.PilaCoin;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

import java.io.IOException;

public class PilaDHTServer {

    final private static int serverPort = 4001;
    final private PeerDHT peer;
    final int id = 1;

    public static void main(String[] args) throws IOException {
        new PilaDHTServer();
    }

    public PilaDHTServer() throws IOException {
        peer = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(id)).ports(serverPort).start()).start();
        FutureBootstrap future = peer.peer().bootstrap().start();
        System.out.println("[SERVER] Bootstraping...");
        future.awaitUninterruptibly();
    }

    public void publicaPilaCoin(PilaCoin pilaCoin) throws IOException {
        peer.put(Number160.createHash(pilaCoin.getId())).data(new Data(pilaCoin)).start().awaitUninterruptibly();
        System.out.println("[SERVER] Publicou pila " + pilaCoin.getId() + ".");
    }

}
