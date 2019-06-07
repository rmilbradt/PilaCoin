package br.ufsm.csi.seguranca.pilacoin;

import br.ufsm.csi.seguranca.pila.model.PilaCoin;
import br.ufsm.csi.seguranca.pila.model.Usuario;
import br.ufsm.csi.seguranca.server.model.UsuarioServer;
import net.tomp2p.dht.FutureGet;
import net.tomp2p.dht.PeerBuilderDHT;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.p2p.PeerBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.storage.Data;

import java.io.IOException;
import java.net.InetAddress;

public class PilaDHTClient {

    final private PeerDHT peer;
    final int matricula = 2074361;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        new PilaDHTClient("127.0.0.1", 4001, null);
    }

    private PilaDHTClient(String ipServer, int portServer, Usuario meuUsuario) throws IOException, ClassNotFoundException {
        peer = new PeerBuilderDHT(new PeerBuilder(Number160.createHash(matricula)).ports(4000).start()).start();
        FutureBootstrap future = peer.peer().bootstrap().inetAddress(InetAddress.getByName(ipServer)).ports(portServer).start();
        System.out.println("[CLIENTE] Bootstraping...");
        future.awaitUninterruptibly();
        if (meuUsuario != null) {
            setUsuario(meuUsuario);
        }
        System.out.println("[CLIENTE] Bootstrap feito.");

    }

    public Usuario getUsuario(String idUsuario) throws IOException, ClassNotFoundException {
        FutureGet futureGet = peer.get(Number160.createHash("usuario_" + idUsuario)).start();
        futureGet.awaitUninterruptibly();
        if (futureGet.isSuccess() && !futureGet.dataMap().values().isEmpty()) {
            System.out.println("[CLIENTE] chave=" + futureGet.dataMap().values().iterator().next().object().toString());
            return (UsuarioServer) futureGet.dataMap().values().iterator().next().object();
        }
        return null;
    }

    public void setUsuario(Usuario usuario) throws IOException, ClassNotFoundException {
        peer.put(Number160.createHash("usuario_" + usuario.getId())).data(new Data(usuario)).start().awaitUninterruptibly();
    }

    public PilaCoin getPilaCoin(Long id) throws IOException, ClassNotFoundException {
        FutureGet futureGet = peer.get(Number160.createHash("pila_" + id)).start();
        futureGet.awaitUninterruptibly();
        if (futureGet.isSuccess() && !futureGet.dataMap().values().isEmpty()) {
            System.out.println("[CLIENTE] chave=" + futureGet.dataMap().values().iterator().next().object().toString());
            return (PilaCoin) futureGet.dataMap().values().iterator().next().object();
        }
        return null;
    }

    public void setPilaCoin(PilaCoin pilaCoin) throws IOException {
        peer.put(Number160.createHash("pila_" + pilaCoin.getId())).data(new Data(pilaCoin)).start().awaitUninterruptibly();
        System.out.println("[SERVER] Publicou novo pila " + pilaCoin.getId() + ".");
    }


}
