package br.ufsm.csi.seguranca.pilacoin;

import br.ufsm.csi.seguranca.pila.model.PilaCoin;
import br.ufsm.csi.seguranca.pila.model.Usuario;
import net.tomp2p.dht.FutureGet;
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

    public void publicaPilaCoin(PilaCoin pilaCoin, Usuario dono) throws IOException, ClassNotFoundException {
        Usuario usuario = getUsuario(pilaCoin.getIdCriador());
        if (usuario != null) {
            usuario.getMeusPilas().add(pilaCoin);
            setUsuario(usuario);
        } else {
            dono.getMeusPilas().add(pilaCoin);
            setUsuario(dono);
        }
        peer.put(Number160.createHash(pilaCoin.getId())).data(new Data(pilaCoin)).start().awaitUninterruptibly();
        System.out.println("[SERVER] Publicou pila " + pilaCoin.getId() + ".");
    }


    public Usuario getUsuario(String idUsuario) throws IOException, ClassNotFoundException {
        FutureGet futureGet = peer.get(Number160.createHash("usuario_" + idUsuario)).start();
        futureGet.awaitUninterruptibly();
        if (futureGet.isSuccess() && !futureGet.dataMap().values().isEmpty()) {
            //System.out.println("[CLIENTE] chave=" + futureGet.dataMap().values().iterator().next().object().toString());
            return (Usuario) futureGet.dataMap().values().iterator().next().object();
        }
        return null;
    }

    private void setUsuario(Usuario usuario) throws IOException, ClassNotFoundException {
        peer.put(Number160.createHash("usuario_" + usuario.getId())).data(new Data(usuario)).start().awaitUninterruptibly();
    }

}
